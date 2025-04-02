package gitlet;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static gitlet.Utils.*;

/**
 * 表示一个 Gitlet 仓库的操作接口类。
 * 提供对版本库的所有核心命令操作，包括 init、add、commit、checkout、branch、reset、merge 等。
 * 同时维护工作目录状态、暂存区、提交历史和分支指针。
 *
 *  @author CST
 */
public class Repository {

    /** The current working directory. */
    public static File CWD = new File(System.getProperty("user.dir"));
        /** .gitlet 结构 */
        public static File GITLET_DIR = join(CWD, ".gitlet");
            /** 暂存文件快照 */
            public static File STAGING_BLOBS = join(GITLET_DIR, "staging");
            /** 永久文件快照 */
            public static File BLOBS = join(GITLET_DIR, "blobs");
            /** commit 对象 */
            public static File COMMITS = join(GITLET_DIR, "commits");
            /** commit管理器和文件管理器 */
            public static File COMMIT_MANAGER = join(GITLET_DIR, "CommitManager");
            public static File FILE_MANAGER = join(GITLET_DIR, "fileManager");

    /** 仅供测试用 */
    public static void refreshCWDForUnitTest() {
        CWD = new File(System.getProperty("user.dir"));
        GITLET_DIR = join(CWD, ".gitlet");
        STAGING_BLOBS = join(GITLET_DIR, "staging");
        BLOBS = join(GITLET_DIR, "blobs");
        COMMITS = join(GITLET_DIR, "commits");
        COMMIT_MANAGER = join(GITLET_DIR, "CommitManager");
        FILE_MANAGER = join(GITLET_DIR, "fileManager");
    }

    /**
     * 初始化版本库目录和初始提交。
     * 创建 .gitlet 目录及其子目录，并生成初始提交。
     */
    public static void setup() {

        if (GITLET_DIR.exists()) {
            throw Utils.error("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        COMMITS.mkdir();
        STAGING_BLOBS.mkdir();
        BLOBS.mkdir();
        // 传入工作区域各文件目录的路径，创建 CommitManager，保存
        CommitManager manager = new CommitManager(COMMIT_MANAGER, COMMITS);
        manager.save();
        // 传入工作区域各文件目录的路径，创建 fileManager，保存
        FileManager fileManager = new FileManager(FILE_MANAGER, CWD, STAGING_BLOBS, BLOBS, COMMIT_MANAGER);
        fileManager.save();
    }

    /**
     * 从磁盘读取并返回 CommitManager 对象。
     *
     * @return CommitManager 管理器
     */
    public static CommitManager callCommitManager(File path) {
        return Utils.readObject(path, CommitManager.class);
    }

    /**
     * 从磁盘读取并返回 FileManager 对象，同时更新文件状态。
     *
     * @return FileManager 管理器
     */
    public static FileManager callFileManager() {
        FileManager manager = Utils.readObject(FILE_MANAGER, FileManager.class);
        manager.updateFiles();
        return manager;
    }

    /**
     * 将文件添加到暂存区：
     * - 若文件被追踪且未发生变化，则从 addition 区移除；
     * - 否则加入 addition；
     * - 无论如何从 removal 区移除。
     *
     * @param fileName 文件名
     */
    public static void addFile(String fileName) {
        // 如果 fileName 被 head 追踪且与追踪的内容相同，则确保它在 fileManager.addition 中不存在
        // 否则则将其加入 fileManager.addition 区（创建或覆盖）
        // 无论如何确保 fileName 在 fileManager.removal 区中不存在。

        Commit headCommit = callCommitManager(COMMIT_MANAGER).getHeadCommit();
        FileManager fileManager = callFileManager();

        // fileManager 利用 headCommit 和 fileName
        if (!fileManager.isInCWD(fileName)) {
            throw error("File does not exist.");
        }
        if (headCommit.isTrackingSame(fileName)) {
            fileManager.removeFromAddition(fileName);
        } else {
            fileManager.addToAddition(fileName);
        }
        fileManager.removeFromRemoval(fileName);
        fileManager.save();
    }

    /**
     * 将文件标记为删除（加入 removal）并从工作目录中删除。
     * 如果文件既不在暂存区也未被追踪，则报错。
     *
     * @param fileName 文件名
     */
    public static void remove(String fileName) {
        // 如果被 HEAD 追踪：就将 fileName 标记为待删除，并删除工作目录中的该文件。
        // 无论如何确保 fileName 在 fileManager.addition 区中不存在。

        Commit headCommit = callCommitManager(COMMIT_MANAGER).getHeadCommit();
        FileManager fileManager = callFileManager();

        if ((!fileManager.isStagingInAdd(fileName)) && (!headCommit.isTracking(fileName))) {
            throw error("No reason to remove the file.");
        }
        if (headCommit.isTracking(fileName)) {
            fileManager.addToRemoval(fileName);
            restrictedDelete(join(CWD, fileName));
        }
        fileManager.removeFromAddition(fileName);
        fileManager.save();
    }

    /**
     * 提交当前暂存的改动，创建一个新的提交对象并更新分支指针。
     *
     * @param commitMessage 提交信息
     * @param branch        若为合并提交，提供第二父提交所在分支名
     */
    public static void commit(String commitMessage, String branch) {

        FileManager fileManager = callFileManager();
        Map<String, String> addition = fileManager.getAddition();
        Set<String> removal = fileManager.getRemoval();

        if (commitMessage == null || commitMessage.isEmpty()) {
            throw error("Please enter a commit message.");
        }

        // 如果 addition 和 removal 都是空的，直接报错
        if (addition.isEmpty() && removal.isEmpty()) {
            throw error("No changes added to the commit.");
        }

        // 读取 head，创建子 commit 更新追踪状态，增加追踪或取消跟踪等
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Commit headCommit = commitManager.getHeadCommit();
        Commit newCommit = headCommit.childCommit(commitMessage);
        if (branch != null) {
            Commit branchCommit = commitManager.getBranchCommit(branch);
            newCommit.addParent(branchCommit.id());
        }
        newCommit.updateTrackingFiles(addition, removal);

        commitManager.addCommit(newCommit);
        commitManager.save();

        fileManager.clearStageArea();
        fileManager.save();
    }

    /**
     * 打印当前分支的提交历史，从 HEAD 回溯到初始提交。
     */
    public static void log() {
        CommitManager manager = callCommitManager(COMMIT_MANAGER);
        Commit cur = manager.getHeadCommit();

        while (true) {
            // Print current commit details
            printLog(cur);
            // Determine the parent commit
            List<String> parents = cur.getParentIds();
            if (parents.isEmpty()) {
                break;
            }
            String parentId = cur.getParentIds().get(0);
            cur = manager.getCommit(parentId);
        }
    }

    /**
     * 打印所有分支的所有提交历史。
     */
    public static void globalLog() {
        CommitManager manager = callCommitManager(COMMIT_MANAGER);
        Map<String, String> allCommits = manager.getAllCommits();
        for (String commitHash: allCommits.keySet()) {
            printLog(manager.getCommit(commitHash));
        }
    }

    /**
     * 打印单个提交的详细信息。
     *
     * @param commit 提交对象
     */
    private static void printLog(Commit commit) {
        Instant time = commit.getTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z", Locale.US)
                .withZone(ZoneId.systemDefault());
        String formattedTime = formatter.format(time);
        String commitMsg = commit.getMessage();
        String commitId = commit.id();
        List<String> parents = commit.getParentIds();

        message("===");
        message("commit %s", commitId);
        if (parents.size() > 1) {
            String abbrP1 = parents.get(0).substring(0, 7);
            String abbrP2 = parents.get(1).substring(0, 7);
            message("Merge: %s %s", abbrP1, abbrP2);
        }
        message("Date: %s", formattedTime);
        message("%s", commitMsg);
        System.out.println();
    }

    /**
     * 查找所有提交信息等于 msg 的提交 ID，并打印它们。
     *
     * @param msg 提交信息
     */
    public static void find(String msg) {
        CommitManager manager = callCommitManager(COMMIT_MANAGER);
        List<Commit> commitsWithMsg = manager.findByMessage(msg);
        if (commitsWithMsg.isEmpty()) {
            throw error("Found no commit with that message.");
        }
        for (Commit commit: commitsWithMsg) {
            System.out.println(commit.id());
        }
    }

    /**
     * 根据参数执行分支或文件版本的切换操作。
     *
     * @param checkoutArgs 切换参数
     */
    public static void checkout(String[] checkoutArgs) {
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Commit head = commitManager.getHeadCommit();
        FileManager fileManager = callFileManager();

        if (checkoutArgs.length == 1) {
            // checkout [branch name]
            // 切换到指定分支
            String branch = checkoutArgs[0];
            Commit branchCommit = commitManager.getBranchCommit(branch);

            if (!commitManager.containsBranch(branch)) {
                throw error("No such branch exists.");
            }
            if (branch.equals(commitManager.headBranch())) {
                throw error("No need to checkout the current branch.");
            }
            List<String> files = plainFilenamesIn(CWD);
            for (String fileName : files) {
                if (fileManager.isNotTracking(head, fileName) && branchCommit.isTracking(fileName)) {
                    throw error("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
            for (String fileName : head.getTrackedFile().keySet()) {
                if (!branchCommit.isTracking(fileName)) {
                    restrictedDelete(join(CWD, fileName));
                }
            }
            fileManager.checkout(branchCommit);
            commitManager.changeHeadTo(branch);
            fileManager.clearStageArea();

        } else {
            // 文件版本恢复
            String fileName;
            Commit commit;
            // checkout -- [file name]
            if (checkoutArgs.length == 2) {
                fileName = checkoutArgs[1];
                commit = head;
            } else {
                // checkout [commit id] -- [file name]
                fileName = checkoutArgs[2];
                commit = commitManager.getCommit(checkoutArgs[0]);
                if (commit == null) throw error("No commit with that id exists.");
            }
            if (!commit.isTracking(fileName)) throw error("File does not exist in that commit.");
            fileManager.checkout(commit, fileName);
        }

        commitManager.save();
        fileManager.save();
    }

    /**
     * 新建分支。
     *
     * @param branch 分支名称
     */
    public static void branch(String branch) {
        CommitManager manager = callCommitManager(COMMIT_MANAGER);
        boolean created =  manager.createNewBranch(branch);
        if (created) {
            manager.save();
        } else {
            throw error("A branch with that name already exists.");
        }
    }

    /**
     * 删除分支。
     *
     * @param branch 分支名称
     */
    public static void rmBranch(String branch) {
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        if (branch.equals(commitManager.headBranch())) {
            throw error("Cannot remove the current branch.");
        }
        if (!commitManager.containsBranch(branch)) {
            throw error("A branch with that name does not exist.");
        }
        commitManager.removeBranch(branch);
        commitManager.save();
    }

    /**
     * 展示当前所有状态，包括分支、暂存区、未追踪文件等。
     */
    public static void status() {
        FileManager fileManager = callFileManager();
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Commit head = commitManager.getHeadCommit();
        String headBranch = commitManager.headBranch();
        List<String> branches = commitManager.getBranches();
        List<String> stagingFiles = fileManager.getStagedFiles();
        List<String> removedFiles = fileManager.getRemovedFiles();
        List<String> modifiedFiles = fileManager.getModifiedFiles(head);
        List<String> untrackedFiles = fileManager.getUntrackedFiles(head);

        printStatus(headBranch, branches, stagingFiles, removedFiles, modifiedFiles, untrackedFiles);
    }

    /**
     * 打印当前工作区状态，包括分支、暂存文件、已移除文件等。
     *
     * @param headBranch    当前活跃分支名称（标记为 *）
     * @param branches      分支名称列表，包括所有分支名
     * @param stagingFiles  暂存文件列表
     * @param removedFiles  标记为已移除的文件列表
     * @param modifiedFiles 修改但未暂存的文件列表
     * @param untrackedFiles 未被追踪的文件列表
     */
    private static void printStatus(String headBranch,
                                    List<String> branches,
                                    List<String> stagingFiles,
                                    List<String> removedFiles,
                                    List<String> modifiedFiles,
                                    List<String> untrackedFiles) {
        // 打印分支信息
        message("=== Branches ===");
        for (String branch : branches) {
            if (branch.equals(headBranch)) {
                message("*" + branch); // 当前分支加上 "*" 标识
            } else {
                message(branch);
            }
        }
        System.out.println(); // 空行

        // 打印暂存文件信息
        message("=== Staged Files ===");
        if (!stagingFiles.isEmpty()) {
            for (String file : stagingFiles) {
                message(file);
            }
        }
        System.out.println(); // 空行

        // 打印已移除文件信息
        message("=== Removed Files ===");
        if (!removedFiles.isEmpty()) {
            for (String file : removedFiles) {
                message(file);
            }
        }
        System.out.println(); // 空行

        // 打印修改未暂存文件信息
        message("=== Modifications Not Staged For Commit ===");
        if (!modifiedFiles.isEmpty()) {
            for (String file : modifiedFiles) {
                message(file);
            }
        }
        System.out.println(); // 空行

        // 打印未追踪文件信息
        message("=== Untracked Files ===");
        if (!untrackedFiles.isEmpty()) {
            for (String file : untrackedFiles) {
                message(file);
            }
        }
    }

    /**
     * 切换 HEAD 指向指定提交，并还原工作目录至该提交的状态。
     *
     * @param commitId 提交 ID
     */
    public static void reset(String commitId) {
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Commit commit = commitManager.getCommit(commitId);
        if (commit == null) {
            throw error("No commit with that id exists.");
        }
        FileManager fileManager = callFileManager();
        if (!fileManager.getUntrackedFiles(commitManager.getHeadCommit()).isEmpty()) {
            throw error("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        // 将工作区中的所有文件恢复成 commit 时的状态
        // 1 清空工作区
        clean(CWD);
        // 2 checkout
        fileManager.checkout(commit);
        // 3 重新设置 headCommit
        commitManager.resetHeadCommit(commitId);
        // 4 清空暂存区
        fileManager.clearStageArea();
        // 5 保存
        fileManager.save();
        commitManager.save();
    }


    /**
     * 合并指定分支到当前分支，并处理冲突与合并提交。
     *
     * @param branch 分支名称
     */
    public static void merge(String branch) {
        // 如果当前暂存区非空，报错 "You have uncommitted changes."
        // 如果分支 branch 不存在，报错 "A branch with that name does not exist."
        // 如果当前分支与 branch 相同，报错 "Cannot merge a branch with itself."
        FileManager fileManager = callFileManager();
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Map<String, String> addition = fileManager.getAddition();
        Set<String> removal = fileManager.getRemoval();
        if (!addition.isEmpty() || !removal.isEmpty()) {
            throw error("You have uncommitted changes.");
        }
        if (!commitManager.containsBranch(branch)) {
            throw error("A branch with that name does not exist.");
        }
        if (branch.equals(commitManager.headBranch())) {
            throw error("Cannot merge a branch with itself.");
        }

        // 找出两个分支的分裂点 commit。
        // 如果分裂点与目标分支 branchName 指向的 commit 相同，
        // 什么都不用做，打印"Given branch is an ancestor of the current branch."直接退出。
        // 如果分裂点与当前分支 headBranchName 指向的 commit 相同，操作 checkout(branchName)，打印"Current branch fast-forwarded."。
        Commit headCommit = commitManager.getHeadCommit();
        Commit branchCommit = commitManager.getBranchCommit(branch);
        String hId = headCommit.id();
        String bId = branchCommit.id();
        Commit splitPoint = commitManager.findSplitPoint(hId, bId);
        if (splitPoint.id().equals(hId)) {
            checkout(new String[]{branch});
            message("Current branch fast-forwarded.");
        } else if (splitPoint.id().equals(bId)) {
            message("Given branch is an ancestor of the current branch.");
        }

        else {
            // 获取当前工作区中未被追踪的文件列表（用于冲突判断）
            List<String> untrackedFiles = fileManager.getUntrackedFiles(headCommit);

            // 创建合并管理器，传入分裂点、当前提交、目标分支提交、未追踪文件
            MergeManager mergeManager = new MergeManager(splitPoint, headCommit, branchCommit, untrackedFiles);

            // 执行合并逻辑，若过程中发现未追踪文件可能被覆盖，则终止合并
            boolean merged = mergeManager.merge();
            if (!merged) {
                throw error("There is an untracked file in the way; delete it, or add and commit it first.");
            }

            // 应用合并结果：处理冲突、删除冲突文件、还原合并结果文件
            mergeManager.handleConflict();
            mergeManager.doRemove();
            mergeManager.doCheckout();

            // 若存在冲突，输出提示信息
            if (mergeManager.encounteredConflict()) {
                message("Encountered a merge conflict.");
            }

            // 创建合并提交，附加两个父提交
            commit("Merged " + branch + " into " + commitManager.headBranch() + ".", branch);
        }
    }
}
