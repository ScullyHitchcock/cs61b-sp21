package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * FileManager 管理以下三大区域的所有文件：
 * - 工作目录（CWD）
 * - 暂存区（分为 addition 和 removal）
 * - 特定 commit（通常是 HEAD）
 *
 * 主要职责：
 * - 操作暂存区（添加、删除文件）
 * - 检测文件状态（已修改、已删除、未追踪）
 * - 恢复文件到特定 commit 的状态
 */
public class FileManager implements Serializable {
    /** 保存路径 */
    private final File savePath;

    /** 工作目录 */
    private final File workingDir;

    /** 暂存区 blob 文件保存目录 */
    private final File stagingBlobsDir;

    /** blob 文件保存目录 */
    private final File blobsDir;

    /** CommitManager 文件路径 */
    private final File commitManagerPath;

    /** addition 记录，以“文件名 -> 文件哈希值”的形式记录特定文件 */
    private Map<String, String> addition;

    /** removal 记录，以“文件名”的形式记录特定文件 */
    private Set<String> removal;

    /** 管理区中的所有文件集合，包括工作区的文件和head正在追踪的文件的并集 */
    private Set<String> filesInManagement;

    /**
     * FileManager 构造函数，用于初始化文件管理器的各个路径和暂存区结构。
     *
     * @param savePath           FileManager 对象的保存路径（用于序列化保存）
     * @param workingDir         当前工作目录路径，代表用户项目的根目录
     * @param stagingBlobsDir    暂存区 blob 文件保存目录（用于记录 add 的文件内容）
     * @param blobsDir           所有 blob 文件的存储目录（版本库中所有文件快照）
     * @param commitManagerPath  CommitManager 的序列化文件路径，用于获取当前 HEAD commit
     */
    public FileManager(File savePath, File workingDir,
                       File stagingBlobsDir, File blobsDir, File commitManagerPath) {
        this.savePath = savePath;
        this.workingDir = workingDir;
        this.stagingBlobsDir = stagingBlobsDir;
        this.blobsDir = blobsDir;
        this.commitManagerPath = commitManagerPath;
        addition = new HashMap<>();
        removal = new HashSet<>();
        filesInManagement = new HashSet<>();
        updateFiles();
    }

    /** 更新 filesInManagement 获取正在管理的所有文件名列表（当前 HEAD 正在追踪的，和工作区目录下的所有文件名集合）。*/
    public void updateFiles() {
        Commit head = Repository.callCommitManager(commitManagerPath).getHeadCommit();
        Map<String, String> tracking = head.getTrackedFile();
        List<String> workingFiles = Utils.plainFilenamesIn(workingDir);
        filesInManagement = new HashSet<>();
        if (tracking != null) {
            filesInManagement.addAll(tracking.keySet());
        }
        if (workingFiles != null) {
            filesInManagement.addAll(workingFiles);
        }
        filesInManagement.addAll(addition.keySet());
        filesInManagement.addAll(removal);
    }

    /** 序列化保存到 savePath 路径中 */
    public void save() {
        Utils.writeObject(savePath, this);
    }

    /** 获取暂存记录 */
    public Map<String, String> getAddition() {
        return addition;
    }

    /** 获取移除记录 */
    public Set<String> getRemoval() {
        return removal;
    }

    /**
     * 将 workingDir 中的文件添加到 addition 记录中，并在 stagingBlobsDir 中写入文件内容。
     *
     * @param fileName 文件名
     */
    public void addToAddition(String fileName) {
        String content = Utils.readContentsAsString(Utils.join(workingDir, fileName));
        String fileHash = Utils.sha1(fileName, content);
        addition.put(fileName, fileHash);
        Utils.writeContents(Utils.join(stagingBlobsDir, fileHash), content);
    }

    /**
     * 将文件从 addition 记录中移除（无论在不在）。
     *
     * @param fileName 文件名
     */
    public void removeFromAddition(String fileName) {
        addition.remove(fileName);
    }

    /**
     * 将文件添加到 removal 记录中，以准备在下一次 commit 时取消追踪。
     *
     * @param fileName 文件名
     */
    public void addToRemoval(String fileName) {
        removal.add(fileName);
    }

    /**
     * 将文件从 removal 记录中移除（无论在不在）。
     *
     * @param fileName 文件名
     */
    public void removeFromRemoval(String fileName) {
        removal.remove(fileName);
    }

    /**
     * 如果文件在工作目录 workingDir 中，返回 true。
     *
     * @param fileName 文件名
     * @return true or false
     */
    public boolean isInCWD(String fileName) {
        return Utils.join(workingDir, fileName).exists();
    }

    /**
     * 如果文件在 addition 中，返回 true。
     *
     * @param fileName 文件名
     * @return 是否在 addition 中
     */
    public boolean isStagingInAdd(String fileName) {
        return (addition.containsKey(fileName));
    }

    /**
     * 如果文件在 removal 中，返回 true。
     *
     * @param fileName 文件名
     * @return 是否在 removal 中
     */
    public boolean isStagingInRm(String fileName) {
        return removal.contains(fileName);
    }

    /**
     * 判断文件是否已被删除（未经暂存的删除）。
     * 条件包括：
     * - 文件存在于工作区；
     * - 文件被追踪但不被标记删除；
     * - 文件被暂存。
     *
     * @param commit 当前 commit
     * @param fileName 文件名
     * @return 是否已删除
     */
    public boolean hasDeleted(Commit commit, String fileName) {
        if (isInCWD(fileName)) {
            return false;
        } else {
            return (commit.isTracking(fileName) && !isStagingInRm(fileName))
                || (isStagingInAdd(fileName));
        }
    }

    /**
     * 判断文件是否已被修改（未经暂存的变更）。
     * 条件包括：
     * - 文件存在于工作区；
     * - 文件被追踪或暂存；
     * - 内容与追踪版本或暂存版本不一致。
     *
     * @param commit 当前 commit
     * @param fileName 文件名
     * @return 是否已修改
     */
    public boolean hasModified(Commit commit, String fileName) {
        if (!isInCWD(fileName)) {
            return false;
        }
        if (isNotTracking(commit, fileName)) {
            return false;
        }
        String fileHash = Utils.fileHashIn(workingDir, fileName);
        return (!isStagingInAdd(fileName)
                && commit.isTrackingDifferentIn(workingDir, fileName))
            || (isStagingInAdd(fileName) && !addition.get(fileName).equals(fileHash));
    }

    /**
     * 判断文件是否未被追踪：
     * - 存在于工作区；
     * - 既未在 commit 中追踪，也未暂存在 addition 中；
     * - 或者存在于 removal 中。
     *
     * @param commit 当前 commit
     * @param fileName 文件名
     * @return 是否未追踪
     */
    public boolean isNotTracking(Commit commit, String fileName) {
        if (!isInCWD(fileName)) {
            return false;
        }
        return (!commit.isTracking(fileName) && !isStagingInAdd(fileName))
            || (isStagingInRm(fileName));
    }

    /** 清空 stagingBlobsDir 目录下的文件，以及 addition 和 removal 的记录 */
    public void clearStageArea() {
        addition = new HashMap<>();
        removal = new HashSet<>();
        Utils.clean(stagingBlobsDir);
    }

    /**
     * 将工作区中所有被 commit 追踪的文件恢复成追踪的状态。
     *
     * @param commit 当前 commit
     */
    public void checkout(Commit commit) {
        Map<String, String> branchTrackingFiles = commit.getTrackedFile();
        for (String fileName : branchTrackingFiles.keySet()) {
            // 只要工作区的文件与追踪的版本不同，或追踪的文件不在工作区中，都进行 checkout
            if (!commit.isTrackingSameIn(workingDir, fileName)) {
                checkout(commit, fileName);
            }
        }
    }

    /**
     * 在工作区中创建或覆盖 commit 所追踪的文件。
     *
     * @param commit 当前 commit
     * @param fileName 文件名
     */
    public void checkout(Commit commit, String fileName) {
        String fileHash = commit.getTrackedFile().get(fileName);
        String blobContent = Utils.readContentsAsString(Utils.join(blobsDir, fileHash));
        Utils.writeContents(Utils.join(workingDir, fileName), blobContent);
    }

    /**
     * 获取暂存文件的列表（staged files）
     *
     * @return 暂存状态的文件列表（已排序）
     */
    public List<String> getStagedFiles() {
        List<String> stagedFiles = new ArrayList<>();
        for (String fileName : filesInManagement) {
            if (isStagingInAdd(fileName)) {
                stagedFiles.add(fileName);
            }
        }
        Collections.sort(stagedFiles);
        return stagedFiles;
    }

    /**
     * 获取移除文件的列表（removed files）
     *
     * @return 移除状态的文件列表（已排序）
     */
    public List<String> getRemovedFiles() {
        List<String> removedFiles = new ArrayList<>();
        for (String fileName : filesInManagement) {
            if (isStagingInRm(fileName)) {
                removedFiles.add(fileName);
            }
        }
        Collections.sort(removedFiles);
        return removedFiles;
    }

    /**
     * 获取被修改或删除文件的列表（modified files）
     *
     * @param head 当前 HEAD commit
     * @return 修改或删除状态的文件列表（已排序）
     */
    public List<String> getModifiedFiles(Commit head) {
        List<String> modifiedFiles = new ArrayList<>();
        for (String fileName : filesInManagement) {
            if (hasDeleted(head, fileName)) {
                modifiedFiles.add(fileName + " (deleted)");
            } else if (hasModified(head, fileName)) {
                modifiedFiles.add(fileName + " (modified)");
            }
        }
        Collections.sort(modifiedFiles);
        return modifiedFiles;
    }

    /**
     * 获取未被追踪文件的列表（untracked files）
     *
     * @param head 当前 HEAD commit
     * @return 未追踪状态的文件列表（已排序）
     */
    public List<String> getUntrackedFiles(Commit head) {
        List<String> untrackedFiles = new ArrayList<>();
        for (String fileName : filesInManagement) {
            if (isNotTracking(head, fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        Collections.sort(untrackedFiles);
        return untrackedFiles;
    }

    /**
     * 从远程 FileManager 的 blobs 目录中拉取指定 blob 文件，如果本地尚未存在该 blob 则保存。
     *
     * @param remoteFM 远程 FileManager 对象，提供 blob 文件的来源
     * @param blobName blob 文件名（即文件哈希值）
     */
    public void fetchBlobFrom(FileManager remoteFM, String blobName) {
        File oldFile = Utils.join(remoteFM.blobsDir, blobName);
        if (oldFile.exists()) {
            String content = Utils.readContentsAsString(oldFile);
            File newFile = Utils.join(blobsDir, blobName);
            if (!newFile.exists()) {
                Utils.writeContents(newFile, content);
            }
        }
    }
}
