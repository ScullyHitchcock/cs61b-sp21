package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CWDManager {
    private HashMap<String, String> files;
    private List<String> stagingFiles;
    private List<String> staingBlobs;
    private List<String> removals;

    public CWDManager() {
        files = updatedFiles();
        stagingFiles = Utils.plainFilenamesIn(Repository.ADDITION);
        staingBlobs = Utils.plainFilenamesIn(Repository.STAGING_BLOBS);
        removals = Utils.plainFilenamesIn(Repository.REMOVAL);
    }

    public HashMap<String, String> getFiles() {
        return files;
    }

    /* 更新 CWD 的文件情况 */
    public HashMap<String, String> updatedFiles() {
        List<String> files = Utils.plainFilenamesIn(Repository.CWD);
        HashMap<String, String> res = new HashMap<>();
        for (String file: files) {
            String content = Utils.readContentsAsString(Utils.join(Repository.CWD, file));
            String hash = Utils.sha1(file, content);
            res.put(file, hash);
        }
        return res;
    }

    /* 修改当前文件，如果没有则创建写入，成功返回 true，否则 false */
    public boolean modify(String fileName, String newContent) {
        File file = Utils.join(Repository.CWD, fileName);
        if (files.containsKey(fileName)) {
            String newHash = Utils.sha1(fileName, newContent);
            if (files.get(fileName).equals(newHash)) return false;
        } else {
            Utils.createFile(file);
        }
        Utils.writeContents(file, newContent);
        return true;
    }

    /* 打开文件返回内容，如果文件不存在则返回 null */
    public String openFile(String file) {
        if (!files.containsKey(file)) return null;
        return Utils.readContentsAsString(Utils.join(Repository.CWD, file));
    }

    public String fileHash(String file) {
        String content = openFile(file);
        if (content == null) return null;
        return Utils.sha1(file, content);
    }

    /* 删除文件，成功删除返回 true，否则 false */
    public boolean deleteFile(String file) {
        if (!files.containsKey(file)) return false;
        return Utils.join(Repository.CWD, file).delete();
    }

    /* 判断当前文件是否处于未暂存状态
    * 1，当文件内容和暂存区不同
    * 2，且和最新的commit追踪的内容也不同
    * 那么它就是未暂存的 */
    public boolean isUnstaged(String file, Commit headCommit) {
        String hash = fileHash(file);
        Map<String, String> tracked = headCommit.getTrackedFile();
        boolean isTracking = (hash.equals(tracked.get(file)));
        boolean isStagingForAddition = (stagingFiles.contains(file) && staingBlobs.contains(hash));
        return (!isTracking && !isStagingForAddition);
    }

    public HashMap<String, String> unStagingFiles() {
        HashMap<String, String> res = new HashMap<>();
        CommitManager manager = Repository.callManager();
        Commit headCommit = manager.getHeadCommit();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String file = entry.getKey();
            String hash = entry.getValue();
            if (isUnstaged(file, headCommit)) {
                res.put(file, hash);
            }
        }
        return res;
    }
}
