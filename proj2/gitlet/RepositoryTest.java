//package gitlet;
//
//import org.junit.jupiter.api.*;
//import java.io.*;
//import java.nio.file.Path;
//import java.util.Objects;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class RepositoryTest {
//
//    private Path originalWorkingDir;
//    private final File TEST_DIR = new File("/Users/scullyhitchcock/Desktop/test_gitlet");
//    private final String FILE_NAME1 = "test1.txt", FILE_NAME2 = "test2.txt";
//    private final String TEXT1 = "Content for test1", TEXT2 = "Content for test2";
//    private final String HASH1 = Utils.sha1(FILE_NAME1, TEXT1);
//    private final String HASH2 = Utils.sha1(FILE_NAME2, TEXT2);
//
//    @BeforeEach
//    public void setUp() {
//        // 保存原始工作目录
//        originalWorkingDir = Path.of(System.getProperty("user.dir"));
//
//        if (!TEST_DIR.exists()) {
//            TEST_DIR.mkdir();
//        }
//
//        int index = 0;
//        File candidate;
//        do {
//            candidate = new File(TEST_DIR, "test" + index);
//            index++;
//        } while (candidate.exists());
//
//        candidate.mkdirs();
//
//        // 设置为临时目录
//        System.setProperty("user.dir", candidate.toPath().toAbsolutePath().toString());
//        Repository.refreshCWDForUnitTest();
//    }
//
//    @AfterEach
//    public void tearDown() {
//        // 恢复原工作目录
//        System.setProperty("user.dir", originalWorkingDir.toString());
//    }
//
//    @Test
//    public void testInitCreatesGitletDirectory() {
//        Repository.setup();
//
//        assertTrue(Repository.GITLET_DIR.exists(), ".gitlet directory should be created");
//        assertTrue(Repository.STAGING_BLOBS.exists(), "staging directory should be created");
//        assertTrue(Repository.BLOBS.exists(), "blobs directory should be created");
//        assertTrue(Repository.COMMITS.exists(), "commits directory should be created");
//        assertTrue(Repository.COMMIT_MANAGER.exists(), "CommitManager file should be created");
//        assertTrue(Repository.FILE_MANAGER.exists(), "fileManager file should be created");
//        assertNotNull(Repository.COMMITS.list(), "commits directory should not be null");
//        assertTrue(Objects.requireNonNull(Repository.COMMITS.list()).length > 0, "commits directory should contain initial commit file");
//        String[] commitFiles = Repository.COMMITS.list();
//        assertNotNull(Repository.COMMITS);
//
//        File firstCommitFile = new File(Repository.COMMITS, commitFiles[0]);
//        Commit initialCommit = Utils.readObject(firstCommitFile, Commit.class);
//        assertEquals("initial commit", initialCommit.getMessage(), "The first commit should be the initial commit");
//    }
//
//    @Test
//    public void testAddAndCommitFile() throws IOException {
//        // Step 1：初始化
//        Repository.setup();
//        String commitMsg = "Commit test1";
//
//        // Step 2: 创建两个文本文件
//        File file1 = Utils.join(Repository.CWD, FILE_NAME1);
//        Utils.writeContents(file1, TEXT1);
//        File file2 = Utils.join(Repository.CWD, FILE_NAME2);
//        Utils.writeContents(file2, TEXT2);
//
//        // Step 3: add test1.txt and assert staging and blob count
//        Repository.addFile(FILE_NAME1);
//        File staged1 = new File(Repository.STAGING_BLOBS, HASH1);
//        assertTrue(staged1.exists(), FILE_NAME1 + "'s blob should be staged for addition");
//
//        // Step 4: add test2.txt and assert staging and blob count
//        Repository.addFile(FILE_NAME2);
//        File staged2 = new File(Repository.STAGING_BLOBS, HASH2);
//        assertTrue(staged2.exists(), FILE_NAME2 + "'s blob should be staged for addition");
//
//        // Step 5: commit with message and assert commit count
//        Repository.commit(commitMsg, null);
//        assertEquals(
//                2, Objects.requireNonNull(Repository.COMMITS.list()).length,
//                "There should be 2 commits after second commit");
//        assertFalse(staged1.exists(), FILE_NAME1 + "'blob should be cleared");
//        assertFalse(staged2.exists(), FILE_NAME2 + "'blob should be cleared");
//        File blob1 = new File(Repository.BLOBS, HASH1);
//        File blob2 = new File(Repository.BLOBS, HASH2);
//        assertTrue(blob1.exists(), FILE_NAME1 + "'s blob should be saved Permanently");
//        assertTrue(blob2.exists(), FILE_NAME2 + "'s blob should be saved Permanently");
//        String content1 = Utils.readContentsAsString(blob1);
//        String content2 = Utils.readContentsAsString(blob2);
//        assertEquals(content1, TEXT1, "The content of " + FILE_NAME1 + "'blob should remain unchanged");
//        assertEquals(content2, TEXT2, "The content of " + FILE_NAME2 + "'blob should remain unchanged");
//
//
//        // Step 6: 检查 HEAD commit 是否追踪两个文件
//        CommitManager manager = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        Commit headCommit = manager.getHeadCommit();
//        assertNotNull(headCommit, "Head commit should not be null");
//        assertEquals(2, headCommit.getTrackedFile().size(), "Head commit should track 2 files");
//        assertEquals(HASH1, headCommit.getTrackedFile().get(FILE_NAME1), "Head commit should be tracking " + FILE_NAME1);
//        assertEquals(HASH2, headCommit.getTrackedFile().get(FILE_NAME2), "Head commit should be tracking " + FILE_NAME2);
//    }
//
//    @Test
//    public void testAddDuplicateFileAndCommit() throws IOException {
//        // 前置步骤
//        Repository.setup();
//        File file1 = Utils.join(Repository.CWD, FILE_NAME1);
//        Utils.writeContents(file1, TEXT1);
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commit1", null);
//
//        // 1 尝试重复操作 add FILE_NAME1，检查 staging 区域，应该什么都没有
//        Repository.addFile(FILE_NAME1);
//        assertEquals(0, Objects.requireNonNull(Repository.STAGING_BLOBS.list()).length, "Staging area should be empty for already tracked file");
//
//        // 2 尝试 commit，应该得到输出 "No changes added to the commit."
//        GitletException duplicateCommitException = assertThrows(GitletException.class, () -> {
//            Repository.commit("duplicate commit", null);
//        });
//        assertEquals("No changes added to the commit.", duplicateCommitException.getMessage());
//
//        // 3 尝试 add 一个不存在的文件，应该得到一个GitletException异常，报错信息应该为 "File does not exist."
//        GitletException exception = assertThrows(GitletException.class, () -> Repository.addFile("not_exist.txt"));
//        assertEquals("File does not exist.", exception.getMessage());
//    }
//
//    @Test
//    public void testRemoveFile() throws IOException {
//        Repository.setup();
//        File file1 = Utils.join(Repository.CWD, FILE_NAME1);
//        File file2 = Utils.join(Repository.CWD, FILE_NAME2);
//        Utils.writeContents(file1, TEXT1);
//        Utils.writeContents(file2, TEXT2);
//
//        Repository.addFile(FILE_NAME1);
//        File staged1 = new File(Repository.STAGING_BLOBS, HASH1);
//        assertTrue(staged1.exists(), FILE_NAME1 + "'s blob should be staged for addition");
//        Repository.commit("commit1", null);
//
//        // 检查 commit1 追踪 FILE_NAME1
//        CommitManager manager1 = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        Commit commit1 = manager1.getHeadCommit();
//        assertEquals(HASH1, commit1.getTrackedFile().get(FILE_NAME1), "Commit1 should be tracking " + FILE_NAME1);
//
//        Repository.remove(FILE_NAME1);
//        // FILE_MANAGER 的 removal 应该记录 FILE_NAME1
//        FileManager fileManager = Utils.readObject(Repository.FILE_MANAGER, FileManager.class);
//        assertTrue(fileManager.getRemoval().containsKey(FILE_NAME1), "FILE_NAME1 should be marked for removal in FILE_MANAGER");
//        // FILE_MANAGER 的 addition 应该不记录 FILE_NAME1
//        assertFalse(fileManager.getAddition().containsKey(FILE_NAME1), "FILE_NAME1 should not be in addition after removal");
//        // CWD 应该没有 FILE_NAME1
//        File cwdFile1 = Utils.join(Repository.CWD, FILE_NAME1);
//        assertFalse(cwdFile1.exists(), "Working directory should not contain " + FILE_NAME1);
//
//        Repository.commit("commit2", null);
//
//        // commit2 应该没有追踪任何文件
//        CommitManager manager2 = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        Commit commit2 = manager2.getHeadCommit();
//        assertEquals(0, commit2.getTrackedFile().size(), "Commit2 should not track any file");
//
//
//        Repository.addFile(FILE_NAME2);
//        File staged2 = new File(Repository.STAGING_BLOBS, HASH2);
//        assertTrue(staged2.exists(), FILE_NAME2 + "'s blob should be staged for addition");
//
//        Repository.remove(FILE_NAME2);
//        // REMOVAL 应该没有 FILE_NAME2
//        fileManager = Utils.readObject(Repository.FILE_MANAGER, FileManager.class);
//        assertFalse(fileManager.getRemoval().containsKey(FILE_NAME2), "FILE_NAME2 should not be marked for removal after being unstaged");
//
//        // CWD 应该仍然有 FILE_NAME2
//        assertTrue(Utils.join(Repository.CWD, FILE_NAME2).exists(), "Working directory should still contain " + FILE_NAME2);
//
//        // commit3: 输出应为 No changes added to the commit.
//        GitletException commit3Exception = assertThrows(GitletException.class, () -> {
//        Repository.commit("commit3", null);
//        });
//        assertEquals("No changes added to the commit.", commit3Exception.getMessage());
//
//        // remove 不存在的文件，抛异常
//        GitletException exception = assertThrows(GitletException.class, () -> Repository.remove("not_file"));
//        assertEquals("No reason to remove the file.", exception.getMessage());
//    }
//
//    @Test
//    public void testLog() throws IOException {
//        /* 预期格式
//        ===
//        commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
//        Date: Thu Nov 9 20:00:05 2017 -0800
//        A commit message.
//
//        ===
//        commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
//        Date: Thu Nov 9 17:01:33 2017 -0800
//        Another commit message.
//
//        ===
//        commit e881c9575d180a215d1a636545b8fd9abfb1d2bb
//        Date: Wed Dec 31 16:00:00 1969 -0800
//        initial commit
//         */
//        Repository.setup();
//        File wug = Utils.join(Repository.CWD, "wug.txt");
//        File notwug = Utils.join(Repository.CWD, "notwug.txt");
//
//        Utils.writeContents(wug, "wug content");
//        Repository.addFile("wug.txt");
//        Repository.commit("First commit wug.txt", null);
//
//        Utils.writeContents(notwug, "not wug");
//        Repository.addFile("notwug.txt");
//        Repository.commit("First commit notwug.txt", null);
//
//        Utils.writeContents(wug, "modified wug");
//        Repository.addFile("wug.txt");
//        Repository.commit("Modified wug.txt", null);
//
//        // 捕获 log 输出
//        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
//        PrintStream originalOut = System.out;
//        System.setOut(new PrintStream(outContent));
//
//        Repository.log();
//
//        System.setOut(originalOut);
//        String logOutput = outContent.toString();
//
//        assertTrue(logOutput.contains("First commit wug.txt"));
//        assertTrue(logOutput.contains("First commit notwug.txt"));
//        assertTrue(logOutput.contains("Modified wug.txt"));
//        assertTrue(logOutput.contains("initial commit"));
//        assertTrue(logOutput.contains("==="));
//
//        // 按 === 拆分 commit 块
//        String[] entries = logOutput.split("(?=^===)", -1);
//        String pattern = "(?s)^===\\s*commit\\s+[a-f0-9]{40}\\s+Date: .+?\\s+.+";
//        for (String entry : entries) {
//            assertTrue(entry.matches(pattern), "Log entry format invalid:\n" + entry);
//        }
//    }
//
//    @Test
//    public void testFind() {
//        Repository.setup();
//        File file = Utils.join(Repository.CWD, FILE_NAME1);
//        Utils.writeContents(file, TEXT1);
//
//        // 提交1：Update
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("Update", null);
//
//        // 提交2：Fix bug
//        Utils.writeContents(file, "new content");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("Fix bug", null);
//
//        // 提交3：Update again
//        Utils.writeContents(file, "third content");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("Update", null);
//
//        // 捕获输出
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        PrintStream originalOut = System.out;
//        System.setOut(new PrintStream(out));
//
//        Repository.find("Update");
//        String output = out.toString().trim();
//        String[] lines = output.split("\n");
//        assertEquals(2, lines.length, "Should find 2 commits with message 'Update'");
//        for (String line : lines) {
//            assertEquals(40, line.length());
//        }
//
//        out.reset();
//        Repository.find("Fix bug");
//        String output2 = out.toString().trim();
//        assertEquals(40, output2.length(), "Should find 1 commit with message 'Fix bug'");
//
//        out.reset();
//        Repository.find("initial commit");
//        String output3 = out.toString().trim();
//        assertEquals(40, output3.length(), "Should find 1 commit with message 'initial commit'");
//
//        out.reset();
//        GitletException exception = assertThrows(GitletException.class, () -> Repository.find("test"));
//        System.setOut(originalOut);
//        assertEquals("Found no commit with that message.", exception.getMessage());
//    }
//
//    @Test
//    public void testCreateNewBranch() {
//        // 进行两次提交，msg 分别为“commitA”和“commitB”
//        // branch("new branch") 创建新分支
//        // 提交一次“commitC”
//        // checkout("main") 切换到原分支 main
//        // 提交一次“commitD”
//        // log输出，应该得到大概 D -> B -> A -> initial commit 四个提交的输出
//        // global-log输出，应该得到总共5无序的提交输出
//
//        Repository.setup();
//        File file = Utils.join(Repository.CWD, FILE_NAME1);
//
//        // commitA
//        Utils.writeContents(file, "A");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitA", null);
//
//        // commitB
//        Utils.writeContents(file, "B");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitB", null);
//
//        // 创建分支并提交 commitC
//        Repository.branch("new-branch");
//        Repository.checkout(new String[]{"new-branch"});
//        Utils.writeContents(file, "C");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitC", null);
//
//        // 切换回 main 分支并提交 commitD
//        Repository.checkout(new String[]{"main"});
//        Utils.writeContents(file, "D");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitD", null);
//
//        // 检查 log 输出（当前在 main 分支，应为 D -> B -> A -> initial commit）
//        ByteArrayOutputStream logOut = new ByteArrayOutputStream();
//        PrintStream originalOut = System.out;
//        System.setOut(new PrintStream(logOut));
//        Repository.log();
//        System.setOut(originalOut);
//
//        String logOutput = logOut.toString();
//        assertTrue(logOutput.contains("commitD"), "log should include commitD");
//        assertTrue(logOutput.contains("commitB"), "log should include commitB");
//        assertTrue(logOutput.contains("commitA"), "log should include commitA");
//        assertTrue(logOutput.contains("initial commit"), "log should include initial commit");
//        assertFalse(logOutput.contains("commitC"), "log should NOT include commitC on main branch");
//
//        // 检查 global-log 输出应包含所有5个提交
//        ByteArrayOutputStream globalLogOut = new ByteArrayOutputStream();
//        System.setOut(new PrintStream(globalLogOut));
//        Repository.globalLog();
//        System.setOut(originalOut);
//
//        String globalOutput = globalLogOut.toString();
//        assertTrue(globalOutput.contains("commitA"), "global-log should include commitA");
//        assertTrue(globalOutput.contains("commitB"), "global-log should include commitB");
//        assertTrue(globalOutput.contains("commitC"), "global-log should include commitC");
//        assertTrue(globalOutput.contains("commitD"), "global-log should include commitD");
//        assertTrue(globalOutput.contains("initial commit"), "global-log should include initial commit");
//    }
//
//    @Test
//    public void testCheckout() {
//
//        Repository.setup();
//        File file = Utils.join(Repository.CWD, FILE_NAME1);
//
//        // 初始提交
//        Utils.writeContents(file, "A");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitA", null);
//
//        // 修改并提交 commitB
//        Utils.writeContents(file, "B");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitB", null);
//
//        // 创建新分支并切换
//        Repository.branch("dev");
//        Repository.checkout(new String[]{"dev"});
//
//        // dev 分支上提交 commitC
//        Utils.writeContents(file, "C");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitC", null);
//
//        // 检查切换回 main，文件应为 B 内容
//        Repository.checkout(new String[]{"main"});
//        String content = Utils.readContentsAsString(file);
//        assertEquals("B", content, "After checking out to main, file content should be 'B'");
//
//        // 再切换回 dev，文件应为 C 内容
//        Repository.checkout(new String[]{"dev"});
//        content = Utils.readContentsAsString(file);
//        assertEquals("C", content, "After checking out to dev, file content should be 'C'");
//
//        // 尝试切换到当前分支，抛出异常
//        GitletException sameBranchEx = assertThrows(GitletException.class, () -> {
//            Repository.checkout(new String[]{"dev"});
//        });
//        assertEquals("No need to checkout the current branch.", sameBranchEx.getMessage());
//
//        // 切换不存在的分支，抛异常
//        GitletException noBranchEx = assertThrows(GitletException.class, () -> {
//            Repository.checkout(new String[]{"nonexistent"});
//        });
//        assertEquals("No such branch exists.", noBranchEx.getMessage());
//
//        // 用 HEAD commit 恢复文件内容
//        Utils.writeContents(file, "overwrite dev content");
//        Repository.checkout(new String[]{"--", FILE_NAME1});
//        assertEquals("C", Utils.readContentsAsString(file), "Checkout -- [file] should restore file from HEAD");
//
//        // checkout 到某个 commit 的文件
//        CommitManager manager = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        Commit commitB = manager.findByMessage("commitB").get(0);
//        Repository.checkout(new String[]{commitB.id(), "--", FILE_NAME1});
//        assertEquals("B", Utils.readContentsAsString(file), "File should be restored to version from commitB");
//
//        // checkout 不存在的 commit id
//        GitletException noCommitEx = assertThrows(GitletException.class, () -> {
//            Repository.checkout(new String[]{"abc123", "--", FILE_NAME1});
//        });
//        assertEquals("No commit with that id exists.", noCommitEx.getMessage());
//
//        // checkout 存在的 commit，但文件不在其中
//        Utils.writeContents(Utils.join(Repository.CWD, FILE_NAME2), TEXT2);
//        Repository.addFile(FILE_NAME2);
//        Repository.commit("commit with file2", null);
//
//        GitletException missingFileEx = assertThrows(GitletException.class, () -> {
//            Repository.checkout(new String[]{commitB.id(), "--", FILE_NAME2});
//        });
//        assertEquals("File does not exist in that commit.", missingFileEx.getMessage());
//    }
//
//    @Test
//    public void testCheckout1() {
//        Repository.setup();
//
//        // 创建文件 A，写入内容并提交
//        File fileA = Utils.join(Repository.CWD, FILE_NAME1);
//        Utils.writeContents(fileA, TEXT1);
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commit A", null);
//
//        // 创建新分支并切换到该分支
//        Repository.branch("new-branch");
//        Repository.checkout(new String[]{"new-branch"});
//
//        // 删除文件 A 并提交
//        Repository.remove(FILE_NAME1);
//        Repository.commit("remove A", null);
//
//        // 手动创建一个未追踪的文件 A（与 main 分支的版本冲突）
//        Utils.writeContents(fileA, "untracked content");
//
//        // 尝试切换回 main 分支，应抛出异常
//        GitletException ex = assertThrows(GitletException.class, () -> {
//            Repository.checkout(new String[]{"main"});
//        });
//        assertEquals("There is an untracked file in the way; delete it, or add and commit it first.", ex.getMessage());
//
//        // add A
//        Repository.addFile(FILE_NAME1);
//        // 提交
//        Repository.commit("commit again A", null);
//        // remove A，立即手动创建 A
//        Repository.remove(FILE_NAME1);
//        Utils.writeContents(fileA, "conflicting untracked");
//        // checkout main, 预期一样的报错
//        GitletException ex2 = assertThrows(GitletException.class, () -> {
//            Repository.checkout(new String[]{"main"});
//        });
//        assertEquals("There is an untracked file in the way; delete it, or add and commit it first.", ex2.getMessage());
//    }
//
//    @Test
//    public void testFileStatus1() throws IOException {
//        Repository.setup();
//
//        File file1 = Utils.join(Repository.CWD, FILE_NAME1);
//        File file2 = Utils.join(Repository.CWD, FILE_NAME2);
//        Utils.writeContents(file1, TEXT1);
//        Utils.writeContents(file2, TEXT2);
//
//        // 添加 file1 到暂存区
//        Repository.addFile(FILE_NAME1);
//
//        // 提交 file1
//        Repository.commit("Commit file1", null);
//
//        // 修改 file1 内容（已追踪但未暂存的修改）
//        Utils.writeContents(file1, "Modified content");
//
//        // 删除 file2（未追踪但存在）
//        file2.delete();
//
//        // 创建一个未追踪的新文件 file3
//        String fileName3 = "file3.txt";
//        File file3 = Utils.join(Repository.CWD, fileName3);
//        Utils.writeContents(file3, "new file");
//
//        // 捕获 status 输出
//        ByteArrayOutputStream statusOut = new ByteArrayOutputStream();
//        PrintStream originalOut = System.out;
//        System.setOut(new PrintStream(statusOut));
//
//        Repository.status();
//
//        System.setOut(originalOut);
//        String output = statusOut.toString();
//
//        assertTrue(output.contains("=== Modifications Not Staged For Commit ===\n" + FILE_NAME1), "Modified file1 should appear under modified list");
//        assertTrue(output.contains("=== Untracked Files ===\n" + fileName3), "Untracked file3 should appear");
//        assertTrue(output.contains("=== Staged Files ==="), "Should list staged files (empty in this case)");
//        assertTrue(output.contains("=== Removed Files ==="), "Should list removed files (empty in this case)");
//    }
//
//    @Test
//    public void testFileStatus2() {
//        // 创建并 add file1
//        // 创建并 add file2
//        // 创建并 add file3
//        // 创建并 add file4
//        // 创建并 add file5
//        // commit
//        // status 应无文件输出
//
//        // 创建新 branch
//        // 移动 head 至新 branch
//
//        // remove file1 (removed)
//        // remove file2 (removed)
//        // 重新创建 file2 (removed + untracked)
//        // 工作区手动修改 file3 (modified)
//        // 工作区手动删除 file4 (deleted)
//        // 创建 file6 并 add (staged)
//        // 创建 file7 并 add (staged)
//        // 手动删除 file7 (staged + deleted)
//        // 创建 file8 (untracked)
//
//        Repository.setup();
//
//        // 创建并 add file1 到 file5
//        String[] files = {"file1.txt", "file2.txt", "file3.txt", "file4.txt", "file5.txt"};
//        for (String f : files) {
//            File file = Utils.join(Repository.CWD, f);
//            Utils.writeContents(file, "original " + f);
//            Repository.addFile(f);
//        }
//
//        Repository.commit("commit file1~5", null);
//
//        // 创建新分支并切换
//        Repository.branch("dev");
//        Repository.checkout(new String[]{"dev"});
//
//        // remove file1
//        Repository.remove("file1.txt");
//
//        // remove file2，然后重新创建 file2（模拟 untracked）
//        Repository.remove("file2.txt");
//        File file2 = Utils.join(Repository.CWD, "file2.txt");
//        Utils.writeContents(file2, "original file2");
//
//        // 修改 file3
//        File file3 = Utils.join(Repository.CWD, "file3.txt");
//        Utils.writeContents(file3, "modified file3");
//
//        // 删除 file4
//        File file4 = Utils.join(Repository.CWD, "file4.txt");
//        file4.delete();
//
//        // 创建并 add file6
//        File file6 = Utils.join(Repository.CWD, "file6.txt");
//        Utils.writeContents(file6, "content6");
//        Repository.addFile("file6.txt");
//
//        // 创建并 add file7，然后删除 file7
//        File file7 = Utils.join(Repository.CWD, "file7.txt");
//        Utils.writeContents(file7, "content7");
//        Repository.addFile("file7.txt");
//        file7.delete();
//
//        // 创建 file8（untracked）
//        File file8 = Utils.join(Repository.CWD, "file8.txt");
//        Utils.writeContents(file8, "file8 content");
//
//        // 捕获 status 输出
//        // 调用 status 应输出以下信息：
//        // branches: *新 branch, main
//        // staged: file6, file7
//        // removed（按字母顺序排列输出）: file1, file2
//        // modified（按字母顺序排列输出）: file3 (modified), file4 (deleted), file7 (deleted)
//        // untracked（按字母顺序排列输出）: file2, file8
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        PrintStream originalOut = System.out;
//        System.setOut(new PrintStream(out));
//        Repository.status();
//        System.setOut(originalOut);
//
//        String statusOut = out.toString();
//        assertTrue(statusOut.contains("=== Branches ===\n*dev\nmain"), "Branch section should list current branch 'dev' and 'main'");
//        assertTrue(statusOut.contains("=== Staged Files ===\nfile6.txt\nfile7.txt"), "file6 and file7 should be listed in staged files");
//        assertTrue(statusOut.contains("=== Removed Files ===\nfile1.txt\nfile2.txt"), "file1 and file2 should be listed in removed files");
//        assertTrue(statusOut.contains("=== Modifications Not Staged For Commit ===\nfile3.txt (modified)\nfile4.txt (deleted)\nfile7.txt (deleted)"),
//                   "file3, file4, file7 should be listed in modified section");
//        assertTrue(statusOut.contains("=== Untracked Files ===\nfile2.txt\nfile8.txt"), "file2 and file8 should be listed in untracked section");
//    }
//
//    @Test
//    public void resetTest() {
//        Repository.setup();
//
//        // 创建 3 个文件 A B C 暂存并提交
//        // 获取提交 id a
//        // 新建分支
//        // checkout 分支
//        // rm 文件 A
//        // 修改另外两个文件 B C
//        // 新建第 4 份文件 D
//        // 暂存并提交
//        // reset a
//        // 创建文件 A B C
//        String fileA = "A.txt", fileB = "B.txt", fileC = "C.txt", fileD = "D.txt";
//        Utils.writeContents(Utils.join(Repository.CWD, fileA), "Content A");
//        Utils.writeContents(Utils.join(Repository.CWD, fileB), "Content B");
//        Utils.writeContents(Utils.join(Repository.CWD, fileC), "Content C");
//
//        Repository.addFile(fileA);
//        Repository.addFile(fileB);
//        Repository.addFile(fileC);
//        Repository.commit("commit ABC", null);
//
//        CommitManager manager = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        String commitA = manager.getHeadCommit().id();
//
//        // 新建分支并切换
//        Repository.branch("dev");
//        Repository.checkout(new String[]{"dev"});
//
//        // 删除 A
//        Repository.remove(fileA);
//
//        // 修改 B C
//        Utils.writeContents(Utils.join(Repository.CWD, fileB), "Modified B");
//        Utils.writeContents(Utils.join(Repository.CWD, fileC), "Modified C");
//
//        // 创建 D
//        Utils.writeContents(Utils.join(Repository.CWD, fileD), "Content D");
//
//        Repository.addFile(fileB);
//        Repository.addFile(fileC);
//        Repository.addFile(fileD);
//        Repository.commit("modify BC and add D", null);
//
//        // reset 到 commitA
//        Repository.reset(commitA);
//
//        // 断言：只有 A B C 存在，且内容是原始内容
//        File cwdA = Utils.join(Repository.CWD, fileA);
//        File cwdB = Utils.join(Repository.CWD, fileB);
//        File cwdC = Utils.join(Repository.CWD, fileC);
//        File cwdD = Utils.join(Repository.CWD, fileD);
//
//        assertTrue(cwdA.exists(), "File A should exist after reset");
//        assertTrue(cwdB.exists(), "File B should exist after reset");
//        assertTrue(cwdC.exists(), "File C should exist after reset");
//        assertFalse(cwdD.exists(), "File D should not exist after reset");
//
//        assertEquals("Content A", Utils.readContentsAsString(cwdA), "Content of A should be original");
//        assertEquals("Content B", Utils.readContentsAsString(cwdB), "Content of B should be original");
//        assertEquals("Content C", Utils.readContentsAsString(cwdC), "Content of C should be original");
//
//        // 断言：HEAD commit id 等于 commitA
//        CommitManager afterResetManager = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        assertEquals(commitA, afterResetManager.getHeadCommit().id(), "HEAD should point to original commit A");
//    }
//
//    @Test
//    public void splitPointTest() {
//        // 提交2次 A 和 B
//        Repository.setup();
//        File file = Utils.join(Repository.CWD, FILE_NAME1);
//
//        Utils.writeContents(file, "A");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitA", null);
//
//        Utils.writeContents(file, "B");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitB", null);
//
//        // 创建分支 b1，checkout b1
//        Repository.branch("b1");
//        Repository.checkout(new String[]{"b1"});
//
//        // 提交1次 C
//        Utils.writeContents(file, "C");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitC", null);
//
//        // checkout main
//        Repository.checkout(new String[]{"main"});
//
//        // 提交1次 D
//        Utils.writeContents(file, "D");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitD", null);
//
//        // 创建分支 b2，checkout b2
//        Repository.branch("b2");
//        Repository.checkout(new String[]{"b2"});
//
//        // 提交1次 E
//        Utils.writeContents(file, "E");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitE", null);
//
//        // checkout main
//        Repository.checkout(new String[]{"main"});
//
//        // 提交1次 F
//        Utils.writeContents(file, "F");
//        Repository.addFile(FILE_NAME1);
//        Repository.commit("commitF", null);
//
//        // 获取 b1 和 b2 的 HEAD commit
//        CommitManager manager = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        String b1Head = manager.getBranchCommit("b1").id();
//        String b2Head = manager.getBranchCommit("b2").id();
//
//        Commit splitPoint = manager.findSplitPoint(b1Head, b2Head);
//
//        assertEquals("commitB", splitPoint.getMessage(), "Split point should be commitB");
//    }
//
//    @Test
//    public void mergeTest() throws IOException {
//        Repository.setup();
//        File file = Utils.join(Repository.CWD, "test.txt");
//
//        // Step 1: initial commit
//        Utils.writeContents(file, "base");
//        Repository.addFile("test.txt");
//        Repository.commit("base", null);
//
//        // Step 2: create and switch to 'dev' branch
//        Repository.branch("dev");
//        Repository.checkout(new String[]{"dev"});
//
//        // Step 3: dev modifies test.txt and commits
//        Utils.writeContents(file, "dev version");
//        Repository.addFile("test.txt");
//        Repository.commit("dev commit", null);
//
//        // Step 4: switch back to main
//        Repository.checkout(new String[]{"main"});
//
//        // Step 5: main modifies test.txt and commits
//        Utils.writeContents(file, "main version");
//        Repository.addFile("test.txt");
//        Repository.commit("main commit", null);
//
//        // Step 6: perform merge with dev
//        Repository.merge("dev");
//
//        // Step 7: check conflict content in working directory
//        String mergedContent = Utils.readContentsAsString(file);
//        assertTrue(mergedContent.contains("<<<<<<< HEAD"), "Merged file should contain HEAD marker");
//        assertTrue(mergedContent.contains("======="), "Merged file should contain divider");
//        assertTrue(mergedContent.contains(">>>>>>>"), "Merged file should contain branch marker");
//
//        // Step 8: verify merge commit has two parents
//        CommitManager manager = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
//        Commit head = manager.getHeadCommit();
//        assertEquals(2, head.getParentIds().size(), "Merge commit should have two parents");
//    }
//}
