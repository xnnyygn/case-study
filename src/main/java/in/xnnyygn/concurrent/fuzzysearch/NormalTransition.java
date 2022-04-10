package in.xnnyygn.concurrent.fuzzysearch;

import java.util.Collections;

class NormalTransition extends Transition {
    final char ch;

    NormalTransition(State next, char ch) {
        super(next);
        this.ch = ch;
    }

    StateCommand apply(TrieNode node) {
        TrieNode child = node.getChild(ch);
        if (child == null) return null;
        return new StateCommand(next, Collections.singletonList(child));
    }
}
