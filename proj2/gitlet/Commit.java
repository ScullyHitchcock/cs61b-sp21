package gitlet;

import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

public class Commit implements Serializable {
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
    private HashMap<String, String> trackedFile;
    private String hashcode;

    // 构造方法，创建新的 Commit 对象
    public Commit(String message,
                  Instant time,
                  ArrayList<String> parentCommits,
                  HashMap<String, String> trackedFile) {
        this.message = message;
        this.time = time;
        // 如果父提交列表为 null，则初始化为空列表
        this.parentCommits = (parentCommits != null) ? parentCommits : new ArrayList<>();
        this.trackedFile = (trackedFile != null) ? trackedFile : new HashMap<>();
        // 计算哈希码时考虑父提交列表、文件内容、信息和时间
        this.hashcode = createHashcode();
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
        return new Commit("initial commit", Instant.EPOCH, new ArrayList<>(), new HashMap<>());
    }

    // 创建子提交对象
    // 1，设置元信息
    // 2，复制其追踪的文件
    // 3，设置当前提交为其父提交（可扩展为合并时添加多个父提交）
    public Commit childCommit(String msg, Instant time) {
        ArrayList<String> newParents = new ArrayList<>();
        HashMap<String, String> newTrackedFiles = new HashMap<>(this.trackedFile);
        Commit child = new Commit(msg, time, newParents, newTrackedFiles);
        child.addParent(this.hashcode);
        return child;
    }

    // 添加父提交
    public void addParent(String parentHash) {
        parentCommits.add(parentHash);
    }

    public String getHashcode() {
        return hashcode;
    }

    public String getParentCommits() {
        if (parentCommits.isEmpty()) return null;
        return parentCommits.get(0);
    }

    public String getMessage() {
        return message;
    }

    public Instant getTime() {
        return time;
    }

    public HashMap<String, String> getTrackedFile() {
        return trackedFile;
    }

    public void trackFile(String file, String blobName) {
        trackedFile.put(file, blobName);
    }

    public void untrackFile(String file) {
        trackedFile.remove(file);
    }

    public boolean isTracking(String fileName) {
        return trackedFile.containsKey(fileName);
    }

    // 将当前 Commit 对象写入文件
    public String save() {
        // 重新计算 hashcode 以确保内容正确
        hashcode = createHashcode();
        File f = Utils.join(Repository.COMMITS, hashcode);
        Utils.createFile(f);
        Utils.writeObject(f, this);
        return hashcode;
    }

    public Commit merge(Commit otherCommit) {
        return null;
    }
}