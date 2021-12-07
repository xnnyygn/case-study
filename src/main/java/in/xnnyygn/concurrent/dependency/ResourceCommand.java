package in.xnnyygn.concurrent.dependency;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

class ResourceCommand extends AbstractResourceCommand {
    ResourceCommand(Resource resource, ResourceRunner runner) {
        super(resource, runner);
        this.waiting = new HashSet<>(resource.dependencies);
    }

    boolean addReferences(Map<String, ResourceCommand> resourceCommandMap) {
        for (String reference : resource.referencedBy) {
            ResourceCommand command = resourceCommandMap.get(reference);
            if (command == null) {
                throw new IllegalStateException("resource command " + reference + " not found");
            }
            waitedBy.add(command);
        }
        return !waitedBy.isEmpty();
    }

    @Override
    protected void perform() {
        if ("ECS Service".equals(resource.name)) {
            throw new IllegalStateException();
        }

        DateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        System.out.println(format.format(new Date()) + " " + resource.name);
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {
        }
    }
}
