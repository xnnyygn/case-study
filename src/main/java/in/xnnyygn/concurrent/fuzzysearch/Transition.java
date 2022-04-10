package in.xnnyygn.concurrent.fuzzysearch;

abstract class Transition {
    final State next;

    Transition(State next) {
        this.next = next;
    }

    abstract StateCommand apply(TrieNode node);
}
