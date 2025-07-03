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
class Repository {

    /** The current working directory. */
    private static final File CWD = new File(System.getProperty("user.dir"));

    /** .gitlet 目录 */
    private static final File GITLET_DIR = join(CWD, ".gitlet");

    /** 暂存文件快照目录 */
    private static final File STAGING_BLOBS = join(GITLET_DIR, "staging");

    /** 永久文件快照目录 */
    private static final File BLOBS = join(GITLET_DIR, "blobs");

    /** commit 对象保存目录 */
    private static final File COMMITS = join(GITLET_DIR, "commits");

    /** commit 管理器和文件管理器保存路径 */
    private static final File COMMIT_MANAGER = join(GITLET_DIR, "CommitManager");
    private static final File FILE_MANAGER = join(GITLET_DIR, "fileManager");

    static File gitletDir() {
        return GITLET_DIR;
    }

    /**
     * 初始化版本库目录和初始提交。
     * 创建 .gitlet 目录及其子目录，并生成初始提交。
     */
    static void setup() {

        if (GITLET_DIR.exists()) {
            throw Utils.error("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        COMMITS.mkdir();
        STAGING_BLOBS.mkdir();
        BLOBS.mkdir();
        // 传入工作区域各文件目录的路径，创建 CommitManager，保存
        new CommitManager(COMMIT_MANAGER, COMMITS).save();
        // 传入工作区域各文件目录的路径，创建 fileManager，保存
        new FileManager(FILE_MANAGER, CWD,
                STAGING_BLOBS, BLOBS, COMMIT_MANAGER).save();
    }

    /**
     * 从磁盘读取并返回 CommitManager 对象。
     *
     * @return CommitManager 管理器
     */
    static CommitManager callCommitManager(File path) {
        return Utils.readObject(path, CommitManager.class);
    }

    /**
     * 从磁盘读取并返回 FileManager 对象，同时更新文件状态。
     *
     * @return FileManager 管理器
     */
    static FileManager callFileManager(File path) {
        FileManager manager = Utils.readObject(path, FileManager.class);
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
    static void addFile(String fileName) {
        // 如果 fileName 被 head 追踪且与追踪的内容相同，则确保它在 fileManager.addition 中不存在
        // 否则则将其加入 fileManager.addition 区（创建或覆盖）
        // 无论如何确保 fileName 在 fileManager.removal 区中不存在。

        Commit headCommit = callCommitManager(COMMIT_MANAGER).getHeadCommit();
        FileManager fileManager = callFileManager(FILE_MANAGER);

        // fileManager 利用 headCommit 和 fileName
        if (!fileManager.isInCWD(fileName)) {
            throw error("File does not exist.");
        }
        if (headCommit.isTrackingSameIn(CWD, fileName)) {
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
    static void remove(String fileName) {
        // 如果被 HEAD 追踪：就将 fileName 标记为待删除，并删除工作目录中的该文件。
        // 无论如何确保 fileName 在 fileManager.addition 区中不存在。

        Commit headCommit = callCommitManager(COMMIT_MANAGER).getHeadCommit();
        FileManager fileManager = callFileManager(FILE_MANAGER);

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
    static void commit(String commitMessage, String branch) {

        FileManager fileManager = callFileManager(FILE_MANAGER);
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
        newCommit.updateTrackingFiles(addition, removal, STAGING_BLOBS, BLOBS);

        commitManager.addCommit(newCommit);
        commitManager.save();

        fileManager.clearStageArea();
        fileManager.save();
    }

    /**
     * 打印当前分支的提交历史，从 HEAD 回溯到初始提交。
     */
    static void log() {
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
    static void globalLog() {
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z",
                Locale.US).withZone(ZoneId.systemDefault());
        String formattedTime = formatter.format(time); // 时间
        String commitMsg = commit.getMessage(); // 信息
        String commitId = commit.id(); // id
        List<String> parents = commit.getParentIds(); // 父提交对象列表

        message("===");
        message("commit %s", commitId);
        // 如果 commit 是 merge 后的 commit，则需要打印它的两个 parent id 缩写
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
    static void find(String msg) {
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
    static void checkout(String[] checkoutArgs) {
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Commit head = commitManager.getHeadCommit();
        FileManager fileManager = callFileManager(FILE_MANAGER);

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
                if (fileManager.isNotTracking(head, fileName)
                        && branchCommit.isTracking(fileName)) {
                    throw error("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
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
                if (commit == null) {
                    throw error("No commit with that id exists.");
                }
            }
            if (!commit.isTracking(fileName)) {
                throw error("File does not exist in that commit.");
            }
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
    static void branch(String branch) {
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
    static void rmBranch(String branch) {
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
    static void status() {
        FileManager fileManager = callFileManager(FILE_MANAGER);
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Commit head = commitManager.getHeadCommit();
        String headBranch = commitManager.headBranch();
        List<String> branches = commitManager.getBranches();
        List<String> stagingFiles = fileManager.getStagedFiles();
        List<String> removedFiles = fileManager.getRemovedFiles();
        List<String> modifiedFiles = fileManager.getModifiedFiles(head);
        List<String> untrackedFiles = fileManager.getUntrackedFiles(head);

        printStatus(headBranch, branches,
                stagingFiles, removedFiles,
                modifiedFiles, untrackedFiles);
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
    static void reset(String commitId) {
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        Commit commit = commitManager.getCommit(commitId);
        if (commit == null) {
            throw error("No commit with that id exists.");
        }
        FileManager fileManager = callFileManager(FILE_MANAGER);
        if (!fileManager.getUntrackedFiles(commitManager.getHeadCommit()).isEmpty()) {
            throw error("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
        }
        // 将工作区中的所有文件恢复成 commit 时的状态
        // 1 清空工作区
        clean(CWD);
        // 2 checkout
        fileManager.checkout(commit);
        // 3 重新设置 headCommit
        commitManager.setHeadCommit(commitId);
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
    static void merge(String branch) {
        // 如果当前暂存区非空，报错 "You have uncommitted changes."
        // 如果分支 branch 不存在，报错 "A branch with that name does not exist."
        // 如果当前分支与 branch 相同，报错 "Cannot merge a branch with itself."
        FileManager fileManager = callFileManager(FILE_MANAGER);
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
        // 如果分裂点与当前分支 headBranchName 指向的 commit 相同，
        // 操作 checkout(branchName)，打印"Current branch fast-forwarded."。
        Commit headCommit = commitManager.getHeadCommit();
        Commit branchCommit = commitManager.getBranchCommit(branch);
        String hId = headCommit.id();
        String bId = branchCommit.id();
        Commit splitPoint = commitManager.findSplitPoint(commitManager, hId, bId);
        if (splitPoint.id().equals(hId)) {
            checkout(new String[]{branch});
            message("Current branch fast-forwarded.");
        } else if (splitPoint.id().equals(bId)) {
            message("Given branch is an ancestor of the current branch.");
        } else {
            // 获取当前工作区中未被追踪的文件列表（用于冲突判断）
            List<String> untrackedFiles = fileManager.getUntrackedFiles(headCommit);

            // 创建合并管理器，传入分裂点、当前提交、目标分支提交、未追踪文件
            MergeManager mergeManager = new MergeManager(splitPoint, headCommit,
                    branchCommit, untrackedFiles,
                    CWD, BLOBS);

            // 执行合并逻辑，若过程中发现未追踪文件可能被覆盖，则终止合并
            boolean merged = mergeManager.merge();
            if (!merged) {
                throw error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }

            // 应用合并结果：处理冲突、删除冲突文件、还原合并结果文件
            mergeManager.handleConflict();
            mergeManager.doRemove();
            mergeManager.doCheckout();

            // 若存在冲突，输出提示信息
            if (mergeManager.encounteredConflict()) {
                message("Encountered a merge conflict.");
            }
            String newMsg = "Merged " + branch
                    + " into " + commitManager.headBranch() + ".";
            // 创建合并提交，附加两个父提交
            commit(newMsg, branch);
        }
    }

    /**
     * 添加一个远程仓库。
     * 该方法会将远程仓库的名称和路径记录到本地 CommitManager 中。
     * 若指定名称的远程仓库已存在，则抛出异常。
     *
     * @param remoteName     远程仓库名称（本地引用名）
     * @param remoteAddress  远程仓库的根路径（包含 .gitlet 目录）
     */
    static void addRemote(String remoteName, String remoteAddress) {
        // 检查是否已经存在 remoteName，存在则报错
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        if (commitManager.containsRemoteRepo(remoteName)) {
            throw error("A remote with that name already exists.");
        }

        // 打开 commitManager，记录远程仓库信息：仓库名和仓库的地址
        File remoteGitletDir = new File(remoteAddress);
        commitManager.addRemoteRepo(remoteName, remoteGitletDir);
        commitManager.save();
    }

    /**
     * 删除远程仓库记录。
     * 如果指定的远程仓库不存在，则抛出异常。
     *
     * @param remoteName 要删除的远程仓库名称
     */
    static void rmRemote(String remoteName) {
        CommitManager commitManager = callCommitManager(COMMIT_MANAGER);
        if (!commitManager.containsRemoteRepo(remoteName)) {
            throw error("A remote with that name does not exist.");
        }
        commitManager.rmRemoteRepo(remoteName);
        commitManager.save();
    }

    /**
     * 将当前分支的提交推送到远程仓库的指定分支。
     * 推送前会检查远程分支是否为当前提交的祖先，以确保推送安全。
     * 如果远程分支落后于本地但不是其祖先，将拒绝推送并提示需要先拉取（pull）。
     * 若远程分支不存在，则自动创建。
     *
     * @param remoteName        远程仓库名称
     * @param remoteBranchName  远程仓库中的分支名称
     */
    static void push(String remoteName, String remoteBranchName) {
        // 打开远程仓库和本地仓库各自的 commitManager，localCM 和 remoteCM
        CommitManager localCM = callCommitManager(COMMIT_MANAGER);
        File remoteGitletDir = localCM.getRemoteRepos().get(remoteName);
        if (!remoteGitletDir.exists()) {
            throw error("Remote directory not found.");
        }
        File remoteCMpath = join(remoteGitletDir, "CommitManager");
        CommitManager remoteCM = callCommitManager(remoteCMpath);

        // 检查 remoteCM 是否存在 remoteBranchName，没有则创建，并设置为 HEAD
        if (!remoteCM.containsBranch(remoteBranchName)) {
            remoteCM.createNewBranch(remoteBranchName);
            remoteCM.changeHeadTo(remoteBranchName);
        }

        // 查询 remoteCM 和 localCM 各自的 HEAD commit 的 splitPoint
        Commit remoteHead = remoteCM.getHeadCommit();
        Commit localHead = localCM.getHeadCommit();
        String rmId = remoteHead.id();
        String lcId = localHead.id();
        Commit splitPoint = localCM.findSplitPoint(remoteCM, rmId, lcId);

        // 如果 splitPoint 不是 remoteCM 的 HEAD commit，报错
        if (!remoteHead.id().equals(splitPoint.id())) {
            throw error("Please pull down remote changes before pushing.");
        }
        // 从 localCM 的 HEAD commit 开始，直到 split point（不包括），执行 remoteCM.addCommit(commit)
        // 因为 CommitManager 中的 commits 是集合形式，无需考虑加入顺序，只需在最后重置 HEAD 指向最新 Commit
        Commit cur = localHead;
        while (!cur.id().equals(splitPoint.id())) {
            remoteCM.addCommit(cur);
            String parentId = cur.getParentIds().get(0);
            cur = localCM.getCommit(parentId);
        }
        remoteCM.setHeadCommit(localHead.id());
        remoteCM.save();
    }

    /**
     * 从远程仓库拉取指定分支的提交记录和相关 blob 文件。
     * 拉取后，本地将创建一个名为 remoteName/remoteBranchName 的分支，
     * 其 HEAD 指向远程分支最新的提交。
     *
     * @param remoteName        远程仓库名称
     * @param remoteBranchName  远程分支名称
     */
    static void fetch(String remoteName, String remoteBranchName) {
        // 打开远程仓库和本地仓库各自的 commitManager 和 fileManager
        CommitManager localCM = callCommitManager(COMMIT_MANAGER);
        FileManager localFM = callFileManager(FILE_MANAGER);
        File remoteGitletDir = localCM.getRemoteRepos().get(remoteName);
        if (!remoteGitletDir.exists()) {
            throw error("Remote directory not found.");
        }
        File remoteCMpath = join(remoteGitletDir, "CommitManager");
        File remoteFMpath = join(remoteGitletDir, "fileManager");
        CommitManager remoteCM = callCommitManager(remoteCMpath);
        FileManager remoteFM = callFileManager(remoteFMpath);
        if (!remoteCM.containsBranch(remoteBranchName)) {
            throw error("That remote does not have that branch.");
        }

        // 查询 remoteCM 指定的分支上的 commit 和 localCM 的 HEAD commit 之间的 split point
        Commit remoteBranchCommit = remoteCM.getBranchCommit(remoteBranchName);
        Commit localHead = localCM.getHeadCommit();
        Commit splitPoint = remoteCM.findSplitPoint(localCM,
                localHead.id(), remoteBranchCommit.id());

        // 保存 localFM 原活跃分支名以备最后复原
        String orinBranch = localCM.headBranch();

        // localCM 创建新分支以储存远程 commit
        String remoteBranch = remoteName + "/" + remoteBranchName;
        if (!localCM.containsBranch(remoteBranch)) {
            localCM.createNewBranch(remoteBranch);
        }
        localCM.changeHeadTo(remoteBranch);

        // localCM 从 remoteCM 中复制所有从 remoteBranchCommit 回溯到 splitPoint（不包括）的所有 commit
        // localFM 从 remoteFM 中复制对应的 blob 文件
        Commit cur = remoteBranchCommit;
        while (!cur.id().equals(splitPoint.id())) {
            localCM.addCommit(cur);
            Map<String, String> trackingFiles = cur.getTrackedFile();
            for (String blobName : trackingFiles.values()) {
                localFM.fetchBlobFrom(remoteFM, blobName);
            }
            String parentId = cur.getParentIds().get(0);
            cur = remoteCM.getCommit(parentId);
        }
        // 最后设置 HEAD 指向最新 commit
        localCM.setHeadCommit(remoteBranchCommit.id());

        // 复原 localCM 分支 HEAD 状态
        localCM.changeHeadTo(orinBranch);
        localCM.save();
        localFM.save();
    }

    /**
     * 从远程仓库拉取指定分支（fetch），然后将其合并到当前分支（merge）。
     *
     * @param remoteName        远程仓库名称
     * @param remoteBranchName  远程分支名称
     */
    static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        merge(remoteName + "/" + remoteBranchName);
    }
}
