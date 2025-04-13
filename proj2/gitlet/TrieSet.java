package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TrieSet 是一个基于前缀树（Trie）的集合结构。
 * 它支持添加字符串、判断字符串是否存在、以及查找所有具有指定前缀的字符串。
 * 每个节点代表一个字符，路径从根节点到某个终止节点构成一个完整的字符串。
 * 该结构特别适用于高效的前缀搜索场景，例如实现模糊查找。
 */
public class TrieSet implements Serializable {
    private final TrieNode root = new TrieNode();

    /** 单个节点结构，存储子节点和终止标志 */
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false; // 表示这个节点是否为一个完整字符串的结束
    }

    /** 向 Trie 中添加一个字符串 */
    public void add(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        node.isEnd = true;
    }

    /** 判断 Trie 中是否包含这个字符串 */
    public boolean contains(String word) {
        TrieNode node = findNode(word);
        return node != null && node.isEnd;
    }

    /** 返回以 prefix 为前缀的所有字符串 */
    public List<String> startsWith(String prefix) {
        List<String> result = new ArrayList<>();
        TrieNode node = findNode(prefix);
        if (node != null) {
            dfs(prefix, node, result);
        }
        return result;
    }

    /** 辅助函数：递归找所有后缀 */
    private void dfs(String prefix, TrieNode node, List<String> result) {
        if (node.isEnd) {
            result.add(prefix);
        }
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            dfs(prefix + entry.getKey(), entry.getValue(), result);
        }
    }

    /** 找到以 prefix 结尾的节点 */
    private TrieNode findNode(String prefix) {
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            node = node.children.get(ch);
            if (node == null) {
                return null;
            }
        }
        return node;
    }
}
