package gitlet;

public class CommitTree {
    // Commit节点
    public Commit commit;
    // 分支1
    public CommitTree branch1;
    // 分支2
    public CommitTree branch2;

    /**
     * 创建初始树：创建根Commit
     */
    public CommitTree() {
        commit = Commit.initializeCommit();
    }
}
