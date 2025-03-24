package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeMap;

public class Commit implements Serializable, Dumpable {
    /**
     * @message 提交信息
     * @time 提交时间
     * @parentCommits 指向的父 commit 列表（可能存在多个父 commit ）
     * @trackedFile 追踪的文件（文件名->哈希码）
     * @hashcode 自身的哈希码
     */
    private String message;
    private Instant time;
    private ArrayList<String> parentCommits;
    private TreeMap<String, String> trackedFile;
    private String id;

    // 构造方法，创建新的 Commit 对象
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

    public String createHashcode() {
        return Utils.sha1(
                Utils.serialize(trackedFile),
                Utils.serialize(parentCommits),
                message,
                Utils.serialize(time));
    }

    // 用于创建初始提交
    public static Commit createInitCommit() {
        return new Commit("initial commit", Instant.EPOCH, new ArrayList<>(), new TreeMap<>());
    }

    // 创建子提交对象
    // 1，设置元信息
    // 2，复制其追踪的文件
    // 3，设置当前提交为其父提交（可扩展为合并时添加多个父提交）
    public Commit childCommit(String msg) {
        ArrayList<String> newParents = new ArrayList<>();
        TreeMap<String, String> newTrackedFiles = new TreeMap<>(this.trackedFile);
        Commit child = new Commit(msg, Instant.now(), newParents, newTrackedFiles);
        child.addParent(this.id);
        return child;
    }

    // 添加父提交
    public void addParent(String parentHash) {
        parentCommits.add(parentHash);
    }

    public String getId() {
        return id;
    }

    /* 返回 commit 的第一父 commit */
    public String getParentHash() {
        if (parentCommits.isEmpty()) return null;
        return parentCommits.get(0);
    }

    public String getMessage() {
        return message;
    }

    public Instant getTime() {
        return time;
    }

    public TreeMap<String, String> getTrackedFile() {
        return trackedFile;
    }

    /* 跟踪文件，创建并储存文件的 blob */
    public void trackFile(String file, String blobName) {
        trackedFile.put(file, blobName);
        String content = Utils.readContentsAsString(Utils.join(Repository.CWD, file));
        Utils.createOrOverride(Repository.BLOBS, blobName, content);
    }

    /* 取消跟踪文件 */
    public void untrackFile(String file) {
        trackedFile.remove(file);
    }

    /* 判断 commit 是否正在追踪文件 fileName */
    public boolean isTracking(String fileName) {
        return trackedFile.containsKey(fileName);
    }

    /* 如果 commit 正在追踪的文件 fileName 没有变化，返回 true */
    public boolean isTrackingSame(String fileName) {
        String fileHash = Utils.fileHash(fileName);
        return (fileHash.equals(trackedFile.get(fileName)));
    }

     /* 如果 commit 正在追踪的文件 fileName 发生变化，返回 true */
    public boolean isTrackingDifferent(String fileName) {
        return (isTracking(fileName) && !isTrackingSame(fileName));
    }

    // 将当前 Commit 对象写入文件
    public String save() {
        // 重新计算 id 以确保内容正确
        id = createHashcode();
        File f = Utils.join(Repository.COMMITS, id);
        Utils.createFile(f);
        Utils.writeObject(f, this);
        return id;
    }

    /* 未完成 */
    public Commit merge(Commit otherCommit) {
        return null;
    }

    @Override
    public void dump() {
        Utils.message("=== Commit Dump ===");
        Utils.message("Hashcode: %s", id);
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