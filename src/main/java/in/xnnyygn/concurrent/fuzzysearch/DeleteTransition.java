package in.xnnyygn.concurrent.fuzzysearch;

import java.util.Collections;

class DeleteTransition extends Transition {
    DeleteTransition(State next) {
        super(next);
    }

    @Override
    StateCommand apply(TrieNode node) {
        return new StateCommand(next, Collections.singletonList(node));
    }
}
