package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Commit 管理器
 */
public class CommitManager implements Serializable {
    // 使用哈希集合，只存放 Commit 对象的哈希值
    private HashSet<String> commits;

    /* HEAD 指针，指向当前活跃的分支名 */
    private String headBranchName;

    /* 分支指针 map，key为分支名，val为当前分支的最新 commit 哈希值*/
    private HashMap<String, String> branchs;

    /**
     * 初始化 manager
     * 1，创建 head 指针，指向默认 main 分支
     * 2，创建 initCommit
     * 4，将 initCommit 加入 commits 中，main 指向它
     */
    public CommitManager() {
        commits = new HashSet<>();
        branchs = new HashMap<>();
        headBranchName = "main";
        Commit initCommit = Commit.createInitCommit();
        addCommit(initCommit);
    }

    /* 将 manager 保存到 Repository.COMMIT_MANAGER 路径中 */
    public void save() {
        if (!Repository.COMMIT_MANAGER.exists()) {
            Utils.createFile(Repository.COMMIT_MANAGER);
        }
        Utils.writeObject(Repository.COMMIT_MANAGER, this);
    }

    /* 返回当前活跃的分支名 */
    public String headBranch() {
        return headBranchName;
    }

    /* 返回 HEAD 分支的 commit 哈希码 */
    public String getHeadHash() {
        return branchs.get(headBranchName);
    }

    /* 返回当前 head 指针指向的 Commit 对象 */
    public Commit getHeadCommit() {
        String headHash = getHeadHash();
        return getCommit(headHash);
    }

    /* 根据哈希值访问对应的 Commit 对象数据 */
    public Commit getCommit(String hashcode) {
        File commitFile = Utils.join(Repository.COMMITS, hashcode);
        return Utils.readObject(commitFile, Commit.class);
    }

    /* 以列表形式返回所有 commit 哈希码 */
    public HashSet<String> getAllCommits() {
        return commits;
    }

    /* 判断 manager 是否储存了指定 commit 哈希码 */
    public boolean containsCommit(String hashcode) {
        return commits.contains(hashcode);
    }

    /* 判断 manager 是否有指定分支名 */
    public boolean containsBranch(String branchName) {
        return branchs.containsKey(branchName);
    }

    /* 根据当前 commit 哈希值查找其第一父 commit 哈希值*/
    public String ParentHash(String hashcode) {
        if (!commits.contains(hashcode)) return null;
        Commit commit = getCommit(hashcode);
        return commit.getParentHash();
    }

    /* 添加一个 Commit 对象到 manager 中 */
    public void addCommit(Commit commit) {
        String hash = commit.save();
        addCommit(hash);
    }
    /* 添加一个 Commit 哈希名称到 manager 中 */
    public void addCommit(String commitHash) {
        commits.add(commitHash);
        branchs.put(headBranchName, commitHash);
    }

    /* 创建新分支引用，成功创建返回 true，否则 false */
    public boolean createNewBranch(String branchName) {
        if (branchs.containsKey(branchName)) return false;
        String headCommitHash = branchs.get(headBranchName);
        branchs.put(branchName, headCommitHash);
        return true;
    }

    /* 仅删除分支引用，不影响 commits，成功删除返回 true，否则 false */
    public boolean removeBranch(String branchName) {
        if (!branchs.containsKey(branchName)) return false;
        branchs.remove(branchName);
        return true;
    }

    /* 切换当前 head 指针到指定分支上 */
    public boolean changeHeadTo(String branchName) {
        if (branchs.containsKey(branchName)) {
            headBranchName = branchName;
            return true;
        }
        return false;
    }

    /* 将指定分支的 Commit 合并到当前活跃分支上 */
    public void merge(String branchName) {
        if (branchs.containsKey(branchName)) {
            String headCommitHash = branchs.get(headBranchName);
            String branchCommitHash = branchs.get(branchName);

            Commit headCommit = getCommit(headCommitHash);
            Commit branchCommit = getCommit(branchCommitHash);
            Commit newCommit = headCommit.merge(branchCommit);

            addCommit(newCommit);
        }
    }
}
