package in.xnnyygn.concurrent.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractResourceCommand implements Runnable {
    protected final ResourceRunner runner;
    protected final Resource resource;

    protected Set<String> waiting = Collections.emptySet();
    protected List<AbstractResourceCommand> waitedBy = new ArrayList<>();

    protected AbstractResourceCommand(Resource resource, ResourceRunner runner) {
        this.resource = resource;
        this.runner = runner;
    }

    String resourceName() {
        return resource.name;
    }

    boolean isNoDependency() {
        return waiting.isEmpty();
    }

    protected abstract void perform();

    @Override
    public void run() {
        try {
            perform();
            runner.commandExecuted(this);
        } catch (Exception e) {
            runner.commandFailed(this);
        } finally {
            done();
        }
    }

    void onResourceOk(String name) {
        synchronized (this) {
            if (!waiting.remove(name)) {
                throw new IllegalStateException("unexpected resource " + name);
            }
            if (!waiting.isEmpty()) {
                return;
            }
        }
        if (!runner.submitCommand(this)) {
            // skip
            done();
        }
    }

    private void done() {
        if (waitedBy.isEmpty()) {
            runner.chainFinished(resource.name);
            return;
        }
        for (AbstractResourceCommand command : waitedBy) {
            command.onResourceOk(resource.name);
        }
    }
}
