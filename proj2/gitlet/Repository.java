package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
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
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File STAGING_AREA = join(GITLET_DIR, "staging");
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
        STAGING_AREA.mkdir();
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
        // 3，读取 fileName 文件以及文件内容 content
        File file = join(CWD, fileName);
        if (!file.exists()) {
            throw Utils.error("File does not exist.");
        }
        String content = Utils.readContentsAsString(file);

        // 4，以文件名+内容为输入创建一串 hascode
        String hashCode = Utils.sha1(fileName, content);
        // 5，检测 blobs 区中是否已经存在此 hashcode，如果存在则跳过，如果不存在则
        // 在 blobs 中创建名为：hashcode，内容为 content 的 blob 文件
        File blobFile = join(BLOBS, hashCode);
        if (!blobFile.exists()) {
            try {
                blobFile.createNewFile();
            } catch (IOException e) {
                System.out.println("创建文件时发生错误：" + e.getMessage());
            }
            Utils.writeContents(blobFile, content);
        }
        // 6，在 staging 区中创建同名文件，内容为 hashcode
        File stagingFile = join(STAGING_AREA, fileName);
        if (!stagingFile.exists()) {
            try {
                stagingFile.createNewFile();
            } catch (IOException e) {
                System.out.println("创建文件时发生错误：" + e.getMessage());
            }
        }
        // 7，staging 文件内容为 blob 区文件的哈希码文件名
        Utils.writeContents(stagingFile, hashCode);
    }

    public static void commit(String commitMassage) {
        // 1，读取 staging 区中的所有文件名
        List<String> files = Utils.plainFilenamesIn(STAGING_AREA);
        if (files == null) {
            System.out.println("No changes added to the commit.");
            return;
        }
        // 2，如果 staging不为空，则读取 head commit
        Commit head = Utils.readObject(HEAD, Commit.class);
        // 3，以 head 为 parent 创建子 commit
        Commit newCommit = head.childCommit(
                commitMassage, // commit message
                Instant.now(), // time
                head.getHashcode()); // parent 的 hashcode 文件名
        // 4，追踪 staging 区中的所有文件然后保存到本地，最后删除 staging 区中文件
        for (String file: files) {
            File filePath = join(STAGING_AREA, file);
            String blobName = Utils.readContentsAsString(filePath);
            newCommit.trackFile(file, blobName);
            filePath.delete();
        }
        String hashName = newCommit.save(COMMITS);
        newCommit.setToBeHead();
    }

    /* TODO: fill in the rest of this class. */
}
