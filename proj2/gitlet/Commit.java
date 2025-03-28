package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 表示一次提交快照，记录文件状态和提交元信息。
 * 包括提交信息、时间、父提交、所追踪文件以及提交 ID。
 * 支持生成子提交、更新追踪文件、判断文件状态变化等功能。
 */
public class Commit implements Serializable {
    /** 提交信息 */
    private final String message;
    /** 提交时间 */
    private final Instant time;
    /** 父提交 ID 列表 */
    private final ArrayList<String> parentCommits;
    /** 当前提交所追踪的文件映射 */
    private final TreeMap<String, String> trackedFile;
    /** 提交 ID */
    private String commitId;

    /**
     * 构造一个新的 Commit 对象。
     *
     * @param message 提交信息
     * @param time 提交时间
     * @param parentCommits 父提交 ID 列表
     * @param trackedFile 当前提交所追踪的文件映射
     */
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

    /** 创建初始提交对象 */
    public static Commit createInitCommit() {
        return new Commit("initial commit", Instant.EPOCH, new ArrayList<>(), new TreeMap<>());
    }

    /**
     * 传入提交信息 msg，返回指向当前提交的新的 Commit 对象。
     *
     * @param msg 提交信息
     * @return 新的 Commit 对象
     */
    public Commit childCommit(String msg) {
        ArrayList<String> newParents = new ArrayList<>();
        TreeMap<String, String> newTrackedFiles = new TreeMap<>(this.trackedFile);
        Commit child = new Commit(msg, Instant.now(), newParents, newTrackedFiles);
        child.addParent(this.commitId);
        return child;
    }

    /**
     * 传入指定的提交 ID，将其视为当前提交的父提交。
     *
     * @param id 父提交 ID
     */
    public void addParent(String id) {
        parentCommits.add(id);
    }

    /** 返回当前提交的 ID。 */
    public String id() {
        return commitId;
    }

    /** 返回所有父提交的 ID 列表。 */
    public List<String> getParentIds() {
        return parentCommits;
    }

    /** 返回提交信息。 */
    public String getMessage() {
        return message;
    }

    /** 返回提交时间。 */
    public Instant getTime() {
        return time;
    }

    /** 返回当前提交所追踪的文件数据。 */
    public TreeMap<String, String> getTrackedFile() {
        return trackedFile;
    }

    /**
     * 解析暂存区域的添加记录和移除记录，更新追踪文件状态。
     *
     * @param addition 添加记录
     * @param removal 移除记录
     */
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

    /**
     * 跟踪文件名 fileName，以及文件哈希值 fileHash，储存内容快照 blob。
     *
     * @param file 文件名
     * @param fileHash 文件哈希值
     */
    private void trackFile(String file, String fileHash) {
        trackedFile.put(file, fileHash);
        permanentSaveBlob(fileHash);
    }

    /** 将 STAGING_BLOBS 文件夹中的文件快照复制到 BLOBS 文件夹中。 */
    private void permanentSaveBlob(String fileHash) {
        File oldFile = Utils.join(Repository.STAGING_BLOBS, fileHash);
        String content = Utils.readContentsAsString(oldFile);
        File newFile = Utils.join(Repository.BLOBS, fileHash);
        if (!newFile.exists()) {
            Utils.writeContents(newFile, content);
        }
    }

    /** 取消跟踪文件 fileName。 */
    private void untrackFile(String fileName) {
        trackedFile.remove(fileName);
    }

    /**
     * 如果当前提交正在追踪文件 fileName，返回 true，否则返回 false。
     *
     * @param fileName 文件名
     * @return 是否正在追踪
     */
    public boolean isTracking(String fileName) {
        return trackedFile.containsKey(fileName);
    }

    /**
     * 如果当前提交正在追踪的文件 fileName 没有变化，返回 true。
     *
     * @param fileName 文件名
     * @return 是否没有变化
     */
    public boolean isTrackingSame(String fileName) {
        String fileHash = Utils.fileHashInCWD(fileName);
        if (isTracking(fileName) && fileHash != null) {
            return (fileHash.equals(trackedFile.get(fileName)));
        }
        return false;
    }

    /**
     * 如果当前提交正在追踪的文件 fileName 发生变化，返回 true。
     *
     * @param fileName 文件名
     * @return 是否发生变化
     */
    public boolean isTrackingDifferent(String fileName) {
        return (isTracking(fileName) && !isTrackingSame(fileName));
    }

    /** 保存当前提交的数据。 */
    public String save() {
        commitId = createId();
        File f = Utils.join(Repository.COMMITS, commitId);
        Utils.writeObject(f, this);
        return commitId;
    }

    /** 创建提交 ID。 */
    private String createId() {
        return Utils.sha1(
                Utils.serialize(trackedFile),
                Utils.serialize(parentCommits),
                message,
                Utils.serialize(time));
    }
}