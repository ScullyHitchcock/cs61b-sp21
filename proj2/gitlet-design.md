# Gitlet Design Document

**Name**: CST

## Classes and Data Structures

### Class Commit
#### 实例变量
1. String message：提交信息。
2. Instant time：提交时间。
3. String parentCommit：指向上一个提交文件的哈希名称。
4. HashMap<String, String> trackedFile：正在追踪的文件。map结构，key为文件名，val为文件对应的哈希名称。
5. String Hashcode：自身的哈希名。
#### 实例方法
1. 构造方法Commit(String message, Instant time, String parentCommit, HashMap<String, String> file)：传入提交信息、提交时间作为元数据，传入父提交节点、需要追踪的。
2. 创建初始Commit对象方法initializeCommit()：创建新Commit对象，初始化其元数据内容：提交信息为："initial commit"、提交时间为：Instant.EPOCH（Unix初始时间）、提交作者："User"。


## Algorithms

## Persistence

