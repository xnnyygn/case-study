package in.xnnyygn.concurrent.fuzzysearch;

import java.util.*;

public class FuzzySearch {
    public List<String> findFuzzilyByStateMachine(List<String> words, String charSeq, int distance) {
        TrieNode root = new TrieNode('\0');
        for (String w : words) {
            root.insert(w, 0);
        }
        StateMachine machine = new StateMachine(charSeq, distance);
        return machine.apply(root);
    }

    public List<String> findFuzzilyDFS(List<String> words, String charSeq, int distance) {
        TrieNode root = new TrieNode('\0');
        for (String w : words) {
            root.insert(w, 0);
        }
        Map<Integer, Set<String>> candidates = new HashMap<>();
        findFuzzilyDFS(charSeq, 0, distance, root, candidates);
        if (candidates.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(candidates.values().iterator().next());
    }

    static class Candidate {
        int from;
        int cost;
        TrieNode node;

        Candidate(int from, int cost, TrieNode node) {
            this.from = from;
            this.cost = cost;
            this.node = node;
        }

        @Override
        public String toString() {
            return "Candidate{" +
                    "from=" + from +
                    ", cost=" + cost +
                    ", node=" + node.id +
                    '}';
        }
    }

    public List<String> findFuzzilyBFS(String charSeq, int distance, TrieNode root) {
        Queue<Candidate> queue = new PriorityQueue<>((c1, c2) -> c2.from - c1.from);
        queue.offer(new Candidate(0, 0, root));
        List<String> words = new ArrayList<>();
        while (!queue.isEmpty()) {
            Candidate c = queue.poll();
            System.out.println(c);
            if (c.from == charSeq.length()) {
                if (c.node.wordEnd) words.add(c.node.word);
                continue;
            }
            TrieNode next = c.node.getChild(charSeq.charAt(c.from));
            if (next != null) {
                queue.offer(new Candidate(c.from + 1, c.cost, next));
            }
            if (c.cost == distance) continue;
            // delete
            queue.offer(new Candidate(c.from + 1, c.cost + 1, c.node));
            for (TrieNode n : c.node.children.values()) {
                // substitute
                queue.offer(new Candidate(c.from + 1, c.cost + 1, n));
                // insert
                queue.offer(new Candidate(c.from, c.cost + 1, n));
            }
        }
        return words;
    }

    private int findFuzzilyDFS(String charSeq, int from, int distance, TrieNode node, Map<Integer, Set<String>> words) {
        System.out.println("from=" + from + ",distance=" + distance + ",node=" + node.id);
        if (from == charSeq.length()) {
            if (!node.wordEnd) return -1;
            words.computeIfAbsent(distance, d -> new HashSet<>()).add(node.word);
            return distance;
        }
        TrieNode next = node.getChild(charSeq.charAt(from));
        if (distance == 0) {
            if (next == null) return -1;
            return findFuzzilyDFS(charSeq, from + 1, 0, next, words);
        }
        // distance > 0
        int max = -1;
        if (next != null) {
            max = findFuzzilyDFS(charSeq, from + 1, distance, next, words);
        }
        // delete it
        max = minDistance(findFuzzilyDFS(charSeq, from + 1, distance - 1, node, words), max, words);
        for (TrieNode n : node.children.values()) {
            // substitute
            max = minDistance(findFuzzilyDFS(charSeq, from + 1, distance - 1, n, words), max, words);
            // insert
            max = minDistance(findFuzzilyDFS(charSeq, from, distance - 1, n, words), max, words);
        }
        return max;
    }

    private int minDistance(int remaining, int max, Map<Integer, Set<String>> words) {
        if (remaining < 0) return max;
        if (remaining > max) {
            words.remove(max);
            return remaining;
        }
        if (remaining < max) {
            words.remove(remaining);
        }
        return max;
    }

    public static void main(String[] args) {
        System.out.println(new FuzzySearch().findFuzzilyByStateMachine(
                Arrays.asList("banana", "woof", "fat", "body", "bad"), "bbnana", 2)
        );
    }
}
