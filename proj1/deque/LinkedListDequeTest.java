package deque;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import static org.junit.Assert.*;

/** Performs some basic linked list tests. */
public class LinkedListDequeTest {

    @Test
    /** Adds a few things to the list, checking isEmpty() and size() are correct,
     * finally printing the results.
     *
     * && is the "and" operation. */
    public void addIsEmptySizeTest() {

//        System.out.println("Make sure to uncomment the lines below (and delete this print statement).");

        LinkedListDeque<String> lld1 = new LinkedListDeque<String>();

		assertTrue("A newly initialized LLDeque should be empty", lld1.isEmpty());
		lld1.addFirst("front");

		// The && operator is the same as "and" in Python.
		// It's a binary operator that returns true if both arguments true, and false otherwise.
        assertEquals(1, lld1.size());
        assertFalse("lld1 should now contain 1 item", lld1.isEmpty());

		lld1.addLast("middle");
		assertEquals(2, lld1.size());

		lld1.addLast("back");
		assertEquals(3, lld1.size());

		System.out.println("Printing out deque: ");
		lld1.printDeque();

    }

    @Test
    /** Adds an item, then removes an item, and ensures that dll is empty afterwards. */
    public void addRemoveTest() {

//        System.out.println("Make sure to uncomment the lines below (and delete this print statement).");

        LinkedListDeque<Integer> lld1 = new LinkedListDeque<Integer>();
		// should be empty
		assertTrue("lld1 should be empty upon initialization", lld1.isEmpty());

		lld1.addFirst(10);
		// should not be empty
		assertFalse("lld1 should contain 1 item", lld1.isEmpty());

		lld1.removeFirst();
		// should be empty
		assertTrue("lld1 should be empty after removal", lld1.isEmpty());

    }

    @Test
    /* Tests removing from an empty deque */
    public void removeEmptyTest() {

//        System.out.println("Make sure to uncomment the lines below (and delete this print statement).");

        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();
        lld1.addFirst(3);

        lld1.removeLast();
        lld1.removeFirst();
        lld1.removeLast();
        lld1.removeFirst();

        int size = lld1.size();
        String errorMsg = "  Bad size returned when removing from empty deque.\n";
        errorMsg += "  student size() returned " + size + "\n";
        errorMsg += "  actual size() returned 0\n";

        assertEquals(errorMsg, 0, size);

    }

    @Test
    /* Check if you can create LinkedListDeques with different parameterized types*/
    public void multipleParamTest() {


        LinkedListDeque<String>  lld1 = new LinkedListDeque<String>();
        LinkedListDeque<Double>  lld2 = new LinkedListDeque<Double>();
        LinkedListDeque<Boolean> lld3 = new LinkedListDeque<Boolean>();

        lld1.addFirst("string");
        lld2.addFirst(3.14159);
        lld3.addFirst(true);

        String s = lld1.removeFirst();
        double d = lld2.removeFirst();
        boolean b = lld3.removeFirst();

    }

    @Test
    /* check if null is return when removing from an empty LinkedListDeque. */
    public void emptyNullReturnTest() {

//        System.out.println("Make sure to uncomment the lines below (and delete this print statement).");

        LinkedListDeque<Integer> lld1 = new LinkedListDeque<Integer>();

        boolean passed1 = false;
        boolean passed2 = false;
        assertEquals("Should return null when removeFirst is called on an empty Deque,", null, lld1.removeFirst());
        assertEquals("Should return null when removeLast is called on an empty Deque,", null, lld1.removeLast());


    }

    @Test
    /* Add large number of elements to deque; check if order is correct. */
    public void bigLLDequeTest() {

//        System.out.println("Make sure to uncomment the lines below (and delete this print statement).");

        LinkedListDeque<Integer> lld1 = new LinkedListDeque<Integer>();
        for (int i = 0; i < 1000000; i++) {
            lld1.addLast(i);
        }

        for (double i = 0; i < 500000; i++) {
            assertEquals("Should have the same value", i, (double) lld1.removeFirst(), 0.0);
        }

        for (double i = 999999; i > 500000; i--) {
            assertEquals("Should have the same value", i, (double) lld1.removeLast(), 0.0);
        }


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
        LinkedListDeque<Integer> deque = new LinkedListDeque<>();
        for (int i = 0; i < 10; i++) {
            deque.addLast(i);
        }
        deque.printDeque();

        String expectedOutput = "0 1 2 3 4 5 6 7 8 9" + System.lineSeparator();
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    public void testStringDeque() {
        LinkedListDeque<String> deque = new LinkedListDeque<>();
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
        LinkedListDeque<Integer> deque = new LinkedListDeque<>();
        deque.printDeque();
        assertEquals(System.lineSeparator(), outContent.toString());
    }

    @Test
    public void testIterator() {
        LinkedListDeque<Integer> deque = new LinkedListDeque<>();
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
        LinkedListDeque<Integer> deque = new LinkedListDeque<>();
        Iterator<Integer> iter = deque.iterator();
        assertFalse(iter.hasNext()); // 空队列应该没有元素
    }

    @Test
    public void testIteratorAfterOperations() {
        LinkedListDeque<String> deque = new LinkedListDeque<>();
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
        LinkedListDeque<Integer> deque = new LinkedListDeque<>();
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
        LinkedListDeque<Integer> deque1 = new LinkedListDeque<>();
        LinkedListDeque<Integer> deque2 = new LinkedListDeque<>();

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
        for (int i = 0; i < 1000000; i++) {
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
        assertTrue(result);
        System.out.println("equals() for two " + dataSize + " element deques took " + duration + " ns");
        // 可选：断言执行时间在可接受范围内（例如 100 毫秒 = 10,000,000 ns）
        long acceptableThreshold = 100_000_000;
        assertTrue("equals() method performance is too slow", duration < acceptableThreshold);
    }

    @Test
    public void testCrossTypeEqualsPerformance() {
        int dataSize = 1000000; // 测试数据量，可根据需要调整
        LinkedListDeque<Integer> deque1 = new LinkedListDeque<>();
        ArrayDeque<Integer> deque2 = new ArrayDeque<>();
        for (int i = 0; i < dataSize; i++) {
            deque1.addLast(i);
            deque2.addLast(i);
        }
        long startTime = System.nanoTime();
        boolean result = deque1.equals(deque2);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        assertTrue(result);
        System.out.println("equals() for two " + dataSize + " element deques took " + duration + " ns");
        // 可选：断言执行时间在可接受范围内（例如 100 毫秒 = 10,000,000 ns）
        long acceptableThreshold = 100_000_000;
        assertTrue("equals() method performance is too slow", duration < acceptableThreshold);
    }
}
