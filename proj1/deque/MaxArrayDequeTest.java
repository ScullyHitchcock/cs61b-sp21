package deque;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Comparator;

public class MaxArrayDequeTest {

    @Test
    public void testMaxNaturalOrder() {
        // Comparator for natural order of integers
        Comparator<Integer> naturalOrder = new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return a - b;
            }
        };

        MaxArrayDeque<Integer> deque = new MaxArrayDeque<>(naturalOrder);
        deque.addLast(3);
        deque.addLast(1);
        deque.addLast(4);
        deque.addLast(1);
        deque.addLast(5);
        deque.addLast(9);

        // The maximum element should be 9
        assertEquals((Integer) 9, deque.max());
    }

    @Test
    public void testMaxWithReverseComparator() {
        // Reverse comparator: returns negative of natural order
        Comparator<Integer> reverseOrder = new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return b - a;
            }
        };

        MaxArrayDeque<Integer> deque = new MaxArrayDeque<>(reverseOrder);
        deque.addLast(3);
        deque.addLast(1);
        deque.addLast(4);
        deque.addLast(1);
        deque.addLast(5);
        deque.addLast(9);

        // Using the reverse comparator, the "maximum" should be the smallest value, i.e. 1
        assertEquals((Integer) 1, deque.max(reverseOrder));
    }

    @Test
    public void testStringMaxByLength() {
        // Comparator comparing strings by their length
        Comparator<String> lengthComparator = new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.length() - b.length();
            }
        };

        MaxArrayDeque<String> deque = new MaxArrayDeque<>(lengthComparator);
        deque.addLast("apple");
        deque.addLast("banana");
        deque.addLast("kiwi");
        deque.addLast("watermelon");

        // The longest string should be "watermelon"
        assertEquals("watermelon", deque.max());
    }

    @Test
    public void testStringMaxByAlphabetical() {
        // Base comparator (by length) for construction
        Comparator<String> lengthComparator = new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.length() - b.length();
            }
        };

        // Comparator for alphabetical order
        Comparator<String> alphabeticalComparator = new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareTo(b);
            }
        };

        MaxArrayDeque<String> deque = new MaxArrayDeque<>(lengthComparator);
        deque.addLast("apple");
        deque.addLast("banana");
        deque.addLast("kiwi");
        deque.addLast("watermelon");

        // Alphabetically, "watermelon" is the largest
        assertEquals("watermelon", deque.max(alphabeticalComparator));
    }

    @Test
    public void testEmptyDeque() {
        // Test that max() returns null for an empty deque
        Comparator<Integer> naturalOrder = new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return a - b;
            }
        };

        MaxArrayDeque<Integer> deque = new MaxArrayDeque<>(naturalOrder);
        assertNull(deque.max());
    }

    @Test
    public void testStringLengthComparatorWithMultipleStrings() {
        // Comparator comparing strings by length
        Comparator<String> lengthComparator = new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.length() - b.length();
            }
        };

        MaxArrayDeque<String> deque = new MaxArrayDeque<>(lengthComparator);
        // Add strings with increasing lengths
        deque.addLast("a");      // length 1
        deque.addLast("ab");     // length 2
        deque.addLast("abc");    // length 3
        deque.addLast("abcd");   // length 4
        deque.addLast("abcde");  // length 5

        // The longest string should be "abcde"
        assertEquals("abcde", deque.max());
    }

}