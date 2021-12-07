package in.xnnyygn.concurrent.dependency;

import java.util.*;

class ResourceBuilder {
    private final Map<String, Resource> resourceMap = new HashMap<>();

    ResourceBuilder add(String name) {
        return add(name, Collections.emptyList());
    }

    ResourceBuilder add(String name, List<String> dependencies) {
        if (resourceMap.containsKey(name)) {
            throw new IllegalStateException("resource with name " + name + " already exists");
        }
        resourceMap.put(name, new Resource(name, new HashSet<>(dependencies)));
        return this;
    }

    List<Resource> build() {
        List<Resource> resources = new ArrayList<>();
        for (Resource resource : resourceMap.values()) {
            for (String dependencyName : resource.dependencies) {
                Resource dependency = resourceMap.get(dependencyName);
                if (dependency == null) {
                    throw new IllegalStateException("unknown dependency " + dependencyName + " in resource " + resource.name);
                }
                dependency.addReference(resource.name);
            }
            resources.add(resource);
        }
        return resources;
    }
}
