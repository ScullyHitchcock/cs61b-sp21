# Gitlet Design Document

**Name**: CST

## Classes and Data Structures

### Class CommitTree
#### 实例变量
1. Commit commit：节点，由 Commit 对象组成。
2. CommitTree branch1：分支1。
3. CommitTree branch2：分支2。
#### 实例方法
1. 构造方法CommitTree()：创建初始Commit节点，调用Commit.initializeCommit()。

### Class Commit
#### 实例变量
1. String message：提交信息。
2. Instant time：提交时间。
3. String author：提交作者。
4. Commit parentCommit：指向上一个提交的指针。
5. File file：该提交所包含的文件。
#### 实例方法
1. 构造方法Commit(String message, Instant time, String author)：提供提交信息、提交时间、提交作者作为元数据，创建新Commit对象。其parentCommit和file变量默认为null。
2. 创建初始Commit对象方法initializeCommit()：创建新Commit对象，初始化其元数据内容：提交信息为："initial commit"、提交时间为：Instant.EPOCH（Unix初始时间）、提交作者："User"。


## Algorithms

## Persistence

