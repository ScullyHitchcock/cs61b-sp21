package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class RemoteRepo implements Serializable {

    /** 远程仓库名 */
    public String name;
    /** 远程仓库的 GITLET 目录 */
    public File GITLET;
    /** GITLET 目录下的 BLOBS 目录，用来储存 blob 文件 */
    public File BLOBS;
    /** GITLET 目录下的 COMMITS 目录，用来储存 commit 文件 */
    public File COMMITS;
    /** GITLET 目录下的 MANAGER 文件，用来储存 RemoManager 对象 */
    public File MANAGER;

    private class RemoManager extends CommitManager {
        private HashMap<String, String> commits;
        private String headBranchName;
        private HashMap<String, String> branches;

        public RemoManager() {
            commits = new HashMap<>();
            headBranchName = null;
            branches = new HashMap<>();
        }

        @Override
        public void save() {
            Utils.writeObject(MANAGER, this);
        }

        @Override
        public Commit getCommit(String id) {
            String matchId = null;
            if (commits.containsKey(id)) {
                matchId = id;
            } else {
                int matchCount = 0;
                for (String commitId : commits.keySet()) {
                    if (commitId.startsWith(id)) {
                        matchCount += 1;
                        matchId = commitId;
                    }
                }
                if (matchCount != 1) {
                    return null;
                }
            }
            File commitFile = Utils.join(COMMITS, matchId);
            if (!commitFile.exists()) {
                return null;
            }
            return Utils.readObject(commitFile, Commit.class);
        }

        @Override
        public Commit findSplitPoint(CommitManager cm, String commitId1, String commitId2) {
            // 先获得 commitId1 的所有祖先
            Set<String> ancestors = cm.getAllAncestors(commitId1);
            // 从 commitId2 向上遍历查找第一个出现在 ancestors 中的 commit
            Queue<String> queue = new LinkedList<>();
            queue.add(commitId2);
            Set<String> visited = new HashSet<>();
            while (!queue.isEmpty()) {
                String currentId = queue.poll();
                Commit currentCommit = getCommit(currentId);
                if (ancestors.contains(currentId)) {
                    return currentCommit;
                }
                visited.add(currentId);
                List<String> parentIds = currentCommit.getParentIds();
                if (parentIds != null && !parentIds.isEmpty()) {
                    for (String parentId : parentIds) {
                        if (!visited.contains(parentId)) {
                            queue.add(parentId);
                        }
                    }
                }
            }
            return null;  // 理论上不可能没有公共祖先
        }

    }
    public RemoteRepo(String name, String dir, CommitManager cm) {
        // 注册远程仓库 GITLET 目录地址
        // 注册 GITLET 下 BLOBS 文件夹地址
        // 注册 GITLET 下 COMMITS 文件夹地址
        // 注册 GITLET 下 MANAGER 文件地址
        this.name = name;
        String convertedPath = dir.replace("/", File.separator);
        GITLET = Utils.join(convertedPath, ".gitlet");
        BLOBS = Utils.join(GITLET, "blobs");
        COMMITS = Utils.join(GITLET, "commits");
        MANAGER = Utils.join(GITLET, "manager");
    }

    public void save() {
        Utils.writeObject(Utils.join(Repository.GITLET_DIR, this.name), this);
    }

    public static void delete(String remoteName) {

    }
}
