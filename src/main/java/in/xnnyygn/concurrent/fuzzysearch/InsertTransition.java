package in.xnnyygn.concurrent.fuzzysearch;

class InsertTransition extends Transition {
    InsertTransition(State next) {
        super(next);
    }

    @Override
    StateCommand apply(TrieNode node) {
        if (node.children.isEmpty()) return null;
        return new StateCommand(next, node.children.values());
    }
}
