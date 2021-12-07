package in.xnnyygn.concurrent.dependency;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

class DependentResourceParallelRunner implements ResourceRunner {
    private final ExecutorService executorService;
    private final List<AbstractResourceCommand> resourcesWithoutDependencies = new ArrayList<>();

    // guarded by this
    private Set<String> tailResourceNames = new HashSet<>();
    private volatile boolean executionFailed = false;
    private final ConcurrentLinkedQueue<AbstractResourceCommand> executedResources = new ConcurrentLinkedQueue<>();

    DependentResourceParallelRunner(ExecutorService executorService, List<Resource> resources) {
        this.executorService = executorService;

        Map<String, ResourceCommand> resourceCommandMap = new HashMap<>();
        for (Resource resource : resources) {
            ResourceCommand command = new ResourceCommand(resource, this);
            if (command.isNoDependency()) {
                resourcesWithoutDependencies.add(command);
            }
            resourceCommandMap.put(resource.name, command);
        }

        for (ResourceCommand command : resourceCommandMap.values()) {
            if (!command.addReferences(resourceCommandMap)) {
                tailResourceNames.add(command.resourceName());
            }
        }
    }

    DependentResourceParallelRunner(ExecutorService executorService, AbstractResourceCommand[] executedResources) {
        this.executorService = executorService;

        Set<String> executedResourceNames = new HashSet<>();
        for (AbstractResourceCommand command : executedResources) {
            executedResourceNames.add(command.resourceName());
        }

        Map<String, ResourceRollbackCommand> resourceCommandMap = new HashMap<>();
        for (AbstractResourceCommand sourceCommand : executedResources) {
            ResourceRollbackCommand rollbackCommand = new ResourceRollbackCommand(
                    (ResourceCommand) sourceCommand, this, executedResourceNames
            );
            if(rollbackCommand.isNoDependency()) {
                resourcesWithoutDependencies.add(rollbackCommand);
            }
            resourceCommandMap.put(rollbackCommand.resourceName(), rollbackCommand);
        }

        for (ResourceRollbackCommand command : resourceCommandMap.values()) {
            if (!command.addReferences(resourceCommandMap)) {
                tailResourceNames.add(command.resourceName());
            }
        }
    }

    void perform() {
        for (AbstractResourceCommand command : resourcesWithoutDependencies) {
            executorService.submit(command);
        }
    }

    @Override
    public void commandExecuted(AbstractResourceCommand command) {
        executedResources.offer(command);
    }

    @Override
    public void commandFailed(AbstractResourceCommand command) {
        /*
         * it's ok if more than one resource failed to execute,
         * in that case, this method will be called more than once
         */
        System.out.println("Resource [" + command.resourceName() + "] failed to execute");
        executionFailed = true;
        executedResources.offer(command);
    }

    @Override
    public boolean submitCommand(AbstractResourceCommand command) {
        if (executionFailed) {
            return false;
        }
        executorService.submit(command);
        return true;
    }

    @Override
    public void chainFinished(String name) {
        synchronized (this) {
            if (!tailResourceNames.remove(name)) {
                throw new IllegalStateException("unexpected tail resource " + name);
            }
            if (tailResourceNames.isEmpty()) {
                tailResourceNames = Collections.emptySet();
                notifyAll();
            }
        }
    }

    boolean await() throws InterruptedException {
        synchronized (this) {
            if (!tailResourceNames.isEmpty()) {
                wait();
            }
        }
        return !executionFailed;
    }

    AbstractResourceCommand[] executedResources() {
        return executedResources.toArray(new AbstractResourceCommand[0]);
    }
}
