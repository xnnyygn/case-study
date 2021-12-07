package in.xnnyygn.concurrent.dependency;

interface ResourceRunner {
    void commandExecuted(AbstractResourceCommand command);

    void commandFailed(AbstractResourceCommand command);

    boolean submitCommand(AbstractResourceCommand command);

    void chainFinished(String name);
}
