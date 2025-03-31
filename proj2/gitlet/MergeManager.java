package gitlet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 负责将指定分支的提交（Commit）合并到当前分支。
 *
 * 合并步骤概述：
 * 1. 找出两个分支的分裂点（split point）：
 *    a. 若 split point 与目标分支相同，则合并无须执行，提示并返回。
 *    b. 若 split point 与当前分支相同，执行 fast-forward 合并。
 * 2. 针对三个 commit（split, current, given）所追踪的每个文件进行合并分析：
 *    a. only given 修改：checkout → add
 *    b. only current 修改：不处理
 *    c. 修改一致或未改动：不处理
 *    d. both added same file：不处理
 *    e. only given 新增：checkout → add
 *    f. only given 删除：remove
 *    g. only current 删除：不处理
 *    h. 修改冲突情况：标记为冲突文件，后续处理
 * 3. 若发生冲突，生成冲突格式内容写入文件，并暂存该文件。
 * 4. 若存在未追踪文件将被覆盖，则提示错误并终止合并。
 * 5. 合并完成后，生成一条带有两个父提交的新 commit。
 * 6. 若处理了冲突，额外打印提示信息。
 */

public class MergeManager {
    private Commit splitPoint;
    private Commit currentCommit;
    private Commit givenCommit;
    private Set<String> untrackedFiles;
    private Set<String> checkoutFiles;
    private Set<String> removeFiles;
    private Set<String> conflictFiles;

    /**
     * 构造一个 MergeManager 实例，初始化提交对象和文件集合。
     *
     * @param splitPoint 分裂点提交
     * @param currentCommit 当前分支的提交
     * @param givenCommit 被合并分支的提交
     * @param untrackedFiles 当前工作区中的未追踪文件集合
     */
    public MergeManager(Commit splitPoint, Commit currentCommit, Commit givenCommit, Collection<String> untrackedFiles) {
        this.splitPoint = splitPoint;
        this.currentCommit = currentCommit;
        this.givenCommit = givenCommit;
        this.untrackedFiles = new HashSet<>(untrackedFiles);
        this.checkoutFiles = new HashSet<>();
        this.removeFiles = new HashSet<>();
        this.conflictFiles = new HashSet<>();
    }

    /* 获取三个 Commit 对象正在追踪的所有文件集合 */
    /**
     * 获取 split、current、given 三个提交中所有被追踪的文件集合。
     *
     * @return 文件名的集合
     */
    private Set<String> getAllFiles() {
        HashSet<String> allFiles = new HashSet<>();
        Set<String> splitPointTracked = splitPoint.getTrackedFile().keySet();
        Set<String> currentCommitTracked = currentCommit.getTrackedFile().keySet();
        Set<String> givenCommitTracked = givenCommit.getTrackedFile().keySet();
        allFiles.addAll(splitPointTracked);
        allFiles.addAll(currentCommitTracked);
        allFiles.addAll(givenCommitTracked);
        return allFiles;
    }

    /* 对三个 commit 对象追踪的所有文件进行 merge 操作，全部文件操作成功返回 true，否则 false */
    /**
     * 对所有相关文件执行合并操作。
     *
     * @return 若所有文件合并成功（无未追踪冲突）返回 true，否则 false
     */
    public boolean merge() {
        Set<String> files = getAllFiles();
        for (String fileName : files) {
            if (!merge(fileName)) {
                return false;
            }
        }
        return true;
    }

    /* 对单个文件进行 merge，如果出现未追踪文件即将被覆盖，返回 false，否则 true */
    /**
     * 对单个文件执行合并判断与操作。
     *
     * @param fileName 文件名
     * @return 若文件合并无冲突返回 true，否则 false
     */
    private boolean merge(String fileName) {
        if (con(1, 1, 2, fileName) || con(0 ,0 ,1, fileName)) {
            if (untrackedFiles.contains(fileName)) {
                return false;
            }
            checkoutFiles.add(fileName);
        } else if (con(1, 1, 0, fileName)) {
            removeFiles.add(fileName);
        } else if (
                con(1, 2, 3, fileName) || con(1, 2, 0, fileName)
                || con(1, 0, 2, fileName) || con(0, 1, 2, fileName)) {
            if (untrackedFiles.contains(fileName)) {
                return false;
            }
            conflictFiles.add(fileName);
        }
        return true;
    }

    /*
     表格：非冲突情况下的 merge 行为（编码：split-current-given）
     | 编码     | 情况描述                                   | 操作                                 |
     |---------|-------------------------------------------|-------------------------------------|
     | 1-1-2   | only given 修改                            | 检查untrackedFile -> checkout → add |
     | 1-2-1   | only current 修改                          | 不做处理                             |
     | 1-1-1   | 双方都未改                                  | 不做处理                             |
     | 1-2-2   | 双方都改但一致                               | 不做处理                             |
     | 1-0-0   | 双方都删了                                  | 不做处理                             |
     | 0-1-1   | split 没有，双方都新增相同文件                | 不做处理                             |
     | 0-1-0   | only current 新增                          | 不做处理                             |
     | 0-0-1   | only given 新增                            | 检查untrackedFile -> checkout → add |
     | 1-1-0   | only given 删除                            | remove                              |
     | 1-0-1   | only current 删除                          | 不做处理                             |

     表格：产生冲突的 merge 情况（编码：split-current-given）
     | 编码     | 情况描述                                   | 操作                                 |
     |---------|-------------------------------------------|-------------------------------------|
     | 1-2-3   | 双方都修改但结果不同                          | 产生冲突 → 写入冲突格式内容             |
     | 1-2-0   | current 修改，given 删除                    | 产生冲突                             |
     | 1-0-2   | current 删除，given 修改                    | 产生冲突                             |
     | 0-1-2   | split 没有，双方都新增内容不同的同名文件        | 产生冲突                             |
     */

    /**
     * 判断文件是否符合表格中的条件
     * @param f 对应表格的第一个数字，0 代表 splitPoint 不追踪文件，1 代表正在追踪
     * @param s 对应表格的第二个数字，0 代表 currentCommit 不追踪，1 代表正在追踪，2 代表追踪且相对 splitPoint 有改动
     * @param t 对应表格的第三个数字，0 代表 givenCommit 不追踪，1 代表追踪，2 代表追踪且相对 splitPoint 有改动，
     *          3 代表追踪且相对 splitPoint 和 currentCommit 都有改动
     * @param fileName 文件名
     * @return true or false
     */
    private boolean con(int f, int s, int t, String fileName) {
        String splitHash = splitPoint.getTrackedFile().get(fileName);
        String currentHash = currentCommit.getTrackedFile().get(fileName);
        String givenHash = givenCommit.getTrackedFile().get(fileName);

        int actualF = (splitHash == null) ? 0 : 1;
        int actualS;
        int actualT;

//        // currentCommit 状态判断
//        if (!currentCommit.isTracking(fileName)) {
//            actualS = 0;
//        } else if (actualF == 0) {
//            actualS = isTrackingSame(currentCommit, givenCommit, fileName) ? 1 : 2;
//        } else if (currentHash.equals(splitHash)) {
//            actualS = 1;
//        } else {
//            actualS = 2;
//        }

        // currentCommit 状态判断
        if (currentHash == null) { // 如果 cur 不追踪 fileName 直接为 0
            actualS = 0;
        } else if (actualF == 0) { // 如果 spl 不追踪而 cur 追踪则为 1
            actualS = 1;
        } else { // 如果 spl 追踪，cur 也追踪
            if (currentHash.equals(splitHash)) { // 如果追踪相同，则为 1
                actualS = 1;
            } else { // 如果追踪不同，则为 2
                actualS = 2;
            }
        }

        // givenCommit 状态判断
        if (givenHash == null) { // 如果 giv 不追踪 fileName 直接为 0
            actualT = 0;
        } else if ((actualF == 0) && (actualS == 0)) { // 如果 giv 追踪，spl 和 cur 都不追踪，则为 1
            actualT = 1;
        } else { // 如果 spl 和 cur 至少有一个在追踪（01，10，11，12）
            if (givenHash.equals(splitHash)) { // 如果 giv 与 spl 追踪相同，则与 actualF 相同
                actualT = actualF;
            } else if (givenHash.equals(currentHash)) { // 如果 giv 与 cur 追踪相同，则与 actualS 相同
                actualT = actualS;
            } else { // 如果 giv 追踪的既和 spl 不同，也和 cur 不同，则一定为 2
                actualT = 2;
            }
        }

//        // givenCommit 状态判断
//        if (!givenCommit.isTracking(fileName)) {
//            actualT = 0;
//        } else if (actualF == 0) {
//            actualT = isTrackingSame(currentCommit, givenCommit, fileName) ? 1 : 2;
//        } else if (givenHash.equals(splitHash)) {
//            actualT = 1;
//        } else {
//            actualT = 2;
//        }

        if (t == 3) {
            // 检查是否是双方都改了但内容不同：1-2-2
            if (actualF == 1 && actualS == 2 && actualT == 2) {
                return !currentHash.equals(givenHash);
            } else {
                return false;
            }
        } else {
            return (actualF == f && actualS == s && actualT == t);
        }
    }

    /* 判断两个 Commit 对象是否追踪了相同的文件 fileName */
    /**
     * 判断两个提交是否对某个文件追踪的版本一致。
     *
     * @param c1 第一个提交
     * @param c2 第二个提交
     * @param fileName 文件名
     * @return 是否追踪相同内容
     */
//    private boolean isTrackingSame(Commit c1, Commit c2, String fileName) {
//        if (!c1.isTracking(fileName) || !c2.isTracking(fileName)) return false;
//        String hash1 = c1.getTrackedFile().get(fileName);
//        String hash2 = c2.getTrackedFile().get(fileName);
//        return hash1.equals(hash2);
//    }

    /**
     * 执行 checkout 操作：将需要检出的文件从 givenCommit 还原到工作区，并添加到暂存区。
     */
    public void doCheckout() {
        // 调用 checkoutFiles
        // ...
        for (String fileName : checkoutFiles) {
            Repository.checkout(new String[]{givenCommit.id(), "--", fileName});
            Repository.addFile(fileName);
        }
    }

    /**
     * 执行 remove 操作：将需要移除的文件从版本库和暂存区中删除。
     */
    public void doRemove() {
        // 调用 removeFiles
        // ...
        for (String fileName : removeFiles) {
            Repository.remove(fileName);
        }
    }

    /**
     * 处理冲突文件：将冲突文件以特定格式写入工作区并添加到暂存区。
     */
    public void handleConflict() {
        // 调用 conflictFiles
        // ...
        for (String fileName : conflictFiles) {
            String curHash = currentCommit.getTrackedFile().get(fileName);
            String givHash = givenCommit.getTrackedFile().get(fileName);
            String curContents = (curHash == null) ? null : Utils.readContentsAsString(Utils.join(Repository.BLOBS, curHash));
            String givContents = (givHash == null) ? null : Utils.readContentsAsString(Utils.join(Repository.BLOBS, givHash));

            String mergedContents = "<<<<<<< HEAD\n"
                    + (curContents == null ? "" : curContents)
                    + "=======\n"
                    + (givContents == null ? "" : givContents)
                    + ">>>>>>>\n";
            Utils.writeContents(Utils.join(Repository.CWD, fileName), mergedContents);

            Repository.addFile(fileName);
        }
    }

    /**
     * 判断此次合并过程中是否发生了冲突。
     *
     * @return 若有冲突文件返回 true，否则 false
     */
    public boolean encounteredConflict() {
        return !conflictFiles.isEmpty();
    }

}
