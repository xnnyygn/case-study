package in.xnnyygn.concurrent.dependency;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceRollbackCommand extends AbstractResourceCommand {
    protected ResourceRollbackCommand(ResourceCommand command, ResourceRunner runner, Set<String> executedResourceNames) {
        super(command.resource, runner);
        waiting = new HashSet<>();
        for (String name : command.resource.referencedBy) {
            if (executedResourceNames.contains(name)) {
                waiting.add(name);
            }
        }
    }

    boolean addReferences(Map<String, ResourceRollbackCommand> resourceCommandMap) {
        for (String reference : resource.dependencies) {
            ResourceRollbackCommand command = resourceCommandMap.get(reference);
            if (command != null) {
                waitedBy.add(command);
            }
        }
        return !waitedBy.isEmpty();
    }

    @Override
    protected void perform() {
        DateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        System.out.println(format.format(new Date()) + " " + resource.name + " rollback");
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {
        }
    }
}
