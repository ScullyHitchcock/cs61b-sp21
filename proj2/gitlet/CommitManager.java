package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * 管理所有提交（Commit）对象和分支信息。
 * 负责维护提交图、分支指针、HEAD 状态、远程仓库地址等数据。
 */
public class CommitManager implements Serializable {
    /** CommitManager 保存路径 */
    private final File savePath;

    /** Commit 文件存放目录 */
    private final File commitDir;

    /** 远程仓库地址 */
    private final HashMap<String, File> remoteRepos;

    /** 存放 Commit id 与 Commit msg 的映射 */
    private final HashMap<String, String> commits;

    /** HEAD 指针，指向当前活跃的分支名，默认为 master */
    private String headBranchName;

    /** 分支，key 为分支名，val 为分支当前的最新 Commit 的 id */
    private final HashMap<String, String> branches;

    /**
     * 初始化 CommitManager。
     * 创建 main 分支和初始提交，并将其添加到提交集合中。
     * 创建初始 commit 并保存
     */
    public CommitManager(File savePath, File commitDir) {
        this.savePath = savePath;
        this.commitDir = commitDir;
        commits = new HashMap<>();
        branches = new HashMap<>();
        remoteRepos = new HashMap<>();
        headBranchName = "master";
        Commit initCommit = Commit.createInitCommit();
        addCommit(initCommit);
    }

    /** 将 manager 保存到 savePath 路径中 */
    public void save() {
        Utils.writeObject(savePath, this);
    }

    /** 获取分支名列表 */
    public List<String> getBranches() {
        List<String> branchList = new ArrayList<>(this.branches.keySet());
        Collections.sort(branchList);
        return branchList;
    }

    /** 返回 HEAD 指向的分支名 */
    public String headBranch() {
        return headBranchName;
    }

    /** 返回 HEAD 指向的 Commit 对象 */
    public Commit getHeadCommit() {
        String headId = branches.get(headBranchName);
        return getCommit(headId);
    }

    /** 以 Map 形式返回所有 commit id 和 commit msg */
    public HashMap<String, String> getAllCommits() {
        return new HashMap<>(commits);
    }

    /** 切换当前 HEAD 指针到指定分支上 */
    public void changeHeadTo(String branchName) {
        if (branches.containsKey(branchName)) {
            headBranchName = branchName;
        }
    }

    /** 传入 commit id，将对应的 commit 设置为 HEAD */
    public void setHeadCommit(String id) {
        branches.put(headBranchName, id);
    }

    /**
     * 通过分支名查找当前分支的最新 Commit 对象
     *
     * @param branch 分支名
     * @return 分支下最新的 Commit 对象，如果分支不存在，返回 null
     */
    public Commit getBranchCommit(String branch) {
        String branchCommitId = branches.get(branch);
        if (branchCommitId == null) {
            return null;
        }
        return getCommit(branchCommitId);
    }

    /**
     * 根据 commit id 或 id 的前几个字符串访问对应的 Commit 对象数据
     *
     * @param id commit id 或 id 的前几个字符串
     * @return 查找成功返回 Commit 对象，失败返回 null
     */
    public Commit getCommit(String id) {
        String matchId = null;
        if (commits.containsKey(id)) {
            matchId = id;
        } else { // 模糊查找
            int matchCount = 0;
            for (String commitId : commits.keySet()) {
                if (commitId.startsWith(id)) {
                    matchCount += 1;
                    matchId = commitId;
                }
            }
            if (matchCount != 1) {
                return null;
            }
        }
        File commitFile = Utils.join(commitDir, matchId);
        if (!commitFile.exists()) {
            return null;
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /** 判断 manager 是否有指定分支名 */
    public boolean containsBranch(String branchName) {
        return branches.containsKey(branchName);
    }

    /**
     * 添加一个新 Commit 对象到 commits 中，保存对象，使 HEAD 指向它。
     *
     * @param commit 要添加的提交对象
     */
    public void addCommit(Commit commit) {
        String id = commit.id();
        if (!commits.containsKey(id)) {
            String commitMessage = commit.getMessage();
            commits.put(id, commitMessage);
            setHeadCommit(id);
            commit.save(commitDir);
        }
    }

    /**
     * 创建新分支，指向 HEAD commit
     *
     * @param branchName 分支名
     * @return 成功创建返回 true，否则 false
     */
    public boolean createNewBranch(String branchName) {
        if (containsBranch(branchName)) {
            return false;
        }
        String headCommitHash = branches.get(headBranchName);
        branches.put(branchName, headCommitHash);
        return true;
    }

    /** 删除分支，不影响 commits */
    public void removeBranch(String branchName) {
        branches.remove(branchName);
    }

    /**
     * 通过提交信息查找 Commit 对象
     *
     * @param msg 提交信息
     * @return Commit 对象列表，当没有对象时返回空列表
     */
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
     * 查找两个 Commit 对象的最近共同祖先（split point），支持输入不同仓库中的 Commit 对象。
     * 通过广度优先搜索第一个出现在另一个提交祖先集合中的节点。
     *
     * @param otherCM CommitManager 对象
     * @param commitId1 otherCM 保存的 Commit 对象 ID
     * @param commitId2 本地 CommitManager 保存的 Commit 对象 ID
     * @return 两者最近公共祖先的 Commit 对象
     */
    public Commit findSplitPoint(CommitManager otherCM, String commitId1, String commitId2) {
        // 先获得 otherCM 里的 commitId1 的所有祖先
        Set<String> ancestors = otherCM.getAllAncestors(commitId1);
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
    public Set<String> getAllAncestors(String commitId) {
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

    /**
     * 创建新远程仓库，保存到 remoteRepos 中
     *
     * @param remoteName 远程仓库名
     * @param remoteCM 远程仓库地址
     */
    public void addRemoteRepo(String remoteName, File remoteCM) {
        remoteRepos.put(remoteName, remoteCM);
    }

    /**
     * 删除远程仓库
     *
     * @param remoteName 仓库名
     */
    public void rmRemoteRepo(String remoteName) {
        remoteRepos.remove(remoteName);
    }

    /** 判断是否保存了指定远程仓库 */
    public boolean containsRemoteRepo(String remoteName) {
        return remoteRepos.containsKey(remoteName);
    }

    /** 返回远程仓库 Map */
    public HashMap<String, File> getRemoteRepos() {
        return new HashMap<>(remoteRepos);
    }
}
