package gitlet;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryTest {

    private Path originalWorkingDir;
    private final File TEST_DIR = new File("/Users/scullyhitchcock/Desktop/test_gitlet");
    private final String FILE_NAME1 = "test1.txt", FILE_NAME2 = "test2.txt";
    private final String TEXT1 = "Content for test1", TEXT2 = "Content for test2";

    @BeforeEach
    public void setUp() {
        // 保存原始工作目录
        originalWorkingDir = Path.of(System.getProperty("user.dir"));

        if (!TEST_DIR.exists()) {
            TEST_DIR.mkdir();
        }

        int index = 0;
        File candidate;
        do {
            candidate = new File(TEST_DIR, "test" + index);
            index++;
        } while (candidate.exists());

        candidate.mkdirs();

        // 设置为临时目录
        System.setProperty("user.dir", candidate.toPath().toAbsolutePath().toString());
    }

    @AfterEach
    public void tearDown() {
        // 恢复原工作目录
        System.setProperty("user.dir", originalWorkingDir.toString());
    }

    @Test
    public void testInitCreatesGitletDirectory() {
        Repository.setup();

        assertTrue(Repository.CWD.exists(), ".gitlet directory should be created");
        assertTrue(Repository.STAGING.exists(), "staging directory should be created");
        assertTrue(Repository.ADDITION.exists(), "staging/files should be created");
        assertTrue(Repository.REMOVAL.exists(), "staging/removal should be created");
        assertTrue(Repository.BLOBS.exists(), "blobs directory should be created");
        assertTrue(Repository.COMMITS.exists(), "commits directory should be created");
        assertTrue(Repository.COMMIT_MANAGER.exists(), "manager file should be created");

        assertNotNull(Repository.COMMITS.list(), "commits directory should not be null");
        assertTrue(Objects.requireNonNull(Repository.COMMITS.list()).length > 0, "commits directory should contain initial commit file");
        String[] commitFiles = Repository.COMMITS.list();
        assertNotNull(Repository.COMMITS);

        File firstCommitFile = new File(Repository.COMMITS, commitFiles[0]);
        Commit initialCommit = Utils.readObject(firstCommitFile, Commit.class);
        assertEquals("initial commit", initialCommit.getMessage(), "The first commit should be the initial commit");
    }

    @Test
    public void testAddAndCommitFile() throws IOException {
        // Step 1：初始化
        Repository.setup();
        String commitMsg = "Commit test1";

        // Step 2: 创建两个文本文件
        File file1 = Utils.join(Repository.CWD, FILE_NAME1);
        Utils.createFile(file1);
        Utils.writeContents(file1, TEXT1);
        File file2 = Utils.join(Repository.CWD, FILE_NAME2);
        Utils.createFile(file2);
        Utils.writeObject(file2, TEXT2);

        // Step 3: add test1.txt and assert staging and blob count
        Repository.addFile(FILE_NAME1);
        File staged1 = new File(Repository.ADDITION, FILE_NAME1);
        assertTrue(staged1.exists(), FILE_NAME1 + " should be staged");
        assertEquals(
                1, Objects.requireNonNull(Repository.STAGING_BLOBS.list()).length,
                "There should be 1 blob after first add");

        // Step 4: add test2.txt and assert staging and blob count
        Repository.addFile(FILE_NAME2);
        File staged2 = new File(Repository.ADDITION, FILE_NAME2);
        assertTrue(staged2.exists(), FILE_NAME2 + " should be staged");
        assertEquals(
                2, Objects.requireNonNull(Repository.STAGING_BLOBS.list()).length,
                "There should be 2 blobs after second add");

        // Step 5: commit with message and assert commit count
        Repository.commit(commitMsg);
        assertEquals(
                2, Objects.requireNonNull(Repository.COMMITS.list()).length,
                "There should be 2 commits after second commit");

        // Step 6: 检查 HEAD commit 是否追踪两个文件
        CommitManager manager = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
        Commit headCommit = manager.getHeadCommit();
        assertNotNull(headCommit, "Head commit should not be null");
        assertEquals(2, headCommit.getTrackedFile().size(), "Head commit should track 2 files");
        assertTrue(headCommit.getTrackedFile().containsKey(FILE_NAME1));
        assertTrue(headCommit.getTrackedFile().containsKey(FILE_NAME2));
    }

    @Test
    public void testAddDuplicateFileAndCommit() throws IOException {
        // 前置步骤
        Repository.setup();
        File file1 = Utils.join(Repository.CWD, FILE_NAME1);
        Utils.createFile(file1);
        Utils.writeContents(file1, TEXT1);
        Repository.addFile(FILE_NAME1);
        Repository.commit("commit1");

        // 1 尝试重复操作 add FILE_NAME1，检查 staging 区域，应该什么都没有
        Repository.addFile(FILE_NAME1);
        assertEquals(0, Objects.requireNonNull(Repository.ADDITION.list()).length, "Staging area should be empty for already tracked file");

        // 2 尝试 commit，应该得到输出 "No changes added to the commit."
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        Repository.commit("duplicate commit");
        System.setOut(originalOut);
        String output = outContent.toString().trim();
        assertEquals("No changes added to the commit.", output);

        // 3 尝试 add 一个不存在的文件，应该得到一个GitletException异常，报错信息应该为 "File does not exist."
        GitletException exception = assertThrows(GitletException.class, () -> Repository.addFile("not_exist.txt"));
        assertEquals("File does not exist.", exception.getMessage());
    }

    @Test
    public void testRemoveFile() throws IOException {
        Repository.setup();
        File file1 = Utils.join(Repository.CWD, FILE_NAME1);
        File file2 = Utils.join(Repository.CWD, FILE_NAME2);
        Utils.createFile(file1);
        Utils.createFile(file2);
        Utils.writeContents(file1, TEXT1);
        Utils.writeContents(file2, TEXT2);

        Repository.addFile(FILE_NAME1);
        Repository.commit("commit1");

        // 检查 commit1 追踪 FILE_NAME1
        CommitManager manager1 = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
        Commit commit1 = manager1.getHeadCommit();
        assertTrue(commit1.getTrackedFile().containsKey(FILE_NAME1));

        Repository.remove(FILE_NAME1);
        // REMOVAL 应该有 FILE_NAME1
        File removalFile1 = Utils.join(Repository.REMOVAL, FILE_NAME1);
        assertTrue(removalFile1.exists(), "REMOVAL should contain " + FILE_NAME1);

        // ADDITION 应该没有 FILE_NAME1
        File additionFile1 = Utils.join(Repository.ADDITION, FILE_NAME1);
        assertFalse(additionFile1.exists(), "ADDITION should not contain " + FILE_NAME1);

        // CWD 应该没有 FILE_NAME1
        File cwdFile1 = Utils.join(Repository.CWD, FILE_NAME1);
        assertFalse(cwdFile1.exists(), "Working directory should not contain " + FILE_NAME1);

        Repository.commit("commit2");

        // commit2 应该没有追踪任何文件
        CommitManager manager2 = Utils.readObject(Repository.COMMIT_MANAGER, CommitManager.class);
        Commit commit2 = manager2.getHeadCommit();
        assertEquals(0, commit2.getTrackedFile().size(), "Commit2 should not track any file");


        Repository.addFile(FILE_NAME2);
        File additionFile2 = Utils.join(Repository.ADDITION, FILE_NAME2);
        assertTrue(additionFile2.exists(), "ADDITION should contain " + FILE_NAME2);

        Repository.remove(FILE_NAME2);
        // ADDITION 应该没有 FILE_NAME2
        assertFalse(additionFile2.exists(), "ADDITION should not contain " + FILE_NAME2);

        // REMOVAL 应该没有 FILE_NAME2
        File removalFile2 = Utils.join(Repository.REMOVAL, FILE_NAME2);
        assertFalse(removalFile2.exists(), "REMOVAL should not contain " + FILE_NAME2);

        // CWD 应该仍然有 FILE_NAME2
        assertTrue(Utils.join(Repository.CWD, FILE_NAME2).exists(), "Working directory should still contain " + FILE_NAME2);

        // commit3: 输出应为 No changes added to the commit.
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        Repository.commit("commit3");

        System.setOut(originalOut);
        String output = outContent.toString().trim();
        assertEquals("No changes added to the commit.", output);

        // remove 不存在的文件，抛异常
        GitletException exception = assertThrows(GitletException.class, () -> Repository.remove("not_file"));
        assertEquals("No reason to remove the file.", exception.getMessage());
    }

    @Test
    public void testLog() throws IOException {
        /* 预期格式
        ===
        commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
        Date: Thu Nov 9 20:00:05 2017 -0800
        A commit message.

        ===
        commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
        Date: Thu Nov 9 17:01:33 2017 -0800
        Another commit message.

        ===
        commit e881c9575d180a215d1a636545b8fd9abfb1d2bb
        Date: Wed Dec 31 16:00:00 1969 -0800
        initial commit
         */
        Repository.setup();
        File wug = Utils.join(Repository.CWD, "wug.txt");
        File notwug = Utils.join(Repository.CWD, "notwug.txt");

        Utils.writeContents(wug, "wug content");
        Repository.addFile("wug.txt");
        Repository.commit("First commit wug.txt");

        Utils.writeContents(notwug, "not wug");
        Repository.addFile("notwug.txt");
        Repository.commit("First commit notwug.txt");

        Utils.writeContents(wug, "modified wug");
        Repository.addFile("wug.txt");
        Repository.commit("Modified wug.txt");

        // 捕获 log 输出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        Repository.log();

        System.setOut(originalOut);
        String logOutput = outContent.toString();

        assertTrue(logOutput.contains("First commit wug.txt"));
        assertTrue(logOutput.contains("First commit notwug.txt"));
        assertTrue(logOutput.contains("Modified wug.txt"));
        assertTrue(logOutput.contains("initial commit"));
        assertTrue(logOutput.contains("==="));

        // 按 === 拆分 commit 块
        String[] entries = logOutput.split("(?=^===)", -1);
        String pattern = "(?s)^===\\s*commit\\s+[a-f0-9]{40}\\s+Date: .+?\\s+.+";
        for (String entry : entries) {
            assertTrue(entry.matches(pattern), "Log entry format invalid:\n" + entry);
        }
    }

    @Test
    public void testFind() {
        Repository.setup();
        File file = Utils.join(Repository.CWD, FILE_NAME1);
        Utils.createFile(file);
        Utils.writeContents(file, TEXT1);

        // 提交1：Update
        Repository.addFile(FILE_NAME1);
        Repository.commit("Update");

        // 提交2：Fix bug
        Utils.writeContents(file, "new content");
        Repository.addFile(FILE_NAME1);
        Repository.commit("Fix bug");

        // 提交3：Update again
        Utils.writeContents(file, "third content");
        Repository.addFile(FILE_NAME1);
        Repository.commit("Update");

        // 捕获输出
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        Repository.find("Update");
        String output = out.toString().trim();
        String[] lines = output.split("\n");
        assertEquals(2, lines.length, "Should find 2 commits with message 'Update'");
        for (String line : lines) {
            assertEquals(40, line.length());
        }

        out.reset();
        Repository.find("Fix bug");
        String output2 = out.toString().trim();
        assertEquals(40, output2.length(), "Should find 1 commit with message 'Fix bug'");

        out.reset();
        Repository.find("initial commit");
        String output3 = out.toString().trim();
        assertEquals(40, output3.length(), "Should find 1 commit with message 'initial commit'");

        out.reset();
        GitletException exception = assertThrows(GitletException.class, () -> Repository.find("test"));
        System.setOut(originalOut);
        assertEquals("Found no commit with that message.", exception.getMessage());
    }

    @Test
    public void testCreateNewBranch() {
        // 进行两次提交，msg 分别为“commitA”和“commitB”
        // branch("new branch") 创建新分支
        // 提交一次“commitC”
        // checkout("main") 切换到原分支 main
        // 提交一次“commitD”
        // log输出，应该得到大概 D -> B -> A -> initial commit 四个提交的输出
        // global-log输出，应该得到总共5无序的提交输出
    }
}
