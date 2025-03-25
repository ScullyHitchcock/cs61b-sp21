package gitlet;

import java.io.Serializable;
import java.util.*;

public class FileManager implements Serializable {
    /* staging for addition 区域文件 map，以“文件名 -> 文件哈希码”形式储存 */
    private Map<String, String> addition;

    /* staging for removal 区域文件 list，以“文件名”形式储存 */
    private Set<String> removal;

    public FileManager() {
        addition = new HashMap<>();
        removal = new HashSet<>();
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

    /* 如果文件 fileName 在 CWD 中，返回 false
     *  否则如果
     *   1 fileName 被 commit 追踪，且不在 removal 中，返回 true
     *   2 fileName 在 addition 中，返回 true */
    public boolean hasDeleted(Commit commit, String fileName) {
        if (Utils.hasFile(Repository.CWD, fileName)) return false;
        else {
            return (isStagingInAdd(fileName))
                || (commit.isTracking(fileName) && !isStagingInRm(fileName));
        }
    }

    /*  1 如果文件 fileName 不在 addition 中，且被 commit 追踪，且内容与追踪内容不同，返回 true
     *  2 如果文件 fileName 在 addition 中，且内容与 addition 内容不同，返回 true */
    public boolean hasModified(Commit commit, String fileName) {
        return (!isStagingInAdd(fileName) && commit.isTrackingDifferent(fileName))
            || (isStagingInAdd(fileName) && !addition.get(fileName).equals(Utils.fileHash(fileName)));
    }

    /* 1 如果 fileName 不被 commit 追踪，且不在 addition 中，返回 true
    *  2 如果 fileName 在 removal 中，且在 CWD 中，返回true */
    public boolean isNotTracking(Commit commit, String fileName) {
        return (!commit.isTracking(fileName) && !isStagingInAdd(fileName))
            || ((isStagingInRm(fileName) && Utils.hasFile(Repository.CWD, fileName)));
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
}
