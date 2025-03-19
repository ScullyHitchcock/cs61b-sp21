package gitlet;

// TODO: any imports you need here
// TODO: 在这里导入所需的任何包

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
    private String author;
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
                  String author,
                  String parentCommit,
                  HashMap<String, String> file) {
        this.message = message;
        this.author = author;
        this.time = time;
        this.parentCommit = parentCommit;
        this.trackedFile = file;
        this.hashcode = Utils.sha1(message, author, time);
    }

    /* 创建 init commit */
    public Commit() {
        this.message = "initial commit";
        this.time = Instant.EPOCH;
        this.author = Repository.USER_NAME;
        this.trackedFile = new HashMap<String, String>();
        this.hashcode = Utils.sha1(message, author, time);
    }

    /* 写入 Commit 对象，返回哈希码文件名 */
    public String saveCommit(File folder) {
        File f = Utils.join(folder, hashcode);
        try {
            f.createNewFile();
        } catch (IOException e) {
            System.out.println("创建文件时发生错误：" + e.getMessage());
        }
        Utils.writeObject(f, this);
        return hashcode;
    }

    public Commit cloneCommit() {
        return new Commit(message, time, author, parentCommit, trackedFile);
    }

    public Commit childCommit(String msg, Instant time, String parent, String file, String blob) {
        Commit child = new Commit(msg, time, author, parent, trackedFile);
        child.trackedFile.put(file, blob);
        return child;
    }

    public void setToBeHead() {
        if (!Repository.HEAD.exists()) {
            try {
                Repository.HEAD.createNewFile();
            } catch (IOException e) {
                System.out.println("创建文件时发生错误：" + e.getMessage());
            }
        }
        Utils.writeObject(Repository.HEAD, this);
    }
}
