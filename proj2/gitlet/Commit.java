package gitlet;

// TODO: any imports you need here
// TODO: 在这里导入所需的任何包

import java.io.File;
import java.time.Instant;
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
public class Commit {
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
    private String author;
    private Commit parentCommit;
    private File file;

    /** The message of this Commit. */
    /** 这个提交的提交信息。 */



    /* TODO: fill in the rest of this class. */
    /* TODO: 补全这个类的其余部分。 */

    public Commit(String message,
                  Instant time,
                  String author) {
        this.message = message;
        this.author = author;
        this.time = time;
    }

    // 创建 initial commit 对象 iniCOmm：
    //  1 contains no files
    //  2 has the commit message: initial commit
    //  3 It will have a single branch: master,
    //    which initially points to this initial commit,
    //    and master will be the current branch.
    //  4 timestamp: 00:00:00 UTC, Thursday, 1 January 1970
    public static Commit initializeCommit() {
        // 创建initcommit
        // 初始提交msg
        String msg = "initial commit";
        // 初始时间：1970-01-01T00:00:00Z
        Instant initTime = Instant.EPOCH;
        // 初始作者
        String author = "User";
        return new Commit(msg, initTime, author);
    }
}
