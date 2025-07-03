package gitlet;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
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

class MergeManager {

    /** 当前分支与目标分支的最近公共祖先提交（分裂点） */
    private final Commit splitPoint;

    /** 当前分支的最新提交 */
    private final Commit currentCommit;

    /** 被合并进当前分支的提交（目标分支的最新提交） */
    private final Commit givenCommit;

    /** 当前工作目录中未被当前分支追踪的文件集合 */
    private final Set<String> untrackedFiles;

    /** 合并过程中需要 checkout（还原）到工作区并暂存的文件集合 */
    private final Set<String> checkoutFiles;

    /** 合并过程中需要从版本控制中移除的文件集合 */
    private final Set<String> removeFiles;

    /** 合并过程中发生冲突的文件集合 */
    private final Set<String> conflictFiles;

    /** 当前 Gitlet 仓库的工作目录 */
    private final File workingDir;

    /** 所有 blob 文件的存储目录（用于获取文件内容） */
    private final File blobDir;

    /** 构造 MergeManager 对象 */
    MergeManager(Commit splitPoint,
                        Commit currentCommit,
                        Commit givenCommit,
                        Collection<String> untrackedFiles,
                        File workingDir,
                        File blobDir) {
        this.splitPoint = splitPoint;
        this.currentCommit = currentCommit;
        this.givenCommit = givenCommit;
        this.untrackedFiles = new HashSet<>(untrackedFiles);
        this.checkoutFiles = new HashSet<>();
        this.removeFiles = new HashSet<>();
        this.conflictFiles = new HashSet<>();
        this.workingDir = workingDir;
        this.blobDir = blobDir;
    }

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

    /**
     * 对所有相关文件执行合并操作。
     *
     * @return 若所有文件合并成功（无未追踪冲突）返回 true，否则 false
     */
    boolean merge() {
        Set<String> files = getAllFiles();
        for (String fileName : files) {
            if (!merge(fileName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据指定文件在 split、current 和 given 提交中的状态编码，执行对应的合并操作。
     * 具体逻辑：
     *   对于状态 "112" 或 "001"：如果该文件存在于未追踪文件集合中，则返回 false；否则，将其加入 checkoutFiles 集合。
     *   对于状态 "110"：将该文件加入 removeFiles 集合。
     *   对于状态 "123", "120", "102", "012"：如果该文件存在于未追踪文件集合中，则返回 false；否则，将其加入 conflictFiles 集合。
     *
     * @param fileName 要合并的文件名
     * @return 若合并操作过程中不存在未追踪文件冲突，返回 true；否则返回 false
     */
    private boolean merge(String fileName) {
        String statusCode = statusCode(fileName);
        switch (statusCode) {
            case "112", "001" -> {
                if (untrackedFiles.contains(fileName)) {
                    return false;
                }
                checkoutFiles.add(fileName);
            }
            case "110" -> removeFiles.add(fileName);
            case "123", "120", "102", "012" -> {
                if (untrackedFiles.contains(fileName)) {
                    return false;
                }
                conflictFiles.add(fileName);
            }
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

    /** 计算并返回 fileName 的状态编码 */
    private String statusCode(String fileName) {
        String splitHash = splitPoint.getTrackedFile().get(fileName);
        String currentHash = currentCommit.getTrackedFile().get(fileName);
        String givenHash = givenCommit.getTrackedFile().get(fileName);

        // 计算 f 的高度：若 splitHash 不为 null，则 f 为 1，否则为 0
        int f = (splitHash != null) ? 1 : 0;

        // 计算 s 的高度：
        // 1. 如果 currentHash 不为 null，则初始 s 为 1；
        // 2. 如果 s 与 f 相等且 currentHash 与 splitHash 不相同，则 s 再加 1（此处使用 Objects.equals 做空安全比较）
        int s = 0;
        if (currentHash != null) {
            s ++;
            if (s == f && !Objects.equals(currentHash, splitHash)) {
                s++;
            }
        }

        // 计算 t 的高度：
        // 1. 如果 givenHash 不为 null，则初始 t 为 1；
        // 2. 如果 t 与 f 相等且 givenHash 与 splitHash 不相同，则 t 再加 1；
        // 3. 如果 t 与 s 相等且 givenHash 与 currentHash 不相同，则 t 再加 1。
        int t = 0;
        if (givenHash != null) {
            t ++;
            if (t == f && !Objects.equals(givenHash, splitHash)) {
                t++;
            }
            if (t == s && !Objects.equals(givenHash, currentHash)) {
                t++;
            }
        }

        return String.format("%d%d%d", f, s, t);
    }


    /**
     * 执行 checkout 操作：将需要检出的文件从 givenCommit 还原到工作区，并添加到暂存区。
     */
    void doCheckout() {
        for (String fileName : checkoutFiles) {
            Repository.checkout(new String[]{givenCommit.id(), "--", fileName});
            Repository.addFile(fileName);
        }
    }

    /**
     * 执行 remove 操作：将需要移除的文件从版本库和暂存区中删除。
     */
    void doRemove() {
        for (String fileName : removeFiles) {
            Repository.remove(fileName);
        }
    }

    /**
     * 处理冲突文件：将冲突文件以特定格式写入工作区并添加到暂存区。
     */
    void handleConflict() {
        for (String fileName : conflictFiles) {
            String curHash = currentCommit.getTrackedFile().get(fileName);
            String givHash = givenCommit.getTrackedFile().get(fileName);
            String curContents = null;
            String givContents = null;
            if (curHash != null) {
                curContents = Utils.readContentsAsString(Utils.join(blobDir, curHash));
            }
            if (givHash != null) {
                givContents = Utils.readContentsAsString(Utils.join(blobDir, givHash));
            }

            String mergedContents = "<<<<<<< HEAD\n"
                    + (curContents == null ? "" : curContents)
                    + "=======\n"
                    + (givContents == null ? "" : givContents)
                    + ">>>>>>>\n";
            Utils.writeContents(Utils.join(workingDir, fileName), mergedContents);

            Repository.addFile(fileName);
        }
    }

    /**
     * 判断此次合并过程中是否发生了冲突。
     *
     * @return 若有冲突文件返回 true，否则 false
     */
    boolean encounteredConflict() {
        return !conflictFiles.isEmpty();
    }

}
