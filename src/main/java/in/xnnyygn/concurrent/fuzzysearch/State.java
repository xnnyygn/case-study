package in.xnnyygn.concurrent.fuzzysearch;

import java.util.*;

class State {
    final List<Transition> transitions = new ArrayList<>();
    final int cost;
    final int depth;
    final boolean stop;
    Set<TrieNode> calculated = new HashSet<>();

    State(int cost, int depth, boolean stop) {
        this.cost = cost;
        this.depth = depth;
        this.stop = stop;
    }

    void addTransaction(Transition transition) {
        transitions.add(transition);
    }

    boolean apply(Collection<TrieNode> nodes, Queue<StateCommand> queue) {
        boolean found = false;
        for (TrieNode node : nodes) {
            if (calculated.contains(node)) continue;
            System.out.println(this + " " + node);
            for (Transition transition : transitions) {
                StateCommand command = transition.apply(node);
                if (command != null) queue.offer(command);
            }
            if (node.wordEnd && stop) found = true;
            calculated.add(node);
        }
        return found;
    }

    @Override
    public String toString() {
        return "State{" +
                "cost=" + cost +
                ", depth=" + depth +
                ", stop=" + stop +
                '}';
    }
}
