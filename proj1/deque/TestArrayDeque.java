package deque;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestArrayDeque {
    @Test
    public void testWithoutResizing() {
        int initSize = 8;
        ArrayDeque<Integer> l = new ArrayDeque<>();
        for (int i = 0; i < initSize; i++) {
            l.addLast(i);
        }
        l.removeLast();
        l.removeFirst();
        int res1 = l.removeFirst(); // Should be 1.
        int res2 = l.removeLast(); // Should be 6.
        assertEquals(1, res1);
        assertEquals(6, res2);
        assertEquals(4, l.size());
    }

    @Test
    public void testExpandResizing() {
        int initSize = 9;
        ArrayDeque<Integer> l = new ArrayDeque<>();
        for (int i = initSize - 1; i >= 0; i--) {
            l.addFirst(i);
        }
        for (int i = initSize; i < initSize * 2; i++) {
            l.addLast(i);
        }
        assertEquals(l.size(), 18);
        for (int i = 0; i < initSize; i++) {
            l.removeFirst();
            l.removeLast();
        }
        assertTrue(l.isEmpty());
    }

    @Test
    public void testShortenResizing() {
        int initSize = 20;
        ArrayDeque<Integer> l = new ArrayDeque<>();
        for (int i = initSize - 1; i >= 0; i--) {
            l.addFirst(i);
        }
        for (int i = initSize; i < initSize * 2; i++) {
            l.addLast(i);
        }
        l.printDeque();
        for (int i = 0; i < initSize; i++) {
            l.removeLast();
            l.removeFirst();
        }
        System.out.println();
    }

    @Test
    public void testGet1() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        for (int i = 0; i < 10; i++) {
            l.addLast(i);
        }
        int res1 = l.get(0); // 0
        int res2 = l.get(1); // 1
        int res3 = l.get(9); // 9
        assertEquals(0, res1);
        assertEquals(1, res2);
        assertEquals(9, res3);
    }

    @Test
    public void testGet2() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        for (int i = 20; i >= 0; i--) {
            l.addFirst(i);
        }
        int res1 = l.get(0); // Should be 0.
        int res2 = l.get(5); // Should be 5.
        int res3 = l.get(15); // Should be 15.
        int res4 = l.get(20); // Should be 20.
        assertEquals(0, res1);
        assertEquals(5, res2);
        assertEquals(15, res3);
        assertEquals(20, res4);
    }

    @Test
    public void testPrintDeque() {
        ArrayDeque<Integer> l = new ArrayDeque<>();
        for (int i = 20; i >= 0; i--) {
            l.addFirst(i);
        }
        l.printDeque();
    }
}
