package gitlet;

import java.io.File;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static void setup() {
        // 新建 .gitlet 文件夹
        GITLET_DIR.mkdir();
        // 创建 initial commit 对象 iniCOmm：
        //  1 contains no files
        //  2 has the commit message: initial commit
        //  3 It will have a single branch: master,
        //    which initially points to this initial commit,
        //    and master will be the current branch.
        //  4 timestamp: 00:00:00 UTC, Thursday, 1 January 1970
    }

    /* TODO: fill in the rest of this class. */
}
