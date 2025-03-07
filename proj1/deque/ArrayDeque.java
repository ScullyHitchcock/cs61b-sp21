package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {
    private static final int MIN_CAPACITY = 8;
    private T[] items;
    private int size;
    private int start;
    private int end;
    private class AListIterator implements Iterator<T> {
        private int curPos;
        @Override
        public boolean hasNext() {
            return curPos < size;
        }
        @Override
        public T next() {
            T item = get(curPos);
            curPos += 1;
            return item;
        }
    }

    public ArrayDeque() {
        items = (T[]) new Object[MIN_CAPACITY];
        size = 0;
        start = 3;
        end = 4;
    }

    /** Returns the previous index of the current index. */
    private int prevIndex(int index) {
        if (index - 1 < 0) {
            return items.length - 1;
        }
        return index - 1;
    }

    /** Returns the next index from the current index. */
    private int nextIndex(int index) {
        if (index + 1 >= items.length) {
            return 0;
        }
        return index + 1;
    }

    /** Gets the item at the given index, where 0 is the front, 1 is the next item, and so forth.
     * If no such item exists, returns null. */
    @Override
    public T get(int num) {
        if (num >= size || num < 0) { return null; }
        int index = (start + 1 + num) % items.length;
        return items[index];
    }

    @Override
    public void addFirst(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[start] = item;
        start = prevIndex(start);
        size += 1;
    }

    @Override
    public void addLast(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[end] = item;
        end = nextIndex(end);
        size += 1;
    }

    /** Resizes the underlying array to the target capacity. */
    private void resize(int capacity) {
        T[] newItems = (T[]) new Object[capacity];

        /* Solution 1 */
//        for (int i = 0; i < size; i++) {
//            newItems[i] = get(i);
//        }
//        items = newItems;
//        start = capacity - 1;
//        end = size;

        /* Solution 2 */
        int first = nextIndex(start);
        int firstPartLength = Math.min(size, items.length - first);
        int secondPartLength = size - firstPartLength;
        System.arraycopy(items, first, newItems, 0, firstPartLength);
        System.arraycopy(items, 0, newItems, firstPartLength, secondPartLength);
        items = newItems;
        start = capacity - 1;
        end = size;
    }

    /** Returns the number of items in the deque. */
    @Override
    public int size() {
        return size;
    }

    /** Prints the items in the deque from first to last, separated by a space.
     * Once all the items have been printed, print out a new line. */
    @Override
    public void printDeque() {
        for (int i = 0; i < size; i ++) {
            T item = get(i);
            if (i == size - 1) {
                System.out.print(item);
            } else {
                System.out.print(item + " ");
            }
        }
        System.out.println();
    }

    /** Removes and returns the item at the front of the deque.
     * If no such item exists, returns null. */
    @Override
    public T removeFirst() {
        if (this.isEmpty()) { return null; }
        T first = items[nextIndex(start)];
        start = nextIndex(start);
        items[start] = null;
        size -= 1;
        if (size < items.length / 4 && size > MIN_CAPACITY) {
            resize(size);
        }
        return first;
    }

    /** Removes and returns the item at the back of the deque.
     * If no such item exists, returns null. */
    @Override
    public T removeLast() {
        if (this.isEmpty()) { return null; }
        T last = items[prevIndex(end)];
        end = prevIndex(end);
        items[end] = null;
        size -= 1;
        if (size < items.length / 4 && size > MIN_CAPACITY) {
            resize(size);
        }
        return last;
    }

    @Override
    public Iterator<T> iterator() { return new AListIterator(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ArrayDeque)) { return false; }

        ArrayDeque<T> o = (ArrayDeque<T>) other;
        if (this.size != o.size) { return false; }

        Iterator<T> iterThis = this.iterator();
        Iterator<T> iterOther = o.iterator();

        while (iterThis.hasNext() && iterOther.hasNext()) {
            T thisItem = iterThis.next();
            T otherItem = iterOther.next();
            if (!thisItem.equals(otherItem)) { return false;}
        }
        return true;
    }
}
