package in.xnnyygn.concurrent.fuzzysearch;

import java.util.Collection;
import java.util.Queue;

class StateCommand {
    final State state;
    final Collection<TrieNode> nodes;

    StateCommand(State state, Collection<TrieNode> nodes) {
        this.state = state;
        this.nodes = nodes;
    }

    boolean apply(Queue<StateCommand> queue) {
        return state.apply(nodes, queue);
    }
}
