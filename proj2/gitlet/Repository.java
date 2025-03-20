package gitlet;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

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
    public static final File STAGING = join(GITLET_DIR, "staging");
    public static final File STAGING_FILES = join(STAGING, "files");
    public static final File STAGING_BLOBS = join(STAGING, "blobs");
    public static final File BLOBS = join(GITLET_DIR, "blobs");
    public static final File COMMITS = join(GITLET_DIR, "commits");

    public static final File HEAD = join(COMMITS, "head");
    public static final String USER_NAME = "User";

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
        STAGING_FILES.mkdir();
        // 创建 blobs 区
        BLOBS.mkdir();
        // 创建初始 commit
        Commit initCommit = new Commit();
        // 将 initCommit 写入文件，获得哈希码文件名
        String initHash = initCommit.save(COMMITS);
        // 设置为 head commit
        initCommit.setToBeHead();
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
        String hashCode = Utils.sha1(fileName, content);

        // 2 打开 head，读取正在追踪的文件以及其 blob 是否与当前文件相同，如果相同则返回
        Commit head = Utils.readObject(HEAD, Commit.class);
        HashMap<String, String> tracked = head.getTrack();
        if (tracked.containsKey(fileName) && tracked.get(fileName).equals(hashCode)) return;

        // 3 判断 STAGING_BLOBS 是否存在 hashcode 文件，无则创建写入 content
        File blob = join(STAGING_BLOBS, hashCode);
        if (!blob.exists()) {
            Utils.createFile(blob);
            Utils.writeContents(blob, content);
        }

        // 4 判断 STAGING_FILES 是否已经存在 fileName 文件，无则创建写入 hashcode
        File stagingFile = join(STAGING_FILES, fileName);
        if (!stagingFile.exists()) {
            Utils.createFile(stagingFile);
        }
        Utils.writeContents(stagingFile, hashCode);
    }

    public static void commit(String commitMessage) {
        // 1，读取 STAGING_FILES 区中的所有文件名
        List<String> stagingFiles = Utils.plainFilenamesIn(STAGING_FILES);
        if (stagingFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        // 2，如果 staging不为空，则读取 head commit
        Commit head = Utils.readObject(HEAD, Commit.class);
        // 3，以 head 为 parent 创建子 commit
        Commit newCommit = head.childCommit(
                commitMessage, // commit message
                Instant.now(), // time
                head.getHashcode()); // parent 的 hashcode 文件名

        for (String file: stagingFiles) {
            // 4，newCommit 追踪 STAGING_FILES 区中的文件
            File filePath = join(STAGING_FILES, file);
            String fileHash = Utils.readContentsAsString(filePath);
            newCommit.trackFile(file, fileHash);

            // 5，BLOB 区复制 STAGING_BLOBS 中 file 文件所指向的 blob 文件
            File stagingBlob = join(STAGING_BLOBS, fileHash);
            String content = Utils.readContentsAsString(stagingBlob);
            File blob = join(BLOBS, fileHash);
            if (!blob.exists()) {
                Utils.createFile(blob);
                Utils.writeContents(blob, content);
            }
        }
        // 6，删除 STAGING_FILE 和 STAGING_BLOB 中的暂存文件
        Utils.clean(STAGING_FILES);
        Utils.clean(STAGING_BLOBS);
        // 保存 newCommit，重新设置为 head
        String hashName = newCommit.save(COMMITS);
        newCommit.setToBeHead();
    }

    public static void remove(String fileName) {
        // 如果文件既不在暂存区里，也没有被当前提交追踪，那么就打印错误信息：No reason to remove the file.
        // 若文件当前在暂存区中（staged for addition）：
        //      则将其从暂存区移除。
        //      若文件在当前提交（HEAD）中被追踪（tracked）：
        //          将它标记为待删除（staged for removal），也就是告诉 Gitlet 在下次提交时把它从版本库的追踪列表里移除。
        //          同时，从工作目录中删除该文件（如果它还存在的话）。
        //      否则不要删除
    }
}
