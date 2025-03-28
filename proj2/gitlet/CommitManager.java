package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * 管理所有提交（Commit）对象和分支信息。
 * 负责维护提交图、分支指针、HEAD 状态等。
 */
public class CommitManager implements Serializable {
    // 使用哈希集合，只存放 Commit 对象的哈希值
    private final HashMap<String, String> commits;

    /* HEAD 指针，指向当前活跃的分支名 */
    private String headBranchName;

    /* 分支指针 map，key为分支名，val为当前分支的最新 commit 哈希值 */
    private final HashMap<String, String> branches;

    /**
     * 初始化 CommitManager。
     * 创建 main 分支和初始提交，并将其添加到提交集合中。
     */
    public CommitManager() {
        commits = new HashMap<>();
        branches = new HashMap<>();
        headBranchName = "master";
        Commit initCommit = Commit.createInitCommit();
        addCommit(initCommit);
    }

    /* 将 manager 保存到 Repository.COMMIT_MANAGER 路径中 */
    public void save() {
        Utils.writeObject(Repository.COMMIT_MANAGER, this);
    }

    /* 获取分支名列表 */
    public List<String> getBranches() {
        List<String> branches = new ArrayList<>(this.branches.keySet());
        Collections.sort(branches);
        return branches;
    }

    /* 返回当前活跃的分支名 */
    public String headBranch() {
        return headBranchName;
    }

    /* 返回当前 head 指针指向的 Commit 对象 */
    public Commit getHeadCommit() {
        String headHash = branches.get(headBranchName);
        return getCommit(headHash);
    }

    /* 以 Map 形式返回所有 commit id 和 commit msg */
    public HashMap<String, String> getAllCommits() {
        return commits;
    }

    /* 切换当前 head 指针到指定分支上 */
    public void changeHeadTo(String branchName) {
        if (branches.containsKey(branchName)) {
            headBranchName = branchName;
        }
    }

    /* 传入 commit id，将对应的 commit 设置为 HEAD */
    public void resetHeadCommit(String id) {
        branches.put(headBranchName, id);
    }

    /* 传入 branch 名，返回该 branch 的最新 commit 对象，若 branch 不存在，返回 null */
    public Commit getBranchCommit(String branch) {
        String branchCommitHash = branches.get(branch);
        if (branchCommitHash == null) return null;
        return getCommit(branchCommitHash);
    }

    /* 根据 id 访问对应的 Commit 对象数据，如果不存在，返回 null */
    public Commit getCommit(String id) {
        File commitFile = Utils.join(Repository.COMMITS, id);
        if (!commitFile.exists()) return null;
        return Utils.readObject(commitFile, Commit.class);
    }

    /* 判断 manager 是否有指定分支名 */
    public boolean containsBranch(String branchName) {
        return branches.containsKey(branchName);
    }

    /**
     * 添加一个 Commit 对象到提交集合，并更新当前分支的最新提交指针。
     *
     * @param commit 要添加的提交对象
     */
    public void addCommit(Commit commit) {
        String id = commit.save();
        String commitMessage = commit.getMessage();
        commits.put(id, commitMessage);
        resetHeadCommit(id);
    }

    /* 创建新分支引用，成功创建返回 true，否则 false */
    public boolean createNewBranch(String branchName) {
        if (branches.containsKey(branchName)) return false;
        String headCommitHash = branches.get(headBranchName);
        branches.put(branchName, headCommitHash);
        return true;
    }

    /* 仅删除分支引用，不影响 commits */
    public void removeBranch(String branchName) {
        branches.remove(branchName);
    }

    /* 输入指定提交信息 msg，输出带有此信息的 commit id 列表 */
    public List<Commit> findByMessage(String msg) {
        ArrayList<Commit> res = new ArrayList<>();
        for (Map.Entry<String, String> entry: commits.entrySet()) {
            String id = entry.getKey();
            String commitMessage = entry.getValue();
            if (commitMessage.equals(msg)) {
                res.add(getCommit(id));
            }
        }
        return res;
    }

    /**
     * 查找两个提交的最近共同祖先（split point）。
     * 通过广度优先搜索第一个出现在另一个提交祖先集合中的节点。
     *
     * @param commitId1 第一个提交的 ID
     * @param commitId2 第二个提交的 ID
     * @return 最近公共祖先的 Commit 对象
     */
    public Commit findSplitPoint(String commitId1, String commitId2) {
        // 先获得 commitId1 的所有祖先
        Set<String> ancestors = getAllAncestors(commitId1);
        // 从 commitId2 向上遍历查找第一个出现在 ancestors 中的 commit
        Queue<String> queue = new LinkedList<>();
        queue.add(commitId2);
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            Commit currentCommit = getCommit(currentId);
            if (ancestors.contains(currentId)) {
                return currentCommit;
            }
            visited.add(currentId);
            List<String> parentIds = currentCommit.getParentIds();
            if (parentIds != null && !parentIds.isEmpty()) {
                for (String parentId : parentIds) {
                    if (!visited.contains(parentId)) {
                        queue.add(parentId);
                    }
                }
            }
        }
        return null;  // 理论上不可能没有公共祖先
    }

    /**
     * 获取指定提交的所有祖先提交 ID，包括其自身。
     *
     * @param commitId 起始提交的 ID
     * @return 包含所有祖先 ID 的集合
     */
    private Set<String> getAllAncestors(String commitId) {
        Set<String> ancestors = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        // 初始在队列加入元素自身
        queue.add(commitId);
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            ancestors.add(currentId);
            List<String> parentIds = getCommit(currentId).getParentIds();
            // 遍历找出 current 的父 commit id，
            if (parentIds != null && !parentIds.isEmpty()) {
                for (String parentId : parentIds) {
                    if (!ancestors.contains(parentId)) {
                        queue.add(parentId);
                    }
                }
            }
        }
        return ancestors;
    }
}
