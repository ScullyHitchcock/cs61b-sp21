package gitlet;

import java.io.Serializable;
import java.util.*;

public class CommitNode implements Serializable {
    private String commitId;
    private List<String> parentIds;
    private List<String> childrenIds; // 记录所有直接子节点的 commit id

    public CommitNode(String commitId, List<String> parentIds) {
        this.commitId = commitId;
        // 为了安全起见，复制一份列表
        this.parentIds = new ArrayList<>(parentIds);
        this.childrenIds = new ArrayList<>();
    }

    public String getCommitId() {
        return commitId;
    }

    public List<String> getParentIds() {
        return Collections.unmodifiableList(parentIds);
    }

    public List<String> getChildrenIds() {
        return Collections.unmodifiableList(childrenIds);
    }

    public void addChild(String childId) {
        childrenIds.add(childId);
    }
}
