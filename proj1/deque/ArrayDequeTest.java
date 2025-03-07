package deque;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import org.junit.After;
import org.junit.Before;


public class ArrayDequeTest {
    @Test
    public void testAddLast() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        // Test: 0 1 2 3 4 5 6 7
        for (int i = 0; i < 8; i++) {
            l.addLast(i);
        }
        assertEquals(8, l.size());
        assertEquals(5, (int) l.get(5));
        assertEquals(6, (int) l.get(6));
    }

    @Test
    public void testAddFirst() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        // 0 1 2 3 4 5 6 7
        for (int i = 7; i >= 0; i--) {
            l.addFirst(i);
        }
        assertEquals(8, l.size());
        assertEquals(5, (int) l.get(5));
        assertEquals(6, (int) l.get(6));
    }

    @Test
    public void testRemoveLast() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        // 0 1 ... 7
        for (int i = 0; i < 8; i++) {
            l.addLast(i);
        }
        for (int i = 7; i >= 0; i--) {
            assertEquals(i, (int) l.removeLast());
        }
        assertNull(l.removeFirst());
        assertNull(l.removeLast());
        assertTrue(l.isEmpty());
    }

    @Test
    public void testRemoveFirst() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        // 0 1 ... 7
        for (int i = 7; i >= 0; i--) {
            l.addFirst(i);
        }
        for (int i = 0; i < 8; i++) {
            int res = l.removeFirst();
            assertEquals(i, res);
        }
        assertTrue(l.isEmpty());
    }

    @Test
    public void testRemoveFromEmptyDeque() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        assertNull(deque.removeFirst());
        assertNull(deque.removeLast());
    }

    @Test
    public void testIsEmptyAndSize() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());

        deque.addLast(1);
        assertFalse(deque.isEmpty());
        assertEquals(1, deque.size());

        deque.removeFirst();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    @Test
    public void testExpandResize() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        // 0 1 2 3 4 5 6 7
        for (int i = 0; i < 8; i++) {
            l.addLast(i);
        }
        assertEquals(1, (int) l.get(1));
        assertEquals(5, (int) l.get(5));
        assertEquals(0, (int) l.get(0));
        assertNull(l.get(-1));
        assertNull(l.get(10));
        // 0 1 2 3 4 5... 14 15
        for (int i = 8; i < 16; i++) {
            l.addLast(i);
        }
        assertEquals(10, (int) l.get(10));
        assertEquals(13, (int) l.get(13));
        assertEquals(0, (int) l.get(0));
        assertEquals(16, l.size());
    }

    @Test
    public void testShortenResize() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        // 0 1 2 ... 61 62 63
        for (int i = 63; i >= 0; i--) {
            l.addFirst(i);
        }
        assertNull(l.get(64));
        assertEquals(43, (int) l.get(43));
        assertEquals(13, (int) l.get(13));
        assertEquals(61, (int) l.get(61));
        assertEquals(1, (int) l.get(1));
        for (int i = 0; i < 32; i++) {
            l.removeLast();
        }
        assertEquals(32, l.size());
        assertNull(l.get(63));
        for (int i = 0; i < 20; i++) {
            l.removeLast();
        }
        assertEquals(12, l.size());
        assertEquals(0, (int) l.get(0));
        assertEquals(3, (int) l.get(3));
        assertEquals(10, (int) l.get(10));
        assertEquals(11, (int) l.get(11));
    }

    @Test
    public void testGetOutOfBounds() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addLast(10);
        assertNull(deque.get(-1)); // 负索引
        assertNull(deque.get(1));  // 超出索引
    }

    @Test
    public void integratedTest() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        for (int i = 0; i < 64; i++) {
            l.addLast(i);
        }
        for (int i = 0; i < 25; i++) {
            l.removeLast();
            l.removeFirst();
        }
        assertEquals(14, l.size());
        assertEquals(25, (int) l.get(0));
        assertEquals(38, (int) l.get(13));
        assertEquals(30, (int) l.get(5));
        assertEquals(34, (int) l.get(9));
    }

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testPrintDeque() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        for (int i = 0; i < 10; i++) {
            deque.addLast(i);
        }
        deque.printDeque();

        String expectedOutput = "0 1 2 3 4 5 6 7 8 9" + System.lineSeparator();
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    public void testStringDeque() {
        ArrayDeque<String> deque = new ArrayDeque<>();
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");
        deque.addLast("d");
        deque.addLast("e");
        deque.addLast("f");

        deque.printDeque();
        String expectedOutput = "a b c d e f" + System.lineSeparator();
        assertEquals(expectedOutput, outContent.toString());

        assertEquals(6, deque.size());
        assertEquals("a", deque.get(0));
        assertEquals("f", deque.get(5));

        deque.addLast("g");
        deque.addLast("h");
        deque.addLast("i");
        deque.addLast("j");

        assertEquals(10, deque.size());
        assertEquals("g", deque.get(6));
        assertEquals("h", deque.get(7));
        assertEquals("j", deque.get(9));
    }

    @Test
    public void testPrintEmptyDeque() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.printDeque();
        assertEquals(System.lineSeparator(), outContent.toString());
    }

    @Test
    public void testIterator() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        deque.addLast(1);
        deque.addLast(2);
        deque.addLast(3);
        deque.addLast(4);

        Iterator<Integer> iter = deque.iterator();
        int[] expected = {1, 2, 3, 4};
        int index = 0;

        while (iter.hasNext()) {
            assertEquals((int) iter.next(), expected[index]);
            index++;
        }

        assertEquals(4, index); // 确保所有元素都被遍历
    }

    @Test
    public void testEmptyIterator() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        Iterator<Integer> iter = deque.iterator();
        assertFalse(iter.hasNext()); // 空队列应该没有元素
    }

    @Test
    public void testIteratorAfterOperations() {
        ArrayDeque<String> deque = new ArrayDeque<>();
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");
        deque.removeFirst(); // 删除 "A"

        Iterator<String> iter = deque.iterator();
        String[] expected = {"B", "C"};
        int index = 0;

        while (iter.hasNext()) {
            assertEquals(expected[index], iter.next());
            index++;
        }

        assertEquals(2, index); // 确保正确遍历所有元素
    }

    @Test
    public void testForEachLoop() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        for (int i = 0; i < 5; i++) {
            deque.addLast(i);
        }
        // Iterate over the deque and print each item
        for (int item : deque) {
            System.out.print(item);
        }
        assertEquals("01234", outContent.toString());
        System.out.println();
    }

    @Test
    public void testEquals() {
        ArrayDeque<Integer> deque1 = new ArrayDeque<>();
        ArrayDeque<Integer> deque2 = new ArrayDeque<>();

        assertTrue(deque1.equals(deque2)); // 空队列相等

        deque1.addLast(1);
        assertFalse(deque1.equals(deque2)); // 一个非空，一个空

        deque2.addLast(1);
        assertTrue(deque1.equals(deque2)); // 两者相等

        deque1.addFirst(2);
        deque2.addFirst(2);
        assertTrue(deque1.equals(deque2)); // 继续测试相等情况

        deque1.addLast(3);
        assertFalse(deque1.equals(deque2)); // 一个有 3，另一个没有
    }

    @Test
    public void testBigDequesEquals() {
        LinkedListDeque<Integer> deque1 = new LinkedListDeque<>();
        LinkedListDeque<Integer> deque2 = new LinkedListDeque<>();
        for (int i = 0; i < 100000; i++) {
            deque1.addLast(i);
            deque2.addLast(i);
        }
        assertTrue(deque1.equals(deque2));
        deque1.removeLast();
        assertFalse(deque1.equals(deque2));
        deque2.removeLast();
        assertTrue(deque1.equals(deque2));
        deque1.addFirst(114514);
        assertFalse(deque1.equals(deque2));
        deque2.addFirst(114514);
        assertTrue(deque1.equals(deque2));
    }

    @Test
    public void testEqualsPerformance() {
        int dataSize = 1000000; // 测试数据量，可根据需要调整
        LinkedListDeque<Integer> deque1 = new LinkedListDeque<>();
        LinkedListDeque<Integer> deque2 = new LinkedListDeque<>();
        for (int i = 0; i < dataSize; i++) {
            deque1.addLast(i);
            deque2.addLast(i);
        }
        long startTime = System.nanoTime();
        boolean result = deque1.equals(deque2);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("equals() for two " + dataSize + " element deques took " + duration + " ns");
        assertTrue(result);
        // 可选：断言执行时间在可接受范围内（例如 100 毫秒 = 100,000,000 ns）
        long acceptableThreshold = 100_000_000;
        assertTrue("equals() method performance is too slow", duration < acceptableThreshold);
    }

    @Test
    public void testCrossTypeEquals() {
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();
        LinkedListDeque<Integer> linkedListDeque = new LinkedListDeque<>();

        // Both deques are empty
        assertTrue(arrayDeque.equals(linkedListDeque));
        assertTrue(linkedListDeque.equals(arrayDeque));

        // Add identical elements to both
        for (int i = 0; i < 10; i++) {
            arrayDeque.addLast(i);
            linkedListDeque.addLast(i);
        }
        assertTrue(arrayDeque.equals(linkedListDeque));
        assertTrue(linkedListDeque.equals(arrayDeque));

        // Modify one deque and verify inequality
        arrayDeque.addLast(10);
        assertFalse(arrayDeque.equals(linkedListDeque));
        assertFalse(linkedListDeque.equals(arrayDeque));
    }
}

