package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public class CommitManager implements Serializable {
    // 使用哈希集合，只存放 Commit 对象的哈希值
    private HashSet<String> commitSet;

    /* HEAD 指针，指向当前活跃的分支名 */
    private String headRef;

    /* 分支指针 map，key为分支名，val为当前分支的最新 commit 哈希值*/
    private HashMap<String, String> branchRef;

    /**
     * 初始化 commit 图
     * 1，创建 head 指针，指向默认 main 分支
     * 2，创建 initCommit
     * 4，将 initCommit 加入 commitSet 中，main 指向它
     */
    public CommitManager() {
        commitSet = new HashSet<>();
        branchRef = new HashMap<>();
        headRef = "main";
        Commit initCommit = Commit.createInitCommit();
        addCommit(initCommit);
    }

    public void save() {
        if (!Repository.COMMIT_MANAGER.exists()) {
            Utils.createFile(Repository.COMMIT_MANAGER);
        }
        Utils.writeObject(Repository.COMMIT_MANAGER, this);
    }

    /* 返回当前 head 指针指向的 Commit 对象 */
    public Commit getHeadCommit() {
        String headHash = branchRef.get(headRef);
        return getCommit(headHash);
    }

    /* 根据哈希值访问对应的 Commit 对象数据 */
    public Commit getCommit(String hashcode) {
        File commitFile = Utils.join(Repository.COMMITS, hashcode);
        return Utils.readObject(commitFile, Commit.class);
    }

    public HashSet<String> getAllCommits() {
        return commitSet;
    }

    public boolean contains(String hashcode) {
        return commitSet.contains(hashcode);
    }

    /* 根据当前 commit 哈希值查找其第一父 commit 哈希值*/
    public String ParentHash(String hashcode) {
        if (!commitSet.contains(hashcode)) return null;
        Commit commit = getCommit(hashcode);
        return commit.getParentHash();
    }

    /* 添加一个 Commit 对象到 commit 图中 */
    public void addCommit(Commit commit) {
        String hash = commit.save();
        addCommit(hash);
    }
    /* 添加一个 Commit 哈希名称到 commit 图中 */
    public void addCommit(String commitHash) {
        commitSet.add(commitHash);
        branchRef.put(headRef, commitHash);
    }

    /* 创建新分支引用 */
    public boolean createNewBranch(String branchName) {
        if (branchRef.containsKey(branchName)) return false;
        String headCommitHash = branchRef.get(headRef);
        branchRef.put(branchName, headCommitHash);
        return true;
    }

    /* 切换当前 head 指针到指定分支上 */
    public boolean changeHead(String branchName) {
        if (branchRef.containsKey(branchName)) {
            headRef = branchName;
            return true;
        }
        return false;
    }

    /* 将指定分支的 Commit 合并到当前活跃分支上 */
    public void merge(String branchName) {
        if (branchRef.containsKey(branchName)) {
            String headCommitHash = branchRef.get(headRef);
            String branchCommitHash = branchRef.get(branchName);

            Commit headCommit = getCommit(headCommitHash);
            Commit branchCommit = getCommit(branchCommitHash);
            Commit newCommit = headCommit.merge(branchCommit);

            addCommit(newCommit);
        }
    }
}
