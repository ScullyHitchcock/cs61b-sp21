package gitlet;

// TODO: any imports you need here
// TODO: 在这里导入所需的任何包

import jh61b.junit.In;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
// TODO: You'll likely use this in this class
// TODO: 你可能会在这个类中使用它

/** Represents a gitlet commit object.
 *  表示一个 Gitlet 提交对象。
 *
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *  TODO: 在这里描述这个类的高层次功能，这样做是个不错的主意。
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     * TODO: 在这里添加实例变量。
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     * 在这里列出 Commit 类的所有实例变量，并在它们上方添加有用的注释，
     * 说明这些变量表示什么，以及它们的用途。我们已经为 `message` 提供了一个示例。
     */
    private String message;
    private Instant time;
    private String parentCommit;
    private HashMap<String, String> trackedFile;
    private String hashcode;
    /** The message of this Commit. */
    /** 这个提交的提交信息。 */



    /* TODO: fill in the rest of this class. */
    /* TODO: 补全这个类的其余部分。 */

    public Commit(String message,
                  Instant time,
                  String parentCommit,
                  HashMap<String, String> file) {
        this.message = message;
        this.time = time;
        this.parentCommit = parentCommit;
        this.trackedFile = file;
    }

    /* 创建 init commit */
    public Commit() {
        this.message = "initial commit";
        this.time = Instant.EPOCH;
        this.trackedFile = new HashMap<String, String>();
        this.parentCommit = "empty";
    }

    /* 写入 Commit 对象，返回哈希码文件名 */
    public String save(File folder) {
        hashcode = Utils.sha1(Utils.serialize(trackedFile), parentCommit, message, Utils.serialize(time));
        File f = Utils.join(folder, hashcode);
        Utils.createFile(f);
        Utils.writeObject(f, this);
        return hashcode;
    }

    /**
     * 创建子 commit
     * @param msg commit 信息
     * @param time 当前时间
     * @param parent 上一个 commit 的hashcode
     * @return child commit
     */
    public Commit childCommit(String msg, Instant time, String parent) {
        return new Commit(msg, time, parent, new HashMap<>(trackedFile));
    }

    public void setToBeHead() {
        if (!Repository.HEAD.exists()) {
            Utils.createFile(Repository.HEAD);
        }
        Utils.writeObject(Repository.HEAD, this);
    }

    public String getHashcode() {
        return hashcode;
    }

    public Instant getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }


    public String getParentCommit() {
        return parentCommit;
    }

    public void trackFile(String file, String blobName) {
        trackedFile.put(file, blobName);
    }

    public void untrackFile(String file) {
        trackedFile.remove(file);
    }

    public HashMap<String, String> getTrack() {
        return trackedFile;
    }
}
