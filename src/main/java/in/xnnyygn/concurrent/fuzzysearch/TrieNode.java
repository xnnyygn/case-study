package in.xnnyygn.concurrent.fuzzysearch;

import java.util.HashMap;
import java.util.Map;

class TrieNode {
    static int NEXT_NODE_ID = 1;
    final int id;
    final char ch;
    final Map<Character, TrieNode> children = new HashMap<>();
    boolean wordEnd = false;
    String word = null;

    TrieNode(char ch) {
        this.ch = ch;
        id = NEXT_NODE_ID++;
    }

    void insert(String word, int from) {
        TrieNode child = children.computeIfAbsent(word.charAt(from), TrieNode::new);
        if (from == word.length() - 1) {
            child.wordEnd = true;
        } else {
            child.insert(word, from + 1);
        }
        child.word = word.substring(0, from + 1);
    }

    TrieNode getChild(char ch) {
        return children.get(ch);
    }

    @Override
    public String toString() {
        return "TrieNode{" +
                "ch=" + ch +
                ", id=" + id +
                ", wordEnd=" + wordEnd +
                ", word=" + word +
                '}';
    }
}
