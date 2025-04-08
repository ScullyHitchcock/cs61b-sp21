package gitlet;

import java.io.Serializable;
import java.util.*;

public class CommitGraph implements Serializable {
    // key: commit id, value: 对应的 CommitNode
    private Map<String, CommitNode> nodes;

    public CommitGraph() {
        nodes = new HashMap<>();
    }

    /** 添加一个 commit 到图中 */
    public void addCommit(Commit commit) {
        String id = commit.id();
        CommitNode node = new CommitNode(id, commit.getParentIds());
        nodes.put(id, node);
        // 同时更新每个父节点的子节点列表
        for (String parentId : commit.getParentIds()) {
            CommitNode parentNode = nodes.get(parentId);
            if (parentNode != null) {
                parentNode.addChild(id);
            } else {
                // 如果父节点尚未添加，可以创建一个占位的节点（其 parentIds 为空）
                parentNode = new CommitNode(parentId, new ArrayList<>());
                parentNode.addChild(id);
                nodes.put(parentId, parentNode);
            }
        }
    }

    public CommitNode getNode(String commitId) {
        return nodes.get(commitId);
    }

    /**
     * 使用简单的 BFS 算法查找两个 commit 的最近公共祖先（split point）。
     * 这里只返回第一个找到的公共祖先 commit id。
     */
    public String findSplitPoint(String commitId1, String commitId2) {
        // 先获得 commitId1 的所有祖先
        Set<String> ancestors = getAllAncestors(commitId1);
        // 从 commitId2 向上遍历查找第一个出现在 ancestors 中的 commit
        Queue<String> queue = new LinkedList<>();
        queue.add(commitId2);
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (ancestors.contains(current)) {
                return current;
            }
            visited.add(current);
            CommitNode node = nodes.get(current);
            if (node != null) {
                for (String parentId : node.getParentIds()) {
                    if (!visited.contains(parentId)) {
                        queue.add(parentId);
                    }
                }
            }
        }
        return null;  // 理论上不可能没有公共祖先
    }

    private Set<String> getAllAncestors(String commitId) {
        Set<String> ancestors = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(commitId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            ancestors.add(current);
            CommitNode node = nodes.get(current);
            if (node != null) {
                for (String parentId : node.getParentIds()) {
                    if (!ancestors.contains(parentId)) {
                        queue.add(parentId);
                    }
                }
            }
        }
        return ancestors;
    }
}