package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * Commit 管理器
 */
public class CommitManager implements Serializable {
    // 使用哈希集合，只存放 Commit 对象的哈希值
    private HashMap<String, String> commits;

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
        commits = new HashMap<>();
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

    /* 获取分支名列表 */
    public List<String> getBranches() {
        List<String> branches = new ArrayList<>(branchs.keySet());
        Collections.sort(branches);
        return branches;
    }

    /* 返回当前活跃的分支名 */
    public String headBranch() {
        return headBranchName;
    }

    /* 返回 HEAD 分支的 commit id */
    public String getHeadHash() {
        return branchs.get(headBranchName);
    }

    /* 返回当前 head 指针指向的 Commit 对象 */
    public Commit getHeadCommit() {
        String headHash = getHeadHash();
        return getCommit(headHash);
    }

    /* 传入 commit id，将对应的 commit 设置为 HEAD */
    public void resetHeadCommit(String id) {
        if (commits.containsKey(id)) {
            branchs.put(headBranchName, id);
        }
    }

    /* 传入 branch 名，返回该 branch 的最新 commit 对象，若 branch 不存在，返回 null */
    public Commit getBranchCommit(String branch) {
        String branchCommitHash = branchs.get(branch);
        if (branchCommitHash == null) return null;
        return getCommit(branchCommitHash);
    }

    /* 根据 id 访问对应的 Commit 对象数据，如果不存在，返回 null */
    public Commit getCommit(String id) {
        File commitFile = Utils.join(Repository.COMMITS, id);
        if (!commitFile.exists()) return null;
        return Utils.readObject(commitFile, Commit.class);
    }

    /* 以 Map 形式返回所有 commit id 和 commit msg */
    public HashMap<String, String> getAllCommits() {
        return commits;
    }

    /* 判断 manager 是否储存了指定 commit id */
    public boolean containsCommit(String id) {
        return commits.containsKey(id);
    }

    /* 判断 manager 是否有指定分支名 */
    public boolean containsBranch(String branchName) {
        return branchs.containsKey(branchName);
    }

    /* 根据当前 commit id 返回其第一个父 commit id，如果 id 不存在或没有父 commit，返回 null */
    public String ParentId(String id) {
        if (!commits.containsKey(id)) return null;
        Commit commit = getCommit(id);
        List<String> parents = commit.getParentIds();
        if (parents.isEmpty()) return null;
        return commit.getParentIds().get(0);
    }

    /* 添加一个 Commit 对象到 manager 中 */
    public void addCommit(Commit commit) {
        String id = commit.save();
        String commitMessage = commit.getMessage();
        addCommit(id, commitMessage);
    }
    /* 添加一个 Commit 哈希名称到 manager 中 */
    public void addCommit(String id, String msg) {
        commits.put(id, msg);
        branchs.put(headBranchName, id);
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

    /* 将指定分支的 Commit 合并到当前活跃分支上
    *  1 找出两个分支的分裂点 commit。
    *    a 如果分裂点与目标分支 branchName 指向的 commit 相同，
    *      什么都不用做，打印"Given branch is an ancestor of the current branch."直接退出。
    *    b 如果分裂点与当前分支 headBranchName 指向的 commit 相同，操作 checkout(branchName)，打印"Current branch fast-forwarded."。
    *  2 如果分裂点不是上述两种情况，对于每一个文件：
    *    a 如果某个文件 fileName 在“给定分支” givenBranch（被 merge 进来的分支）的内容和分裂点的版本不同，
    *      但在“当前分支” currentBranch（执行 merge 的分支）中的内容与分裂点一致，
    *      则 checkout(givenBranch.commit, fileName) -> addFile(fileName)。
    *    b 如果反过来，currentBranch 与分裂点不同，givenBranch 与分裂点相同，则什么都不用做。
    *    c 如果给定分支与当前分支的特定文件的修改方式完全一致（fileHash 相同），或以不再追踪，则什么都不用做。
    *    d 如果给定分支与当前分支两者都追踪了分裂点没有的新文件，则什么都不用做。
    *    e 如果给定分支出现了分裂点没有的新文件，而当前分支没有出现，则 checkout(givenBranch.commit, fileName) -> addFile(fileName)。
    *    f 如果给定分支删除了分裂点有的文件，而当前分支没有删除，则 remove(fileName)。
    *    g 如果当前分支删除了分裂点有的文件，而给定分支没有删除，则什么都不用做。
    *    h 如果文件在当前分支和给定分支都被改动了，但改法不同，则产生冲突：
    *      a 两边都修改了，但结果不同；
    *      b 一边改了内容，一边删掉了；
    *      c 文件原来 split point 没有，但两边新增了不同版本。
    * 3 处理冲突，将文件的内容覆盖为以下格式：
    *   <<<<<<< HEAD
    *   contents of file in current branch
    *   =======
    *   contents of file in given branch
    *   >>>>>>>
    * */
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
}
