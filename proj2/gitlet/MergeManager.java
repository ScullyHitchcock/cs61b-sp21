package gitlet;

import java.util.List;

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
 *    c 如果给定分支与当前分支的特定文件的修改方式完全一致（fileHash 相同），或以不再追踪，则什么都不用做。
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
    private List<String> untrackedFile;

    /* 寻找两个分支的共同祖先 Commit */
    public void locateSplitPoint(String branch1, String branch2) {

    }

    private boolean merge() {
        return false;
    }
}
