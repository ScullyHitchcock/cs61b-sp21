package deque;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Before;


public class TestArrayDeque {
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
}

