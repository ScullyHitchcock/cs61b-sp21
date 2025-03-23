package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class CWDManager {
    private HashMap<String, String> files;

    private CWDManager() {
        files = updatedFiles();
    }

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

    /* 删除文件，成功删除返回 true，否则 false */
    public boolean deleteFile(String file) {
        if (!files.containsKey(file)) return false;
        return Utils.join(Repository.CWD, file).delete();
    }
}
