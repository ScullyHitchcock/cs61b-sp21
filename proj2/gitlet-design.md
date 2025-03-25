# Gitlet Design Document

**Name**: CST

## Classes and Data Structures

### Class Commit
#### 实例变量
1. String message：提交信息。
2. Instant time：提交时间。
3. ArrayList<String> parentCommits：所有父提交的列表。
4. HashMap<String, String> trackedFile：正在追踪的文件。为Map 结构，其key 为文件名，val 为文件对应的哈希码。
5. String commitId：commit 哈希码。
#### 实例方法
1. public Commit(String message, Instant time, String parentCommit, HashMap<String, String> file)：传入提交信息、提交时间作为元数据，传入父提交节点、需要追踪的文件，得到新的 Commit 对象。
2. public static createInitCommit()：创建并返回初始 Commit 对象。
3. public Commit childCommit(String msg)：传入提交信息 msg，创建并返回指向 this 的新 Commit 对象。
4. public void setParent(String id)：传入指定提交的 id，将其设置为 this 对象的父提交。
5. public String getParentHash()：返回 this 的第一父 commit id。
6. public String getMessage()：返回提交信息。
7. public Instant getTime()：返回提交时间。
8. public String id()：返回 id。
8. public String save()：保存 this 数据，返回 id。
9. public TreeMap<String, String> getTrackedFile()：返回正在追踪的文件数据。
10. public void trackFile(String file, String fileHash)：跟踪文件名 fileName，以及文件哈希码 fileHash。
11. public void untrackFile(String fileName)：取消跟踪文件 fileName。
12. public boolean isTracking(String fileName)：如果 this 正在追踪文件 fileName，返回true，否则false。
13. public boolean isTrackingSame(String fileName)：如果 commit 正在追踪的文件 fileName 没有变化，返回 true。
14. public boolean isTrackingDifferent(String fileName)：如果 commit 正在追踪的文件 fileName 发生变化，返回 true。
15. public Commit merge(Commit otherCommit)：传入另一个 Commit 对象 otherCommit，合并后返回新 Commit 对象。
16. public void dump()：打印 this 的 id、提交信息、提交时间、追踪文件等相关信息。

## Algorithms

## Persistence

