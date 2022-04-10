package in.xnnyygn.concurrent.fuzzysearch;

class SubstituteTransition extends Transition {
    SubstituteTransition(State next) {
        super(next);
    }

    @Override
    StateCommand apply(TrieNode node) {
        if (node.children.isEmpty()) return null;
        return new StateCommand(next, node.children.values());
    }
}
