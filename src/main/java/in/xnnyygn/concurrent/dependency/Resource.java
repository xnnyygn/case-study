package in.xnnyygn.concurrent.dependency;

import java.util.HashSet;
import java.util.Set;

class Resource {
    final String name;
    final Set<String> dependencies;
    final Set<String> referencedBy = new HashSet<>();

    Resource(String name, Set<String> dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    void addReference(String name) {
        referencedBy.add(name);
    }

    boolean isNoDependency() {
        return dependencies.isEmpty();
    }

    @Override
    public String toString() {
        return "Resource{" +
                "name='" + name + '\'' +
                ", dependencies=" + dependencies +
                ", referencedBy=" + referencedBy +
                '}';
    }
}
