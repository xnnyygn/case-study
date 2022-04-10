package in.xnnyygn.concurrent.fuzzysearch;

import java.util.*;

class StateMachine {
    final State[][] states;

    StateMachine(String charSeq, int distance) {
        int len = charSeq.length();
        states = new State[distance + 1][len + 1];
        for (int d = 0; d <= distance; d++) {
            for (int i = len; i >= 0; i--) {
                states[d][i] = new State(distance - d, i, i == len);
                if (i != len) {
                    states[d][i].addTransaction(new NormalTransition(states[d][i + 1], charSeq.charAt(i)));
                }
                if (d != 0) {
                    states[d][i].addTransaction(new InsertTransition(states[d - 1][i]));
                }
                if (i != len && d != 0) {
                    states[d][i].addTransaction(new SubstituteTransition(states[d - 1][i + 1]));
                    states[d][i].addTransaction(new DeleteTransition(states[d - 1][i + 1]));
                }
            }
        }
    }

    List<String> apply(TrieNode root) {
        State start = states[states.length - 1][0];
        Queue<StateCommand> queue = new PriorityQueue<>((c1, c2) -> c2.state.depth - c1.state.depth);
        start.apply(Collections.singletonList(root), queue);
        while (!queue.isEmpty()) {
            if (queue.poll().apply(queue)) break;
        }
        List<String> words = new ArrayList<>();
        int len = states[0].length - 1;
        for (State[] state : states) {
            for (TrieNode node : state[len].calculated) {
                if (node.wordEnd) {
                    words.add(node.word);
                    break;
                }
            }
        }
        return words;
    }
}
