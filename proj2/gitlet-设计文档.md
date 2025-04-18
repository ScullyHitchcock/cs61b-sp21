# Main

## 设计思路

本类设计遵循 Unix 风格的命令派发器模式：主方法 `main()` 接收命令行参数 `args`，通过第一个参数确定命令类型，并根据命令长度验证参数个数是否正确。所有 `Gitlet` 命令（如 `init`、`add`、`commit` 等）都通过 `switch-case` 显式分派给 `Repository` 类处理逻辑，从而使主入口与具体命令逻辑解耦，保持结构清晰。

为提高健壮性，`Main` 在运行前对命令格式进行检查（`validateArgs()`），并捕获所有运行过程中抛出的 `GitletException` 异常，向用户提供友好的错误提示。

## 实现逻辑

- 命令解析与调度：
  - 程序启动后先检查是否提供了命令（`args.length == 0`）；
  - 使用 `Java 12` 的增强 `switch-case` 语法，按首个参数调用对应方法；
  - 除 `init` 外所有命令都需在 `.gitlet` 仓库目录中运行，否则抛出异常。

- 参数校验 `validateArgs()`：
  - 检查命令参数个数是否在给定范围内（`min <= args.length <= max`）；
  - `checkout` 命令要求特殊格式（带有 `"--"` 分隔符）；
  - 自动调用 `validatePath()`，确认是否在已初始化的 `Gitlet` 仓库中。

- 命令支持列表：
  - 本地命令：`init`、`add`、`commit`、`rm`、`log`、`global-log`、`find`、`status`、`checkout`、`branch`、`rm-branch`、`reset`、`merge`
  - 远程命令：`add-remote`、`rm-remote`、`push`、`fetch`、`pull`
  - 所有命令最终都由 `Repository` 类实现。

- 错误处理：
  - 所有异常通过 `GitletException` 封装，在控制台输出友好的提示后退出程序。

## 主要方法

- `main(String[] args)`：程序主入口，命令分发器。
- `validateArgs(String[] args, int min, int max)`：参数数量与语法验证。
- `validatePath()`：验证当前目录是否为 `.gitlet` 仓库根目录。



# Repository

## 设计思路

`Repository` 类设计为一个高度模块化的控制层接口类，所有命令操作（如 `add`、`commit`、`checkout`、`merge` 等）都通过静态方法实现，调用封装好的内部管理器（`CommitManager`、`FileManager`、`MergeManager`）完成具体功能。此设计使得：
- 每个 `Gitlet` 命令操作拥有单独方法，结构清晰；
- 所有持久化状态（提交、文件、暂存区）都通过 `readObject` 和 `save` 方法在磁盘中加载与更新；
- 所有异常通过统一抛出 `GitletException`，简化错误处理逻辑。

## 实现逻辑

- 初始化命令 (setup)
  - 创建 `.gitlet/` 目录及子目录（`commits`、`blobs`、`staging`）；
  - 初始化 `CommitManager` 和 `FileManager`，并各自进行序列化保存。

- 版本操作
  - `addFile()`：将文件加入暂存区，如果文件内容未变化则忽略。
  - `remove()`：将文件标记为删除，并从工作目录中删除。
  - `commit()`：根据暂存区内容创建一个新的提交对象，更新分支指针。
  - `log()` / `globalLog()` / `find()`：遍历提交历史、按消息查找提交。

- 分支操作
  - `branch()`：新建分支。
  - `rmBranch()`：删除分支（不能删除当前分支）。
  - `checkout()`：支持文件恢复、提交还原、分支切换等三种模式；
  - `reset()`：强制切换 `HEAD` 到指定提交，并更新工作目录和暂存区。

- 合并操作
  - `merge()`：调用 `MergeManager` 实现三方合并：
  - 查找 `split point`；
  - 判断 `fast-forward`、祖先关系；
  - 检查未追踪文件冲突；
  - 根据状态编码执行文件操作（`checkout`/`remove`/`conflict`）；
  - 最后创建带两个父提交的合并提交。

- 状态展示
  - `status()`：展示当前状态（分支、暂存区、未追踪文件等）；
  - 调用 `FileManager` 和 `CommitManager` 获取所有相关状态信息。

- 远程仓库
  - `addRemote()` / `rmRemote()`：管理远程仓库地址。
  - `push()`：向远程仓库分支推送提交，需满足“远程为当前提交祖先”要求。
  - `fetch()`：拉取远程分支提交及 `blobs`，生成 `remote/xxx` 分支。
  - `pull()`：`fetch` + `merge` 的组合命令。

## 关键模块协作关系

                     用户命令  
                        ↓  
                   Repository
       ┌────────────┬──────────────┬─────────────┐  
       ↓            ↓              ↓             ↓  
     FileManager  CommitManager  MergeManager   Utils
    （暂存管理）    （提交图管理）    （合并逻辑）   （工具类）  

## 核心字段（目录定义）

- `CWD`：工作目录路径（当前项目根路径）
- `GITLET_DIR`：`.gitlet` 目录
- `STAGING_BLOBS`：暂存快照目录
- `BLOBS`：`blob` 文件永久存储目录
- `COMMITS`：提交对象保存目录
- `COMMIT_MANAGER` / `FILE_MANAGER`：两大核心状态管理器的序列化保存路径

## 总结亮点

- 封装清晰、接口统一：通过静态方法分别处理每个命令。
- 状态一致性保障：每次调用都重新读取最新状态对象，确保多命令之间的正确性。
- 与底层模块解耦：控制逻辑完全由 `Repository` 调度，底层模块专注于状态维护和算法逻辑。



# Commit

## 设计思路

该类借鉴了 `Git` 中的不可变提交对象的思想，设计为不可变类（核心字段为 `final`），每一个提交保存一次独立的文件快照状态，并通过内容生成唯一的提交 `ID`（`commitId`），确保版本历史的完整性与可追溯性。通过 `parentCommits` 字段支持单一提交与合并提交（两个父节点），使提交记录构成有向无环图（`DAG`）。

## 实现逻辑

- 构造提交对象：构造函数接收提交信息、时间戳、父提交列表和文件追踪映射，支持初始提交和普通提交。
- 创建提交 `ID`：`createId()` 方法将 `tracked` 文件映射、父提交、信息和时间序列化后计算 `SHA1` 值，确保提交唯一性。
- 文件追踪管理：
  - `trackedFile` 为 `TreeMap`，记录文件名 → `blob` 哈希值。
  - `updateTrackingFiles()` 方法接收暂存区的添加与删除操作，更新追踪文件。
  - 添加文件时通过 `trackFile()` 同时将其从 `staging` 复制至 `blobs` 目录（永久保存）。
- 状态判断接口：
  - `isTracking()`：判断当前提交是否追踪指定文件。
  - `isTrackingSameIn()`：判断追踪文件与工作目录中文件是否一致。
  - `isTrackingDifferentIn()`：判断追踪文件内容是否发生改变。
- 子提交创建：`childCommit()` 用于在当前提交基础上创建新提交，复制追踪状态并添加当前为父提交。
- 持久化存储：`save()` 方法将提交对象序列化写入磁盘。

## 主要字段

- `message`：本次提交的说明信息
- `time`：提交的时间戳（类型为 `Instant`）
- `parentCommits`：父提交的 `ID` 列表，支持合并时记录两个父提交
- `trackedFile`：当前提交追踪的文件名与 `blob` 哈希值的映射表
- `commitId`：通过序列化提交内容生成的 `SHA1` 哈希，作为唯一标识符



# CommitManager

## 设计思路

该类充当版本控制的“状态记录器”，维护所有的提交 `ID`、分支名及其对应的最新提交，同时跟踪当前 `HEAD` 所在的分支。它封装了提交的保存与读取、分支切换、查找分裂点（用于合并）等逻辑，确保每次版本演进都可被准确记录和回溯。其状态会被序列化存入 `.gitlet/commitManager` 文件，实现持久化。

此外，`CommitManager` 还支持管理远程仓库的信息，用于分布式操作（如 `fetch/push`）。

## 实现逻辑

- 初始化管理器：
  - 构造函数创建初始提交（`initCommit`），建立主分支 `master`，并将其设为 `HEAD`。
  - 初始化 `commits`（`commitId` → `message`）、`branches`（分支名 → `commitId`）和 `remoteRepos`（远程名 → 路径）等结构。
  - 初始化 `commitTries`，作为基于 Trie 的前缀索引结构，用于加速模糊 ID 匹配操作。

- 提交与持久化：
  - `addCommit()` 方法会添加提交记录并持久化保存到 `commitDir`，同时更新当前 `HEAD` 指向的分支。

- 分支管理：
  - 提供 `createNewBranch()` 创建新分支、`removeBranch()` 删除分支、`changeHeadTo()` 切换分支、`setHeadCommit()` 修改 `HEAD` 所指提交。

- 提交查找与读取：
  - `getCommit()` 支持根据完整 `commit ID` 或其前缀模糊查找对应的 `Commit` 对象。
  - `getBranchCommit()` 获取某分支下最新的提交对象。
  - `findByMessage()` 支持按提交信息搜索提交历史。

- 分裂点查找（用于合并）：
  - `findSplitPoint()` 使用广度优先搜索查找两个提交的最近公共祖先（支持跨仓库）。
  - `getAllAncestors()` 返回给定提交的所有祖先（含自身）。

- 远程仓库支持：
  - 使用 `addRemoteRepo()` 和 `rmRemoteRepo()` 管理远程仓库路径，保存在 `remoteRepos` 字典中。

## 主要字段

- `savePath`：该对象的持久化路径（通常是 `.gitlet/commitManager`）
- `commitDir`：所有提交对象的存储目录
- `commits`：提交 `ID` 到信息的映射（用于日志、查找等）
- `commitTries`：TrieSet 结构，存储所有提交 ID，用于支持前缀模糊匹配操作。
- `branches`：分支名 → 最新提交 `ID` 的映射
- `headBranchName`：当前活跃分支名
- `remoteRepos`：远程仓库名 → 远程路径的映射



# TrieSet

## 设计思路

`TrieSet` 是一个自定义的基于前缀树（`Trie`）的集合类，专门用于高效地支持字符串集合的“模糊前缀匹配”。相较于 `HashMap` 的线性遍历方式，`Trie` 能在 O(k) 时间内完成前缀查找（k 为前缀长度），在 Gitlet 项目中被用于 commit id 的模糊匹配。该类实现中，每个 `Trie` 节点表示一个字符，路径从根节点出发，形成一个字符串。叶子节点的 `isEnd` 用于标记完整字符串。

## 实现逻辑

- `add(word)`：将字符串按字符依次插入 Trie，若路径中不存在某字符，则新建 TrieNode。
- `contains(word)`：逐字符查找是否存在该完整字符串（并以 isEnd 为终止判断）。
- `startsWith(prefix)`：查找所有以指定前缀开头的字符串，借助 DFS 深度遍历。
- `dfs(prefix, node, result)`：递归辅助方法，用于从某个节点开始收集所有后缀路径。

## 主要字段
- `TrieNode root`：Trie 树的根节点，所有插入路径从此开始。
  - 每个 TrieNode：
    - `Map<Character, TrieNode> children`：当前节点的所有子节点（按字符分）。
    - `boolean isEnd`：是否为某个完整字符串的终点。


# FileManager

## 设计思路

`FileManager` 类是 `Gitlet` 的工作区与暂存区管理器，负责协调用户当前文件系统与版本控制系统之间的交互，包括文件状态检测、暂存区管理、文件快照操作等。

`Gitlet` 将版本控制拆分为三大文件区域：工作目录（`CWD`）、暂存区（`staging area`） 与 提交快照（`commit`）。`FileManager` 设计用于统一管理这些区域中的文件，提供高层次的接口用于判断文件的修改状态、未追踪状态、添加与删除，以及从提交记录中恢复文件。

暂存区被划分为 `addition`（新增或修改文件）和 `removal`（标记为删除的文件）两部分，并以 `blob` 哈希为唯一标识持久化管理。该类本身是可序列化的，用于保存暂存区状态。

## 实现逻辑

- 初始化：
  - 构造函数传入包括工作目录、`blob` 存储目录、暂存目录、提交管理器路径等信息，并初始化暂存记录。
  - 调用 `updateFiles()` 获取管理范围内的所有文件（包括工作区文件、`commit` 追踪文件、暂存记录）。

- 暂存管理：
  - 使用 `Map<String, String>` `addition` 和 `Set<String>` `removal` 分别记录待提交的新增/修改和删除操作。
  - `addToAddition()` 会将工作区中的文件内容写入暂存目录，生成 `blob` 哈希作为文件 `ID`。
  - 提供 `removeFromAddition()`、`addToRemoval()` 等方法维护暂存记录。
  - `clearStageArea()` 方法用于在一次提交后清空暂存状态和暂存目录。

- 文件状态检测（供 `status` 命令使用）：
  - `hasModified()`：判断文件是否被修改但尚未加入暂存。
  - `hasDeleted()`：判断文件是否被删除但未标记为待删除。
  - `isNotTracking()`：判断是否为未追踪文件。
  - `getStagedFiles()`、`getRemovedFiles()`、`getModifiedFiles()`、`getUntrackedFiles()`：分别获取不同状态文件列表（并排序）。

- 文件恢复与 `checkout`：
  - `checkout(Commit commit)`：将提交中追踪的所有文件恢复至工作目录。
  - `checkout(commit, fileName)`：恢复特定文件到工作区。

- 远程操作支持：
  - `fetchBlobFrom()`：从远程仓库拉取 `blob` 文件（如果本地不存在）。

## 主要字段

- `savePath`：当前 `FileManager` 对象的序列化保存路径（如 `.gitlet/stage`）
- `workingDir`：当前项目的工作目录
- `stagingBlobsDir`：暂存目录，用于保存待提交的 `blob` 文件副本
- `blobsDir`：版本库中所有 `blob` 文件的存储路径
- `commitManagerPath`：`CommitManager` 的保存路径（便于提取 `HEAD` 提交）
- `addition`：暂存添加记录，文件名 → `blob` 哈希值
- `removal`：暂存删除记录，仅记录文件名
- `filesInManagement`：管理范围内的文件名集合（`commit` + `CWD` + `addition` + `removal`）



# MergeManager

## 设计思路

`MergeManager` 是 `Gitlet` 项目中专门用于处理 `merge` 操作的类，封装了复杂的合并逻辑，包括分裂点判断、文件三方合并、冲突处理、工作区与暂存区的修改等。

合并是 `Git` 的核心操作之一，涉及多个提交之间的内容对比和冲突解析。`Gitlet` 将合并逻辑从 `Repository` 中解耦，独立封装在 `MergeManager` 中，以便清晰地组织与 `split-point` 识别、文件三方对比、冲突检测、操作分类（`checkout`、`remove`、`conflict` 处理）等相关的功能。

该类以“状态编码”（`split-current-given`）为基础，对所有涉及的文件执行分类处理，借助状态码快速判断每个文件需要执行的操作。

## 实现逻辑

- 构造器初始化：
  - 接收三个 `Commit`（`splitPoint`、`current`、`given`），未追踪文件集合、工作目录与 `blob` 存储路径；
  - 初始化四个集合：`checkoutFiles`、`removeFiles`、`conflictFiles`、`untrackedFiles`。

- 文件分析与状态分类：
  - `getAllFiles()`：合并三方提交中所有追踪的文件名集合；
  - `statusCode(fileName)`：为每个文件生成一个 3 位编码（`f-s-t`），表示在 `split`、`current`、`given` 中的状态；
  - 具体编码规则按“高度”比较而设计（例如 `f=1` 表示 `split` 中追踪了该文件）。

- 合并调度逻辑：
  - `merge()`：遍历所有文件并调用 `merge(fileName)` 执行分类判断；
  - 在 `merge(fileName)` 中根据状态码做出操作决策：
    - `112, 001` → `checkout`（还原并暂存）
    - `110` → `remove`
    - `123, 120, 102, 012` → 冲突

- 具体操作执行：
  - `doCheckout()`：将需要还原的文件从 `given commit` 检出到工作区，并加入暂存区；
  - `doRemove()`：将需要删除的文件从版本控制中移除；
  - `handleConflict()`：为冲突文件生成冲突标记格式内容（`HEAD` 与目标分支之间）并写入本地，随后加入暂存区。

- 冲突检测：
  - `encounteredConflict()`：返回是否存在冲突文件（非空即为冲突）。

## 状态编码说明（`split-current-given`）

- 例如 `statusCode = "112"` 表示：
  - 文件在 `split` 中存在（1）
  - 在 `current` 中相同（1）
  - 在 `given` 中有改动（2）
  - 此编码用于快速匹配合并场景的决策表（详见类中注释表格）。

## 主要字段

- `splitPoint`：当前分支与目标分支的最近公共祖先提交
- `currentCommit`：当前分支的最新提交
- `givenCommit`：待合并进当前分支的提交
- `untrackedFiles`：当前工作区中未被当前提交追踪的文件（用于冲突检测）
- `checkoutFiles`：合并后需要还原（`checkout`）并暂存的文件
- `removeFiles`：合并后需要从版本库中删除的文件
- `conflictFiles`：合并冲突文件集合
- `workingDir`：当前 `Gitlet` 工作目录
- `blobDir`：所有 `blob` 快照的存储目录（用于文件内容获取）



# Utils

## 设计思路

`Utils` 类是 `Gitlet` 项目的通用工具类，封装了大量文件处理、`SHA1` 哈希、对象序列化与反序列化、错误处理等常用功能，是多个模块依赖的基础库。

`Gitlet` 项目中多个核心功能依赖对文件系统、二进制对象和字符串的处理，为避免重复实现和逻辑混乱，所有通用操作被集中封装在 `Utils` 类中。该类仅包含静态方法，方便全局调用，不需要实例化，使用简单灵活。

`Utils` 采用模块化设计结构，按功能分类包括：哈希计算、文件读写、序列化与反序列化、路径拼接、异常处理等。

## 实现逻辑

- `SHA-1` 哈希计算
  - `sha1(Object... vals)`：接收任意数量的字符串或字节数组，计算其拼接后内容的 `SHA-1` 哈希。
  - 用于 `Gitlet` 中提交、`blob` 的唯一 `ID` 生成。

- 文件读取与写入
  - `readContents(File)` / `readContentsAsString(File)`：读取文件为字节数组或字符串。
  - `writeContents(File, Object...)`：将多个字符串/字节数组写入文件（可创建或覆盖），支持文件内容拼接。

- 对象序列化 / 反序列化
  - `serialize(Serializable)`：将对象转换为字节数组。
  - `writeObject(File, Serializable)`：将对象持久化写入文件。
  - `readObject(File, Class<T>)`：从文件中读取对象并转换为指定类型。

- 路径拼接
  - `join(String, ...)` 和 `join(File, ...)`：拼接路径为新的 `File` 对象，等效于 `Paths.get(...).toFile()`，用于文件定位。

- 目录与文件操作
  - `plainFilenamesIn(File)`：返回目录下所有普通文件名（非文件夹），按字典序排序。
  - `restrictedDelete(File)`：仅当当前目录为合法 `Gitlet` 工作区时才允许删除文件（存在 `.gitlet` 文件夹）。
  - `clean(File)`：删除目录中所有普通文件（不递归子目录）。

- 错误处理与消息
  - `error(String, Object...)`：生成 `GitletException` 异常（格式化输出）。
  - `message(String, Object...)`：格式化打印信息，用于命令行提示。

- 辅助工具
  - `fileHashIn(File dir, String fileName)`：计算某个目录中文件的“文件名 + 内容”哈希，用于判断文件修改状态。



# 辅助类与调试支持

## GitletException

`GitletException` 是 `Gitlet` 项目中所有自定义运行时错误的基类，用于统一处理用户操作中的异常情形，并提供清晰的错误提示。

## 设计思路

为了与系统错误区分，同时在出现异常时能友好地向用户输出错误信息，`Gitlet` 定义了自己的运行时异常类，继承自 `Java` 标准库的 `RuntimeException`。

该异常类用于表示如参数错误、非法状态、文件缺失、冲突错误等逻辑异常，并在 `Main.java` 中统一捕获处理，使错误提示行为一致。

## 实现逻辑

- 提供两个构造方法：
  - `GitletException()`：默认构造，无错误信息；
  - `GitletException(String msg)`：带自定义错误信息，用于直接输出用户可见的提示。

该类常与 `Utils.error()` 方法配合使用，用于快速抛出带格式化提示的错误异常。

## DumpObj

`DumpObj` 是一个独立的调试工具类，用于将 `Gitlet` 中序列化存储的对象（如 `CommitManager`、`FileManager` 等）从磁盘读取出来并打印其内容。

## 设计思路

在 `Gitlet` 开发过程中，为了便于调试序列化对象的实际结构与状态，引入该工具类。它从命令行接收文件名，反序列化后调用对象本身实现的 `dump()` 方法，输出其结构化内容。

## 实现逻辑

- `main(String... files)` 接收若干路径；
- 对每个路径，调用 `Utils.readObject(File, Dumpable.class)` 加载对象；
- 要求对象实现 `Dumpable` 接口，并具备 `dump()` 方法；
- 将输出打印到标准输出，供开发者调试观察。

## Dumpable

`Dumpable` 是 `Gitlet` 项目中所有可被调试打印的对象所需实现的接口。

## 设计思路

为确保 `DumpObj` 能统一调用所有类的打印方法而不依赖具体类型，定义该接口作为类型标识。

## 实现逻辑

- 继承自 `Serializable`，表示同时具备序列化能力；
- 定义唯一方法 `void dump()`，用于自定义输出对象内部状态。