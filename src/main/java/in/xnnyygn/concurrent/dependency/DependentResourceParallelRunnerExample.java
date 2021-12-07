package in.xnnyygn.concurrent.dependency;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DependentResourceParallelRunnerExample {
    static class ResourceReferenceCounter {
        final Resource resource;
        private final Set<String> dependencies;

        ResourceReferenceCounter(Resource resource) {
            this.resource = resource;
            this.dependencies = new HashSet<>(resource.dependencies);
        }

        boolean remove(String name) {
            if (!dependencies.remove(name)) {
                throw new IllegalStateException("resource " + resource.name + " is dependent on " + name);
            }
            return dependencies.isEmpty();
        }
    }

    static boolean hasCycle(Collection<Resource> resources) {
        List<Resource> resourcesWithoutDependencies = new ArrayList<>();
        Map<String, ResourceReferenceCounter> remaining = new HashMap<>();
        for (Resource resource : resources) {
            if (resource.isNoDependency()) {
                resourcesWithoutDependencies.add(resource);
            } else {
                remaining.put(resource.name, new ResourceReferenceCounter(resource));
            }
        }
        return hasCycle(resourcesWithoutDependencies, remaining);
    }

    static boolean hasCycle(List<Resource> resourcesWithoutDependencies, Map<String, ResourceReferenceCounter> remaining) {
        if (remaining.isEmpty()) {
            return false;
        }
        if (resourcesWithoutDependencies.isEmpty()) {
            return true;
        }
        List<Resource> resourcesWithoutDependencies2 = new ArrayList<>();
        for (Resource resource : resourcesWithoutDependencies) {
            for (String referenceName : resource.referencedBy) {
                ResourceReferenceCounter rc = remaining.get(referenceName);
                if (rc == null) {
                    throw new IllegalStateException("reference " + referenceName + " not found");
                }
                if (rc.remove(resource.name)) {
                    remaining.remove(referenceName);
                    resourcesWithoutDependencies2.add(rc.resource);
                }
            }
        }
        return hasCycle(resourcesWithoutDependencies2, remaining);
    }

    public static void main(String[] args) throws InterruptedException {
        ResourceBuilder builder = new ResourceBuilder();
        List<Resource> resources = builder
                .add("CloudWatch LogGroup")
                .add("IAM Role")
                .add("ECS TaskDefinition", Arrays.asList("CloudWatch LogGroup", "IAM Role"))
                .add("LoadBalancer")
                .add("LoadBalancer TargetGroup", Collections.singletonList("LoadBalancer"))
                .add("LoadBalancer Listener", Arrays.asList("LoadBalancer", "LoadBalancer TargetGroup"))
                .add("EC2 Security Group")
                .add("ECS Cluster")
                .add("ECS Service", Arrays.asList("ECS TaskDefinition", "ECS Cluster", "LoadBalancer Listener", "LoadBalancer TargetGroup", "EC2 Security Group"))
                .build();
        if (hasCycle(resources)) {
            throw new IllegalArgumentException("found cycle");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        DependentResourceParallelRunner runner = new DependentResourceParallelRunner(executorService, resources);
        runner.perform();
        if (!runner.await()) {
            DependentResourceParallelRunner rollbackRunner = new DependentResourceParallelRunner(executorService, runner.executedResources());
            rollbackRunner.perform();
            rollbackRunner.await();
        }
        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.SECONDS);
    }
}
