package gitlet;

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

    /* addition 区，以“文件名 -> 文件哈希值”的形式储存特定文件 */
    private Map<String, String> addition;

    /* removal 区，以“文件名 -> 文件哈希值”的形式储存特定文件 */
    private Map<String, String> removal;

    /* 管理区中的所有文件集合，包括工作区的文件和head正在追踪的文件的并集 */
    private Set<String> filesInManagement;

    public FileManager() {
        addition = new HashMap<>();
        removal = new HashMap<>();
        filesInManagement = new HashSet<>();
        updateFiles();
    }

    /**
     * 更新 filesInManagement 获取正在管理的所有文件名列表（当前 HEAD 正在追踪的，和工作区目录下的所有文件名集合）。
     */
    public void updateFiles() {
        Commit head = Repository.callCommitManager().getHeadCommit();
        Map<String, String> tracking = head.getTrackedFile();
        List<String> workingFiles = Utils.plainFilenamesIn(Repository.CWD);
        filesInManagement = new HashSet<>();
        if (tracking != null) {
            filesInManagement.addAll(tracking.keySet());
        }
        if (workingFiles != null) {
            filesInManagement.addAll(workingFiles);
        }
        filesInManagement.addAll(addition.keySet());
        filesInManagement.addAll(removal.keySet());
    }

    /**
     * 保存文件管理器。
     */
    public void save() {
        Utils.writeObject(Repository.FILE_MANAGER, this);
    }

    /**
     * 获取 addition 变量。
     *
     * @return addition 区的文件映射
     */
    public Map<String, String> getAddition() {
        return addition;
    }

    /**
     * 获取 removal 变量。
     *
     * @return removal 区的文件映射
     */
    public Map<String, String> getRemoval() {
        return removal;
    }

    /**
     * 将文件添加到 addition 区，并在 STAGING_BLOBS 中写入文件内容。
     *
     * @param fileName 文件名
     */
    public void addToAddition(String fileName) {
        String content = Utils.readContentsAsString(Utils.join(Repository.CWD, fileName));
        String fileHash = Utils.sha1(fileName, content);
        addition.put(fileName, fileHash);
        Utils.writeContents(Utils.join(Repository.STAGING_BLOBS, fileHash), content);
    }

    /**
     * 将文件从 addition 中移除（无论在不在）。
     *
     * @param fileName 文件名
     */
    public void removeFromAddition(String fileName) {
        addition.remove(fileName);
    }

    /**
     * 将文件暂存至 removal，并在 STAGING_BLOBS 中写入文件内容。
     *
     * @param fileName 文件名
     */
    public void addToRemoval(String fileName) {
        String content = Utils.readContentsAsString(Utils.join(Repository.CWD, fileName));
        String fileHash = Utils.sha1(fileName, content);
        removal.put(fileName, fileHash);
        Utils.writeContents(Utils.join(Repository.STAGING_BLOBS, fileHash), content);
    }

    /**
     * 将文件从 removal 中移除（无论在不在）。
     *
     * @param fileName 文件名
     */
    public void removeFromRemoval(String fileName) {
        removal.remove(fileName);
    }

    /**
     * 如果文件在工作目录中，返回 true。
     *
     * @param fileName 文件名
     * @return 是否存在于工作目录
     */
    public boolean isInCWD(String fileName) {
        return Utils.join(Repository.CWD, fileName).exists();
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
        return removal.containsKey(fileName);
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
        if (isInCWD(fileName)) return false;
        else {
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
        if (!isInCWD(fileName)) return false;
        if (isNotTracking(commit, fileName)) return false;
        return (!isStagingInAdd(fileName) && commit.isTrackingDifferent(fileName))
            || (isStagingInAdd(fileName) && !addition.get(fileName).equals(Utils.fileHashInCWD(fileName)));
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
        if (!isInCWD(fileName)) return false;
        return (!commit.isTracking(fileName) && !isStagingInAdd(fileName))
            || (isStagingInRm(fileName));
    }

    /**
     * 清空 staging 区域。
     */
    public void clearStageArea() {
        addition = new HashMap<>();
        removal = new HashMap<>();
        Utils.clean(Repository.STAGING_BLOBS);
    }

    /**
     * 将工作区中所有被 commit 追踪的文件恢复成追踪的状态。
     *
     * @param commit 当前 commit
     */
    static void checkout(Commit commit) {
        Map<String, String> branchTrackingFiles = commit.getTrackedFile();
        for (String fileName : branchTrackingFiles.keySet()) {
            // 只要工作区的文件与追踪的版本不同，或追踪的文件不在工作区中，都进行 checkout
            if (!commit.isTrackingSame(fileName)) {
                checkout(commit, fileName);
            }
        }
    }

    /**
     * 在工作区中创建或覆盖 commit 所追踪的内容。
     *
     * @param commit 当前 commit
     * @param fileName 文件名
     */
    static void checkout(Commit commit, String fileName) {
        String fileHash = commit.getTrackedFile().get(fileName);
        String blobContent = Utils.readContentsAsString(Utils.join(Repository.BLOBS, fileHash));
        Utils.writeContents(Utils.join(Repository.CWD, fileName), blobContent);
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
}
