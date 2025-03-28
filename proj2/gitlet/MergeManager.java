package gitlet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** 将指定分支的 Commit 合并到当前活跃分支上
 *  1 找出两个分支的分裂点 commit。
 *    a 如果分裂点与目标分支 branchName 指向的 commit 相同，
 *      什么都不用做，打印"Given branch is an ancestor of the current branch."直接退出。
 *    b 如果分裂点与当前分支 headBranchName 指向的 commit 相同，操作 checkout(branchName)，打印"Current branch fast-forwarded."。
 *  2 如果分裂点不是上述两种情况，对于每一个文件：
 *    a 如果某个文件 fileName 在“给定分支” givenBranch（被 merge 进来的分支）的内容和分裂点的版本不同，
 *      但在“当前分支” currentBranch（执行 merge 的分支）中的内容与分裂点一致，
 *      则 checkout(givenBranch.commit, fileName) -> addFile(fileName)。
 *    b 如果反过来，currentBranch 与分裂点不同，givenBranch 与分裂点相同，则什么都不用做。
 *    c 如果给定分支与当前分支的特定文件的修改方式完全一致（fileHash 相同），或已不再追踪，则什么都不用做。
 *    d 如果给定分支与当前分支两者都追踪了分裂点没有的新文件，则什么都不用做。
 *    e 如果给定分支出现了分裂点没有的新文件，而当前分支没有出现，则 checkout(givenBranch.commit, fileName) -> addFile(fileName)。
 *    f 如果给定分支删除了分裂点有的文件，而当前分支没有删除，则 remove(fileName)。
 *    g 如果当前分支删除了分裂点有的文件，而给定分支没有删除，则什么都不用做。
 *    h 如果文件在当前分支和给定分支都被改动了，但改法不同，则产生冲突：
 *      a 两边都修改了，但结果不同；
 *      b 一边改了内容，一边删掉了；
 *      c 文件原来 split point 没有，但两边新增了不同版本。
 *  3 处理冲突，将文件的内容覆盖为以下格式：
 *    <<<<<<< HEAD
 *    contents of file in current branch
 *    =======
 *    contents of file in given branch
 *    >>>>>>>
 *  4 如果工作区中存在未追踪文件，且该文件会被 merge 后的同名文件覆盖，则报错 "There is an untracked file in the way; delete it, or add and commit it first."
 *  4 当处理完合并后，自动生成一个合并 commit，msg 为 "Merged [given branch name] into [current branch name]."，parents 是两个 commit。
 *  5 如果处理了冲突，则输出一句 "Encountered a merge conflict."。
 *
 * */

public class MergeManager {
    private Commit splitPoint;
    private Commit currentCommit;
    private Commit givenCommit;
    private Set<String> untrackedFiles;
    private Set<String> checkoutFiles;
    private Set<String> removeFiles;
    private Set<String> conflictFiles;

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

        int actualF = splitPoint.isTracking(fileName) ? 1 : 0;
        int actualS;
        int actualT;

        // currentCommit 状态判断
        if (!currentCommit.isTracking(fileName)) {
            actualS = 0;
        } else if (actualF == 0) {
            actualS = isTrackingSame(currentCommit, givenCommit, fileName) ? 1 : 2;
        } else if (currentHash.equals(splitHash)) {
            actualS = 1;
        } else {
            actualS = 2;
        }

        // givenCommit 状态判断
        if (!givenCommit.isTracking(fileName)) {
            actualT = 0;
        } else if (actualF == 0) {
            actualT = isTrackingSame(currentCommit, givenCommit, fileName) ? 1 : 2;
        } else if (givenHash.equals(splitHash)) {
            actualT = 1;
        } else {
            actualT = 2;
        }

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
    private boolean isTrackingSame(Commit c1, Commit c2, String fileName) {
        if (!c1.isTracking(fileName) || !c2.isTracking(fileName)) return false;
        String hash1 = c1.getTrackedFile().get(fileName);
        String hash2 = c2.getTrackedFile().get(fileName);
        return hash1.equals(hash2);
    }

    public void doCheckout() {
        // 调用 checkoutFiles
        // ...
        for (String fileName : checkoutFiles) {
            Repository.checkout(new String[]{givenCommit.id(), "--", fileName});
            Repository.addFile(fileName);
        }
    }

    public void doRemove() {
        // 调用 removeFiles
        // ...
        for (String fileName : removeFiles) {
            Repository.remove(fileName);
        }
    }

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

    public boolean encounteredConflict() {
        return !conflictFiles.isEmpty();
    }

}
