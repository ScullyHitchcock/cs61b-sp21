package gitlet;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
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

    /* 供测试用 */
    public static void refreshCWDForUnitTest() {
        CWD = new File(System.getProperty("user.dir"));
        GITLET_DIR = join(CWD, ".gitlet");
        STAGING_BLOBS = join(GITLET_DIR, "staging");
        BLOBS = join(GITLET_DIR, "blobs");
        COMMITS = join(GITLET_DIR, "commits");
        COMMIT_MANAGER = join(GITLET_DIR, "CommitManager");
        FILE_MANAGER = join(GITLET_DIR, "fileManager");
    }

    /** "init" 命令：初始化 gitlet
     * 创建.gitlet目录和目录下的 commits 文件夹
     * 初始化 commit tree，创建 init commit
     * 将 commit tree 数据写入 commits 下的 CommitTree文件
     */
    public static void setup() {

        if (GITLET_DIR.exists()) {
            throw Utils.error("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        COMMITS.mkdir();
        STAGING_BLOBS.mkdir();
        BLOBS.mkdir();
        // 创建CommitManager，保存
        CommitManager manager = new CommitManager();
        manager.save();
        // 创建CWDManager，保存
        FileManager fileManager = new FileManager();
        fileManager.save();
    }

    public static CommitManager callCommitManager() {
        return Utils.readObject(COMMIT_MANAGER, CommitManager.class);
    }
    public static FileManager callFileManager() {
        FileManager manager = Utils.readObject(FILE_MANAGER, FileManager.class);
        manager.updateFiles();
        return manager;
    }

    /** 1 如果没有被最新 commit 追踪则加入暂存区（创建或覆盖）
     *  2 如果被最新 commit 追踪，则删除暂存区的内容（无论在不在）
     *  3 最后删除文件在暂删区的记录（无论在不在）
     * @param fileName
     */
    public static void addFile(String fileName) {
        // 如果 fileName 被 head 追踪且与追踪的内容相同，则确保它在 fileManager.addition 中不存在
        // 否则则将其加入 fileManager.addition 区（创建或覆盖）
        // 无论如何确保 fileName 在 fileManager.removal 区中不存在。

        Commit headCommit = callCommitManager().getHeadCommit();
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

    public static void remove(String fileName) {
        // 如果被 HEAD 追踪：就将 fileName 标记为待删除，并删除工作目录中的该文件。
        // 无论如何确保 fileName 在 fileManager.addition 区中不存在。

        Commit headCommit = callCommitManager().getHeadCommit();
        FileManager fileManager = callFileManager();

        if ((!fileManager.isInCWD(fileName)) || (fileManager.isNotTracking(headCommit, fileName))) {
            throw error("No reason to remove the file.");
        }
        if (headCommit.isTracking(fileName)) {
            fileManager.addToRemoval(fileName);
            restrictedDelete(join(CWD, fileName));
        }
        fileManager.removeFromAddition(fileName);
        fileManager.save();
    }

    public static void commit(String commitMessage, String branch) {

        FileManager fileManager = callFileManager();
        Map<String, String> addition = fileManager.getAddition();
        Map<String, String> removal = fileManager.getRemoval();

        // 如果 addition 和 removal 都是空的，直接报错
        if (addition.isEmpty() && removal.isEmpty()) {
            throw error("No changes added to the commit.");
        }

        // 读取 head，创建子 commit 更新追踪状态，增加追踪或取消跟踪等
        CommitManager commitManager = callCommitManager();
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

    /* 按顺序打印从 HEAD 所在的分支的 commit 回溯到 init commit 的提交信息（不打印其他分支） */
    public static void log() {
        CommitManager manager = callCommitManager();
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

    /* 无序打印所有分支的所有 commit */
    public static void globalLog() {
        CommitManager manager = callCommitManager();
        Map<String, String> allCommits = manager.getAllCommits();
        for (String commitHash: allCommits.keySet()) {
            printLog(manager.getCommit(commitHash));
        }
    }

    /* 打印单个 commit 信息 */
    private static void printLog(Commit commit) {
        Instant time = commit.getTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z", Locale.US)
                .withZone(ZoneId.systemDefault());
        String formattedTime = formatter.format(time);
        String commitMsg = commit.getMessage();
        String commitId = commit.id();

        message("===");
        message("commit %s", commitId);
        message("Date: %s", formattedTime);
        message("%s", commitMsg);
        System.out.println();

    }

    /* 打印全部 commit message 为 msg 的 commit 的 id 字符串 */
    public static void find(String msg) {
        CommitManager manager = callCommitManager();
        List<Commit> commitsWithMsg = manager.findByMessage(msg);
        if (commitsWithMsg.isEmpty()) {
            throw error("Found no commit with that message.");
        }
        for (Commit commit: commitsWithMsg) {
            System.out.println(commit.id());
        }
    }

    public static void checkout(String[] checkoutArgs) {
        CommitManager commitManager = callCommitManager();
        Commit head = commitManager.getHeadCommit();
        FileManager fileManager = callFileManager();
        if (checkoutArgs.length == 1) {
            // checkout [branch name]
            // Takes all files in the commit at the head of the given branch, and puts them in the working directory,
            // overwriting the versions of the files that are already there if they exist.
            // Also, at the end of this command, the given branch will now be considered the current branch (HEAD).
            // Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
            // The staging area is cleared, unless the checked-out branch is the current branch (see Failure cases below).
            String branch = checkoutArgs[0];
            Commit branchCommit = commitManager.getBranchCommit(branch);

            // 如果你输入的分支名在分支列表中找不到，直接报错，不要进行任何操作。
            if (!commitManager.containsBranch(branch)) {
                throw error("No such branch exists.");
            }
            // 如果你试图切换到你已经在的分支，没有必要做任何事，直接报错退出。
            if (branch.equals(commitManager.headBranch())) {
                throw error("No need to checkout the current branch.");
            }
            // 如果工作目录中有某个文件，它：
            //	•	没有被当前分支追踪（就是你之前没 add、也没 commit），
            //	•	但是在目标分支中存在该文件（会被 checkout 覆盖），
            //→ 这种情况下要报错退出，不能执行 checkout。防止覆盖用户未保存的修改。
            List<String> files = plainFilenamesIn(CWD);
            for (String fileName: files) {
                if (fileManager.isNotTracking(head, fileName) && branchCommit.isTracking(fileName)) {
                    throw error("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
            // 以上异常检查通过后，从“目标分支的最新 commit”中提取所有文件，把它们放进工作目录（CWD）中，如果工作目录已经存在这些文件，就覆盖它们。
            // 这个命令执行完后，当前分支（HEAD）会切换成你指定的分支。
            // 如果有些文件被当前分支追踪、但在目标分支的最新 commit 中没有这些文件，那么这些文件会从工作目录中删除。
            // 切换分支时 staging 区会被清空（除非你 checkout 的正好是当前分支）。
            Map<String, String> headTrackingFiles = head.getTrackedFile();
            for (String fileName: headTrackingFiles.keySet()) {
                if (head.isTracking(fileName) && !branchCommit.isTracking(fileName)) {
//                    deleteFileFrom(CWD, fileName);
                    restrictedDelete(join(CWD, fileName));
                }
            }
            FileManager.checkout(branchCommit);
            commitManager.changeHeadTo(branch);
            fileManager.clearStageArea();
        }
        else {
            String fileName;
            Commit commit;
            if (checkoutArgs.length == 2) {
                // checkout -- [file name]
                // Takes the version of the file as it exists in the head commit and puts it in the working directory,
                // overwriting the version of the file that’s already there if there is one.
                // The new version of the file is not staged.
                fileName = checkoutArgs[1];
                commit = head;
            } else {
                // checkout [commit id] -- [file name]
                // Takes the version of the file as it exists in the commit with the given id,
                // and puts it in the working directory,
                // overwriting the version of the file that’s already there if there is one.
                // The new version of the file is not staged.
                fileName = checkoutArgs[2];
                commit = commitManager.getCommit(checkoutArgs[0]);
                if (commit == null) throw error("No commit with that id exists.");
            }
            if (!commit.isTracking(fileName)) throw error("File does not exist in that commit.");
            FileManager.checkout(commit, fileName);
        }
        commitManager.save();
        fileManager.save();
    }

    /* 新建名为 branch 的分支 */
    public static void branch(String branch) {
        CommitManager manager = callCommitManager();
        boolean created =  manager.createNewBranch(branch);
        if (created) {
            manager.save();
        } else {
            throw error("A branch with that name already exists.");
        }
    }

    /* 删除名为 branch 的分支 */
    public static void rmBranch(String branch) {
        CommitManager commitManager = callCommitManager();
        if (branch.equals(commitManager.headBranch())) {
            throw error("Cannot remove the current branch.");
        }
        if (!commitManager.containsBranch(branch)) {
            throw error("A branch with that name does not exist.");
        }
        commitManager.removeBranch(branch);
        commitManager.save();
    }

    /* 打印当前 gitlet 目前正在管理的所有文件的状态 */
    public static void status() {
        FileManager fileManager = callFileManager();
        CommitManager commitManager = callCommitManager();
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

    /* 选择特定 commit，将当前工作区全部内容恢复成该 commit 追踪的状态，重新设置该 commit 为 HEAD。
    *  创建 commit 追踪但 CWD 不存在的新文件，覆盖同名文件，删除 commit 没有追踪但 CWD 存在的文件。*/
    public static void reset(String commitId) {
        CommitManager commitManager = callCommitManager();
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
        FileManager.checkout(commit);
        // 3 重新设置 headCommit
        commitManager.resetHeadCommit(commitId);
        // 4 清空暂存区
        fileManager.clearStageArea();
        // 5 保存
        fileManager.save();
        commitManager.save();
    }


    /* 1  */
    public static void merge(String branch) {
        // 如果当前暂存区非空，报错 "You have uncommitted changes."
        // 如果分支 branch 不存在，报错 "A branch with that name does not exist."
        // 如果当前分支与 branch 相同，报错 "Cannot merge a branch with itself."
        FileManager fileManager = callFileManager();
        CommitManager commitManager = callCommitManager();
        Map<String, String> addition = fileManager.getAddition();
        Map<String, String> removal = fileManager.getRemoval();
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
        } else {
            List<String> untrackedFIles = fileManager.getUntrackedFiles(headCommit);
            MergeManager mergeManager = new MergeManager(splitPoint, headCommit, branchCommit, untrackedFIles);
            boolean merged =  mergeManager.merge();
            if (!merged) {
                throw error("There is an untracked file in the way; delete it, or add and commit it first.");
            }
            // 操作工作区文件，进行各种暂存逻辑操作
            mergeManager.handleConflict();
            mergeManager.doRemove();
            mergeManager.doCheckout();

            // commit
            commit("Merged " + branch + " into " + commitManager.headBranch() + ".", branch);
        }
    }
}
