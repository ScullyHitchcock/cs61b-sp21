package gitlet;

import java.io.Serializable;
import java.util.*;

public class FileManager implements Serializable {
    /* staging for addition 区域文件 map，以“文件名 -> 文件哈希码”形式储存 */
    private Map<String, String> addition;

    /* staging for removal 区域文件 list，以“文件名”形式储存 */
    private Set<String> removal;

    /* 管理区中的所有文件集合，包括工作区的文件和head正在追踪的文件的并集 */
    private Set<String> filesInManagement;

    public FileManager() {
        addition = new HashMap<>();
        removal = new HashSet<>();
        filesInManagement = new HashSet<>();
        updateFiles();
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
    public Set<String> getRemoval() {
        return removal;
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
        filesInManagement.addAll(removal);
    }

    /* 将文件 fileName 暂存至 addition（创建或覆盖），同时在 STAGING_BLOBS 创建相应文件（创建或覆盖） */
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

    /* 将文件 fileName 暂存至 removal（创建或覆盖） */
    public void addToRemoval(String fileName) {
        removal.add(fileName);
    }
    /* 将文件 fileName 从 removal 中移除（无论在不在） */
    public void removeFromRemoval(String fileName) {
        removal.remove(fileName);
    }

    /* 如果文件 fileName 在 addition 中，返回 true */
    public boolean isStagingInAdd(String fileName) {
        return (addition.containsKey(fileName));
    }

    /* 如果文件 fileName 在 removal 中，返回 true */
    public boolean isStagingInRm(String fileName) {
        return removal.contains(fileName);
    }

    /* 在不通过 gitlet 程序被移出工作区的文件，属于 hasDeleted 状态：
     * 如果文件 fileName 在 CWD 中，返回 false
     * 否则如果：
     *   1 fileName 被 commit 追踪，且不在 removal 中，返回 true
     *   2 fileName 在 addition 中，返回 true */
    public boolean hasDeleted(Commit commit, String fileName) {
        if (Utils.hasFile(Repository.CWD, fileName)) return false;
        else {
            return (commit.isTracking(fileName) && !isStagingInRm(fileName))
                || (isStagingInAdd(fileName));
        }
    }

    /* 在不通过 gitlet 程序被修改内容的工作区文件，属于 hasModified 状态：
     * 1 如果文件 fileName 不在 addition 中，且被 commit 追踪，且内容与追踪内容不同，返回 true
     * 2 如果文件 fileName 在 addition 中，且内容与 addition 内容不同，返回 true */
    public boolean hasModified(Commit commit, String fileName) {
        if (!Utils.hasFile(Repository.CWD, fileName)) return false;
        if (isNotTracking(commit, fileName)) return false;
        return (!isStagingInAdd(fileName) && commit.isTrackingDifferent(fileName))
            || (isStagingInAdd(fileName) && !addition.get(fileName).equals(Utils.fileHash(fileName)));
    }

    /* 工作区中没有被 gitlet 追踪或暂存的文件，或存在于 removal 中又在工作区出现的文件，属于 isNotTracking：
     * 如果 fileName 在 CWD 中，且
     * 1 fileName 不被 commit 追踪，且不在 addition 中，返回 true
     * 2 fileName 在 removal 中，返回 true */
    public boolean isNotTracking(Commit commit, String fileName) {
        if (!Utils.hasFile(Repository.CWD, fileName)) return false;
        return (!commit.isTracking(fileName) && !isStagingInAdd(fileName))
            || (isStagingInRm(fileName));
    }

    /* 清空 staging 区域 */
    public void clearStageArea() {
        addition = new HashMap<>();
        removal = new HashSet<>();
        Utils.clean(Repository.STAGING_BLOBS);
    }

    /* 把 fileName 的在工作区的内容恢复为 commit 记录的状态 */
    static void checkout(Commit commit, String fileName) {
        String fileHash = commit.getTrackedFile().get(fileName);
        String blobContent = Utils.readContentsAsString(Utils.join(Repository.BLOBS, fileHash));
        Utils.createOrOverride(Repository.CWD, fileName, blobContent);
    }

    /* 将工作区中所有被 commit 追踪的文件恢复成追踪的状态 */
    static void checkout(Commit commit) {
        Map<String, String> branchTrackingFiles = commit.getTrackedFile();
        for (String fileName: branchTrackingFiles.keySet()) {
            String fileHash = commit.getTrackedFile().get(fileName);
            String blobContent = Utils.readContentsAsString(Utils.join(Repository.BLOBS, fileHash));
            Utils.createOrOverride(Repository.CWD, fileName, blobContent);
        }
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
