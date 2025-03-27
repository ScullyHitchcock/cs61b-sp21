package gitlet;

import java.io.Serializable;
import java.util.*;

/** FileManager 管理以下三大区域的所有文件：
 *  工作目录（CWD）、暂存区（分为 addition 和 removal 两个小区域）、特定 commit（通常是 HEAD）。
 *  暂存区中的 addition 区域储存为下一次 commit 所要添加追踪的文件。
 *  暂存区中的 removal 区域储存为下一次 commit 所要移除追踪的文件。
 *
 *  FileManager 提供以下方法：
 *  1 在特定区域（commit 追踪区域除外）添加或修改特定文件，写入特定内容；
 *  2 在特定区域（commit 追踪区域除外）移除特定文件；
 *  3 查询特定文件是否存在于特定区域中；
 *  4 查询特定文件的是否处于某种工作状态；
 *  5 返回符合特定工作状态的所有文件；
 *  6 恢复工作区的所有文件为特定提交时刻的状态。*/
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

    /* 更新 filesInManagement 获取正在管理的所有文件名列表（当前 HEAD 正在追踪的，和工作区目录下的所有文件名集合） */
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

    /* 保存文件管理器 */
    public void save() {
        if (!Repository.FILE_MANAGER.exists()) {
            Utils.createFile(Repository.FILE_MANAGER);
        }
        Utils.writeObject(Repository.FILE_MANAGER, this);
    }

    /* 获取 addition 变量 */
    public Map<String, String> getAddition() {
        return addition;
    }

    /* 获取 removal 变量 */
    public Map<String, String> getRemoval() {
        return removal;
    }

    /* 将文件 fileName 添加至 addition（创建或覆盖），同时在 STAGING_BLOBS 创建相应文件（创建或覆盖） */
    public void addToAddition(String fileName) {
        String content = Utils.readContentsAsString(Utils.join(Repository.CWD, fileName));
        String fileHash = Utils.sha1(fileName, content);
        addition.put(fileName, fileHash);
        Utils.createOrOverride(Repository.STAGING_BLOBS, fileHash, content);
    }

    /* 将文件 fileName 从 addition 中移除（无论在不在）*/
    public void removeFromAddition(String fileName) {
        addition.remove(fileName);
    }

    /* 将文件 fileName 暂存至 removal（创建或覆盖），同时在 STAGING_BLOBS 创建相应文件（创建或覆盖） */
    public void addToRemoval(String fileName) {
        String content = Utils.readContentsAsString(Utils.join(Repository.CWD, fileName));
        String fileHash = Utils.sha1(fileName, content);
        removal.put(fileName, fileHash);
        Utils.createOrOverride(Repository.STAGING_BLOBS, fileHash, content);
    }
    /* 将文件 fileName 从 removal 中移除（无论在不在） */
    public void removeFromRemoval(String fileName) {
        removal.remove(fileName);
    }

    /* 如果文件 fileName 在工作目录中，返回true */
    public boolean isInCWD(String fileName) {
        return Utils.join(Repository.CWD, fileName).exists();
    }

    /* 如果文件 fileName 在 addition 中，返回 true */
    public boolean isStagingInAdd(String fileName) {
        return (addition.containsKey(fileName));
    }

    /* 如果文件 fileName 在 removal 中，返回 true */
    public boolean isStagingInRm(String fileName) {
        return removal.containsKey(fileName);
    }

    /* 在不通过 gitlet 程序被移出工作区的文件，属于 hasDeleted 状态：
     * 如果文件 fileName 在 CWD 中，返回 false
     * 否则如果：
     *   1 fileName 被 commit 追踪，且不在 removal 中，返回 true
     *   2 fileName 在 addition 中，返回 true */
    public boolean hasDeleted(Commit commit, String fileName) {
        if (isInCWD(fileName)) return false;
        else {
            return (commit.isTracking(fileName) && !isStagingInRm(fileName))
                || (isStagingInAdd(fileName));
        }
    }

    /* 曾经被 gitlet 跟踪过，修改内容后又不通知 gitlet 的工作区文件，属于 hasModified 状态：
     * 1 如果文件不在工作目录中，返回 false
     * 1 如果文件是新建的或未曾被 gitlet 跟踪过的，返回 false
     * 1 如果文件 fileName 不在 addition 中，但被 commit 追踪，且内容与追踪内容不同，返回 true
     * 2 如果文件 fileName 在 addition 中，且内容与 addition 内容不同，返回 true */
    public boolean hasModified(Commit commit, String fileName) {
        if (!isInCWD(fileName)) return false;
        if (isNotTracking(commit, fileName)) return false;
        return (!isStagingInAdd(fileName) && commit.isTrackingDifferent(fileName))
            || (isStagingInAdd(fileName) && !addition.get(fileName).equals(Utils.fileHashInCWD(fileName)));
    }

    /* 工作区中没有被 gitlet 追踪或暂存的文件，或存在于 removal 中又在工作区出现的文件，属于 isNotTracking：
     * 如果 fileName 在 CWD 中，且
     * 1 fileName 不被 commit 追踪，且不在 addition 中，返回 true
     * 2 fileName 在 removal 中，返回 true */
    public boolean isNotTracking(Commit commit, String fileName) {
        if (!isInCWD(fileName)) return false;
        return (!commit.isTracking(fileName) && !isStagingInAdd(fileName))
            || (isStagingInRm(fileName));
    }

    /* 清空 staging 区域 */
    public void clearStageArea() {
        addition = new HashMap<>();
        removal = new HashMap<>();
        Utils.clean(Repository.STAGING_BLOBS);
    }

    /* 将工作区中所有被 commit 追踪的文件恢复成追踪的状态 */
    static void checkout(Commit commit) {
        Map<String, String> branchTrackingFiles = commit.getTrackedFile();
        for (String fileName: branchTrackingFiles.keySet()) {
            // 只要工作区的文件与追踪的版本不同，或追踪的文件不在工作区中，都进行 checkout
            if (!commit.isTrackingSame(fileName)) {
                checkout(commit, fileName);
            }
        }
    }

    /* 在工作区中创建或覆盖 commit 所追踪的内容 */
    static void checkout(Commit commit, String fileName) {
        String fileHash = commit.getTrackedFile().get(fileName);
        String blobContent = Utils.readContentsAsString(Utils.join(Repository.BLOBS, fileHash));
        Utils.createOrOverride(Repository.CWD, fileName, blobContent);
    }

    /**
     * 获取暂存文件的列表（staged files）
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
