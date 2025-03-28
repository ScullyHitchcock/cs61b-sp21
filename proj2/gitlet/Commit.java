package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Commit implements Serializable, Dumpable {
    /**
     * @message 提交信息
     * @time 提交时间
     * @parentCommits 指向的父 commit 列表（可能存在多个父 commit ）
     * @trackedFile 追踪的文件（文件名->哈希码）
     * @id 提交id
     */
    private String message;
    private Instant time;
    private ArrayList<String> parentCommits;
    private TreeMap<String, String> trackedFile;
    private String commitId;

    public Commit(String message,
                  Instant time,
                  ArrayList<String> parentCommits,
                  TreeMap<String, String> trackedFile) {
        this.message = message;
        this.time = time;
        // 如果父提交列表为 null，则初始化为空列表
        this.parentCommits = (parentCommits != null) ? parentCommits : new ArrayList<>();
        this.trackedFile = (trackedFile != null) ? trackedFile : new TreeMap<>();
    }

    /* 创建初始提交对象 */
    public static Commit createInitCommit() {
        return new Commit("initial commit", Instant.EPOCH, new ArrayList<>(), new TreeMap<>());
    }

    /* 传入提交信息 msg，返回指向 this 的新 commit 对象 */
    public Commit childCommit(String msg) {
        ArrayList<String> newParents = new ArrayList<>();
        TreeMap<String, String> newTrackedFiles = new TreeMap<>(this.trackedFile);
        Commit child = new Commit(msg, Instant.now(), newParents, newTrackedFiles);
        child.addParent(this.commitId);
        return child;
    }

    /* 传入指定的提交 id，将其视为 this 对象的父提交 */
    public void addParent(String id) {
        parentCommits.add(id);
    }

    /* 返回提交 id */
    public String id() {
        return commitId;
    }

    /* 返回所有父 commit 的 id 列表 */
    public List<String> getParentIds() {
        return parentCommits;
    }

    /* 返回提交信息 */
    public String getMessage() {
        return message;
    }

    /* 返回提交时间 */
    public Instant getTime() {
        return time;
    }

    /* 返回正在追踪的文件数据 */
    public TreeMap<String, String> getTrackedFile() {
        return trackedFile;
    }

    /* 解析 staging 区域的 addition 记录和 removal 记录，更新追踪文件状态 */
    public void updateTrackingFiles(Map<String, String> addition, Map<String, String> removal) {
        for (Map.Entry<String, String> entry: addition.entrySet()) {
            String fileName = entry.getKey();
            String fileHash = entry.getValue();
            trackFile(fileName, fileHash);
        }
        for (String fileTobeRemoved: removal.keySet()) {
            untrackFile(fileTobeRemoved);
        }
    }

    /* 跟踪文件名 fileName，以及文件哈希值 fileHash，储存内容快照 blob */
    private void trackFile(String file, String fileHash) {
        trackedFile.put(file, fileHash);
        permanentSaveBlob(fileHash);
    }
    /* 将 STAGING_BLOBS 文件夹中的文件快照复制到 BLOBS 文件夹中 */
    private void permanentSaveBlob(String fileHash) {
        File oldFile = Utils.join(Repository.STAGING_BLOBS, fileHash);
        String content = Utils.readContentsAsString(oldFile);
        File newFile = Utils.join(Repository.BLOBS, fileHash);
        if (!newFile.exists()) {
            Utils.writeContents(newFile, content);
        }
    }
    /* 取消跟踪文件 fileName */
    private void untrackFile(String fileName) {
        trackedFile.remove(fileName);
    }

    /* 如果 this 正在追踪文件 fileName，返回true，否则 false */
    public boolean isTracking(String fileName) {
        return trackedFile.containsKey(fileName);
    }

    /* 如果 commit 正在追踪的文件 fileName 没有变化，返回 true */
    public boolean isTrackingSame(String fileName) {
        String fileHash = Utils.fileHashInCWD(fileName);
        if (isTracking(fileName) && fileHash != null) {
            return (fileHash.equals(trackedFile.get(fileName)));
        }
        return false;
    }

     /* 如果 commit 正在追踪的文件 fileName 发生变化，返回 true */
    public boolean isTrackingDifferent(String fileName) {
        return (isTracking(fileName) && !isTrackingSame(fileName));
    }

    /* 保存 this 数据 */
    public String save() {
        commitId = createId();
        File f = Utils.join(Repository.COMMITS, commitId);
        Utils.writeObject(f, this);
        return commitId;
    }
    private String createId() {
        return Utils.sha1(
                Utils.serialize(trackedFile),
                Utils.serialize(parentCommits),
                message,
                Utils.serialize(time));
    }

    /* 传入另一个 Commit 对象 otherCommit，合并后返回新 Commit 对象 */
    public Commit mergeCommit(Commit otherCommit) {
        return null;
    }

    @Override
    /* 打印 this 相关信息 */
    public void dump() {
        Utils.message("=== Commit Dump ===");
        Utils.message("Hashcode: %s", commitId);
        Utils.message("Message: %s", message);
        Utils.message("Time: %s", time);
        Utils.message("Parent Commits:");
        for (String parent : parentCommits) {
            Utils.message("  - %s", parent);
        }
        Utils.message("Tracked Files:");
        for (String file : trackedFile.keySet()) {
            Utils.message("  %s -> %s", file, trackedFile.get(file));
        }
    }
}