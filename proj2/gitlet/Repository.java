package gitlet;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
        /** .gitlet 结构 */
        public static final File GITLET_DIR = join(CWD, ".gitlet");
            /** 暂存文件快照 */
            public static final File STAGING_BLOBS = join(GITLET_DIR, "staging");
            /** 永久文件快照 */
            public static final File BLOBS = join(GITLET_DIR, "blobs");
            /** commit 对象 */
            public static final File COMMITS = join(GITLET_DIR, "commits");
            /** commit管理器和文件管理器 */
            public static final File COMMIT_MANAGER = join(GITLET_DIR, "CommitManager");
            public static final File FILE_MANAGER = join(GITLET_DIR, "fileManager");

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

    private static CommitManager callCommitManager() {
        return Utils.readObject(COMMIT_MANAGER, CommitManager.class);
    }
    private static FileManager callFileManager() {
        return Utils.readObject(FILE_MANAGER, FileManager.class);
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
        if (!Utils.hasFile(CWD, fileName)) {
            throw Utils.error("File does not exist.");
        }
        if (headCommit.isTrackingSame(fileName)) {
            fileManager.removeFromAddition(fileName);
        } else {
            fileManager.addToAddition(fileName);
        }
        fileManager.removeFromRemoval(fileName);
        fileManager.save();

//        // 1 读取 file 获取 hashcode
//        File file = join(CWD, fileName);
//        if (!file.exists()) {
//            throw Utils.error("File does not exist.");
//        }
//        String content = Utils.readContentsAsString(file);
//        String fileHash = Utils.sha1(fileName, content);
//
//        // 2 打开 headCommit，读取正在追踪的文件以及其 blob 是否与当前文件相同，如果相同则返回
//        CommitManager manager = callCommitManager();
//        Commit head = manager.getHeadCommit();
//        TreeMap<String, String> tracked = head.getTrackedFile();
//        if (tracked.containsKey(fileName) && tracked.get(fileName).equals(fileHash)) return;
//
//        // 3 打开 REMOVAL 区检查此文件是否之前在这里，如果在则删除
//        List<String> removals = Utils.plainFilenamesIn(REMOVAL);
//        if (removals != null && removals.contains(fileName)) {
//            join(REMOVAL, fileName).delete();
//        }
//
//        // 3 判断 STAGING_BLOBS 是否存在 fileHash 文件，无则创建写入 content
//        File blob = join(STAGING_BLOBS, fileHash);
//        if (!blob.exists()) {
//            Utils.createFile(blob);
//            Utils.writeContents(blob, content);
//        }
//
//        // 4 判断 ADDITION 是否已经存在 fileName 文件，无则创建写入 fileHash
//        File stagingFile = join(ADDITION, fileName);
//        if (!stagingFile.exists()) {
//            Utils.createFile(stagingFile);
//        }
//        Utils.writeContents(stagingFile, fileHash);
    }

    public static void commit(String commitMessage) {

        FileManager fileManager = callFileManager();
        Map<String, String> addition = fileManager.getAddition();
        Set<String> removal = fileManager.getRemoval();

        // 如果 addition 和 removal 都是空的，直接报错
        if (addition.isEmpty() && removal.isEmpty()) {
            throw Utils.error("No changes added to the commit.");
        }

        // 读取 head，创建子 commit 更新追踪状态，增加追踪或取消跟踪等
        CommitManager commitManager = callCommitManager();
        Commit headCommit = commitManager.getHeadCommit();
        Commit newCommit = headCommit.childCommit(commitMessage);
        for (Map.Entry<String, String> entry: addition.entrySet()) {
            String fileName = entry.getKey();
            String fileHash = entry.getValue();
            newCommit.trackFile(fileName, fileHash);
        }
        for (String fileTobeRemoved: removal) {
            newCommit.untrackFile(fileTobeRemoved);
        }
        commitManager.addCommit(newCommit);
        commitManager.save();

        fileManager.clearStageArea();
        fileManager.save();
//        // 1，读取 ADDITION 、REMOVAL 区中的所有文件名
//        List<String> stagingFiles = Utils.plainFilenamesIn(ADDITION);
//        List<String> removals = Utils.plainFilenamesIn(REMOVAL);
//        if ((stagingFiles.isEmpty()) && (removals.isEmpty())) {
//            System.out.println("No changes added to the commit.");
//            return;
//        }
//        // 2，如果 staging 不为空，则读取 head commit
//        CommitManager manager = callCommitManager();
//        Commit head = manager.getHeadCommit();
//        // 3，以 head 为 parent 创建子 commit
//        Commit newCommit = head.childCommit(
//                commitMessage, // commit message
//                Instant.now() // time
//                );
//
//        if (stagingFiles != null) {
//            for (String file: stagingFiles) {
//                // 4，newCommit 追踪 ADDITION 区中的文件
//                File filePath = join(ADDITION, file);
//                String fileHash = Utils.readContentsAsString(filePath);
//                newCommit.trackFile(file, fileHash);
//
//                // 5，BLOB 区复制 STAGING_BLOBS 中 file 文件所指向的 blob 文件
//                Utils.copyFile(fileHash, STAGING_BLOBS, BLOBS);
//            }
//        }
//        if (removals != null) {
//            // 6，如果 REMOVAL 区中有标记删除的文件，则在 newCommit 中取消跟踪
//            for (String file: removals) {
//                if (newCommit.isTracking(file)) {
//                    newCommit.untrackFile(file);
//                }
//            }
//        }
//        // 6，删除 ADDITION、REMOVAL 和 STAGING_BLOB 中的暂存文件
//        Utils.clean(ADDITION);
//        Utils.clean(STAGING_BLOBS);
//        Utils.clean(REMOVAL);
//        // 保存 newCommit，重新设置为 head
//        manager.addCommit(newCommit);
//        manager.save();
    }

    public static void remove(String fileName) {
        // 如果被 HEAD 追踪：就将 fileName 标记为待删除，并删除工作目录中的该文件。
        // 无论如何确保 fileName 在 fileManager.addition 区中不存在。

        Commit headCommit = callCommitManager().getHeadCommit();
        FileManager fileManager = callFileManager();
        if (!headCommit.isTracking(fileName) && !fileManager.isStaging(fileName)) {
            throw Utils.error("No reason to remove the file.");
        }
        if (headCommit.isTracking(fileName)) {
            fileManager.addToRemoval(fileName);
            Utils.deleteFileFrom(CWD, fileName);
        }
        fileManager.removeFromAddition(fileName);
        fileManager.save();
//        CommitManager manager = callCommitManager();
//        Commit head = manager.getHeadCommit();
//        TreeMap<String, String> trackingFiles = head.getTrackedFile();
//        List<String> stagingFiles = Utils.plainFilenamesIn(ADDITION);
//
//        boolean inTrackingFiles = trackingFiles.containsKey(fileName);
//        boolean inStagingFiles = (stagingFiles != null && stagingFiles.contains(fileName));
//
//        if (!inStagingFiles && !inTrackingFiles) {
//            throw Utils.error("No reason to remove the file.");
//        }
//
//        if (inTrackingFiles) {
//            Utils.createFile(join(REMOVAL, fileName));
//            Utils.restrictedDelete(join(CWD, fileName));
//        }
//
//        if (inStagingFiles) {
//            join(ADDITION, fileName).delete();
//        }
    }

    /* 按顺序打印从 HEAD 所在的分支的 commit 回溯到 init commit 的提交信息（不打印其他分支） */
    public static void log() {
        CommitManager manager = callCommitManager();
        Commit cur = manager.getHeadCommit();

        while (true) {
            // Print current commit details
            printLog(cur);
            // Determine the parent commit
            String parentId = manager.ParentHash(cur.getId());
            if (parentId == null || !manager.containsCommit(parentId)) {
                break;
            }
            cur = manager.getCommit(parentId);
        }
    }

    /* 无序打印所有分支的所有 commit */
    public static void globalLog() {
        CommitManager manager = callCommitManager();
        HashSet<String> allCommits = manager.getAllCommits();
        for (String commitHash: allCommits) {
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
        String commitId = commit.getId();

        Utils.message("===");
        Utils.message("commit %s", commitId);
        Utils.message("Date: %s", formattedTime);
        Utils.message("%s", commitMsg);
        System.out.println();

    }

    public static void find(String msg) {
        boolean found = false;
        CommitManager manager = callCommitManager();
        HashSet<String> allCommits = manager.getAllCommits();
        for (String commitHash: allCommits) {
            Commit commit = manager.getCommit(commitHash);
            String commitMsg = commit.getMessage();
            if (msg.equals(commitMsg)) {
                found = true;
                System.out.println(commit.getId());
            }
        }
        if (!found) {
            throw Utils.error("Found no commit with that message.");
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
                throw Utils.error("No such branch exists.");
            }
            // 如果你试图切换到你已经在的分支，没有必要做任何事，直接报错退出。
            if (branch.equals(commitManager.headBranch())) {
                throw Utils.error("No need to checkout the current branch.");
            }
            // 如果工作目录中有某个文件，它：
            //	•	没有被当前分支追踪（就是你之前没 add、也没 commit），
            //	•	但是在目标分支中存在该文件（会被 checkout 覆盖），
            //→ 这种情况下要报错退出，不能执行 checkout。防止覆盖用户未保存的修改。
            List<String> files = Utils.plainFilenamesIn(CWD);
            for (String fileName: files) {
                if (!head.isTracking(fileName) && branchCommit.isTrackingDifferent(fileName)) {
                    throw Utils.error("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
            // 以上异常检查通过后，从“目标分支的最新 commit”中提取所有文件，把它们放进工作目录（CWD）中，如果工作目录已经存在这些文件，就覆盖它们。
            // 这个命令执行完后，当前分支（HEAD）会切换成你指定的分支。
            // 如果有些文件被当前分支追踪、但在目标分支的最新 commit 中没有这些文件，那么这些文件会从工作目录中删除。
            // 切换分支时 staging 区会被清空（除非你 checkout 的正好是当前分支）。
            Map<String, String> branchTrackingFiles = branchCommit.getTrackedFile();
            Map<String, String> headTrackingFiles = head.getTrackedFile();
            for (String fileName: headTrackingFiles.keySet()) {
                if (head.isTracking(fileName) && !branchCommit.isTracking(fileName)) {
                    Utils.deleteFileFrom(CWD, fileName);
                }
            }
            for (String fileName: branchTrackingFiles.keySet()) {
                FileManager.checkout(branchCommit, fileName);
            }
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
                if (commit == null) throw Utils.error("No commit with that id exists.");
            }
            if (!commit.isTracking(fileName)) throw Utils.error("File does not exist in that commit.");
            FileManager.checkout(commit, fileName);
        }
        commitManager.save();
        fileManager.save();
    }

    public static void branch(String branch) {
        CommitManager manager = callCommitManager();
        boolean created =  manager.createNewBranch(branch);
        if (created) {
            manager.save();
        } else {
            throw Utils.error("A branch with that name already exists.");
        }
    }

    public static void rmBranch(String branch) {
        CommitManager commitManager = callCommitManager();
        if (branch.equals(commitManager.headBranch())) {
            throw Utils.error("Cannot remove the current branch.");
        }
        if (!commitManager.containsBranch(branch)) {
            throw Utils.error("A branch with that name does not exist.");
        }
        commitManager.removeBranch(branch);
        commitManager.save();
    }
}
