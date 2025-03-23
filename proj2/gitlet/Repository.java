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
            /** 暂存区 */
            public static final File STAGING = join(GITLET_DIR, "staging");
                public static final File ADDITION = join(STAGING, "files");
                public static final File REMOVAL = join(STAGING, "removal");
                public static final File STAGING_BLOBS = join(STAGING, "blobs");
    /** 文件快照 */
    public static final File BLOBS = join(GITLET_DIR, "blobs");
    /** commit 对象 */
    public static final File COMMITS = join(GITLET_DIR, "commits");
    public static final File COMMIT_MANAGER = join(GITLET_DIR, "manager");

    /** "init" 命令：初始化 gitlet
     * 创建.gitlet目录和目录下的 commits 文件夹
     * 初始化 commit tree，创建 init commit
     * 将 commit tree 数据写入 commits 下的 CommitTree文件
     */
    public static void setup() {
        if (GITLET_DIR.exists()) {
            throw Utils.error("A Gitlet version-control system already exists in the current directory.");
        }
        // 新建 .gitlet 文件夹
        GITLET_DIR.mkdir();
        // 新建 commits 文件夹
        COMMITS.mkdir();
        // 创建暂存区 staging area。
        STAGING.mkdir();
        STAGING_BLOBS.mkdir();
        ADDITION.mkdir();
        REMOVAL.mkdir();
        // 创建 blobs 区
        BLOBS.mkdir();
        // 创建CommitManager，保存
        CommitManager manager = new CommitManager();
        manager.save();
    }

    private static CommitManager callManager() {
        return Utils.readObject(COMMIT_MANAGER, CommitManager.class);
    }

    /** 描述：将文件当前版本复制到暂存区（请参阅 commit 命令的描述）。
     * 因此，添加文件也称为将文件暂存以供添加。将已暂存的文件暂存会用新的内容覆盖暂存区中的先前条目。
     * 暂存区应位于 .gitlet 中。如果文件当前工作版本与当前提交中的版本相同，则不要将其暂存以供添加，
     * 如果它已在暂存区中（例如，当文件被修改、添加，然后更改回其原始版本时可能会发生这种情况），则将其从暂存区中删除。
     * 如果该文件在命令执行时已暂存以供删除（请参阅 gitlet rm ），则它将不再被暂存以供删除。
     *
     * @param fileName
     */
    public static void addFile(String fileName) {
        // 1 读取 file 获取 hashcode
        File file = join(CWD, fileName);
        if (!file.exists()) {
            throw Utils.error("File does not exist.");
        }
        String content = Utils.readContentsAsString(file);
        String fileHash = Utils.sha1(fileName, content);

        // 2 打开 head，读取正在追踪的文件以及其 blob 是否与当前文件相同，如果相同则返回
        CommitManager manager = callManager();
        Commit head = manager.getHeadCommit();
        TreeMap<String, String> tracked = head.getTrackedFile();
        if (tracked.containsKey(fileName) && tracked.get(fileName).equals(fileHash)) return;

        // 3 打开 REMOVAL 区检查此文件是否之前在这里，如果在则删除
        List<String> removals = Utils.plainFilenamesIn(REMOVAL);
        if (removals != null && removals.contains(fileName)) {
            join(REMOVAL, fileName).delete();
        }

        // 3 判断 STAGING_BLOBS 是否存在 fileHash 文件，无则创建写入 content
        File blob = join(STAGING_BLOBS, fileHash);
        if (!blob.exists()) {
            Utils.createFile(blob);
            Utils.writeContents(blob, content);
        }

        // 4 判断 ADDITION 是否已经存在 fileName 文件，无则创建写入 fileHash
        File stagingFile = join(ADDITION, fileName);
        if (!stagingFile.exists()) {
            Utils.createFile(stagingFile);
        }
        Utils.writeContents(stagingFile, fileHash);
    }

    public static void commit(String commitMessage) {
        // 1，读取 ADDITION 、REMOVAL 区中的所有文件名
        List<String> stagingFiles = Utils.plainFilenamesIn(ADDITION);
        List<String> removals = Utils.plainFilenamesIn(REMOVAL);
        if ((stagingFiles.isEmpty()) && (removals.isEmpty())) {
            System.out.println("No changes added to the commit.");
            return;
        }
        // 2，如果 staging 不为空，则读取 head commit
        CommitManager manager = callManager();
        Commit head = manager.getHeadCommit();
        // 3，以 head 为 parent 创建子 commit
        Commit newCommit = head.childCommit(
                commitMessage, // commit message
                Instant.now() // time
                );

        if (stagingFiles != null) {
            for (String file: stagingFiles) {
                // 4，newCommit 追踪 ADDITION 区中的文件
                File filePath = join(ADDITION, file);
                String fileHash = Utils.readContentsAsString(filePath);
                newCommit.trackFile(file, fileHash);

                // 5，BLOB 区复制 STAGING_BLOBS 中 file 文件所指向的 blob 文件
                Utils.copyFile(fileHash, STAGING_BLOBS, BLOBS);
            }
        }
        if (removals != null) {
            // 6，如果 REMOVAL 区中有标记删除的文件，则在 newCommit 中取消跟踪
            for (String file: removals) {
                if (newCommit.isTracking(file)) {
                    newCommit.untrackFile(file);
                }
            }
        }
        // 6，删除 ADDITION、REMOVAL 和 STAGING_BLOB 中的暂存文件
        Utils.clean(ADDITION);
        Utils.clean(STAGING_BLOBS);
        Utils.clean(REMOVAL);
        // 保存 newCommit，重新设置为 head
        manager.addCommit(newCommit);
        manager.save();
    }

    public static void remove(String fileName) {
        // 只要在暂存区：就把文件从暂存区中移除。
        // 只要被 HEAD 追踪：就将文件标记为待删除，并删除工作目录中的该文件。

        CommitManager manager = callManager();
        Commit head = manager.getHeadCommit();
        TreeMap<String, String> trackingFiles = head.getTrackedFile();
        List<String> stagingFiles = Utils.plainFilenamesIn(ADDITION);

        boolean inTrackingFiles = trackingFiles.containsKey(fileName);
        boolean inStagingFiles = (stagingFiles != null && stagingFiles.contains(fileName));

        if (!inStagingFiles && !inTrackingFiles) {
            throw Utils.error("No reason to remove the file.");
        }

        if (inTrackingFiles) {
            Utils.createFile(join(REMOVAL, fileName));
            Utils.restrictedDelete(join(CWD, fileName));
        }

        if (inStagingFiles) {
            join(ADDITION, fileName).delete();
        }

    }

    /* 按顺序打印从 HEAD 所在的分支的 commit 回溯到 init commit 的提交信息（不打印其他分支） */
    public static void log() {
        CommitManager manager = callManager();
        Commit head = manager.getHeadCommit();
        Commit cur = head;

        while (true) {
            // Print current commit details
            printLog(cur);
            // Determine the parent commit
            String parentHash = manager.ParentHash(cur.getHashcode());
            if (parentHash == null || !manager.containsCommit(parentHash)) {
                break;
            }
            cur = manager.getCommit(parentHash);
        }
    }

    /* 无序打印所有分支的所有 commit */
    public static void globalLog() {
        CommitManager manager = callManager();
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
        String hashcode = commit.getHashcode();

        Utils.message("===");
        Utils.message("commit %s", hashcode);
        Utils.message("Date: %s", formattedTime);
        Utils.message("%s", commitMsg);
        System.out.println();

    }

    public static void find(String msg) {
        boolean found = false;
        CommitManager manager = callManager();
        HashSet<String> allCommits = manager.getAllCommits();
        for (String commitHash: allCommits) {
            Commit commit = manager.getCommit(commitHash);
            String commitMsg = commit.getMessage();
            if (msg.equals(commitMsg)) {
                found = true;
                System.out.println(commit.getHashcode());
            }
        }
        if (!found) {
            throw Utils.error("Found no commit with that message.");
        }
    }

    public static void checkout(String[] checkoutArgs) {
        if (checkoutArgs.length == 1) {
            // checkout [branch name]
            // Takes all files in the commit at the head of the given branch, and puts them in the working directory,
            // overwriting the versions of the files that are already there if they exist.
            // Also, at the end of this command, the given branch will now be considered the current branch (HEAD).
            // Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
            // The staging area is cleared, unless the checked-out branch is the current branch (see Failure cases below).


        } else if (checkoutArgs.length == 2) {
            // checkout -- [file name]
            // Takes the version of the file as it exists in the head commit and puts it in the working directory,
            // overwriting the version of the file that’s already there if there is one.
            // The new version of the file is not staged.

        } else {
            // checkout [commit id] -- [file name]
            // Takes the version of the file as it exists in the commit with the given id,
            // and puts it in the working directory,
            // overwriting the version of the file that’s already there if there is one.
            // The new version of the file is not staged.

        }
    }

    public static void branch(String branch) {
        CommitManager manager = callManager();
        boolean created =  manager.createNewBranch(branch);
        if (created) {
            manager.changeHeadTo(branch);
        } else {
            throw Utils.error("A branch with that name already exists.");
        }
    }

    public static void rmBranch(String branch) {
        CommitManager manager = callManager();
        if (branch.equals(manager.headBranch())) {
            throw Utils.error("Cannot remove the current branch.");
        }
        if (!manager.containsBranch(branch)) {
            throw Utils.error("A branch with that name does not exist.");
        }
        manager.removeBranch(branch);
        manager.save();
    }
}
