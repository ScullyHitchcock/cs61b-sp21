package deque;

public class ArrayDeque<T> {
    private static final int MIN_CAPACITY = 8;
    private T[] items;
    private int size;
    private int start;
    private int end;

    public ArrayDeque() {
        items = (T []) new Object[MIN_CAPACITY];
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
    public T get(int num) {
        if (num >= size || num < 0) { return null; }
        int index = (start + 1 + num) % items.length;
        return items[index];
    }

    public void addFirst(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[start] = item;
        start = prevIndex(start);
        size += 1;
    }

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

    /** Returns true if deque is empty, false otherwise. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Returns the number of items in the deque. */
    public int size() {
        return size;
    }

    /** Prints the items in the deque from first to last, separated by a space.
     * Once all the items have been printed, print out a new line. */
    public void printDeque() {
        for (int i = 0; i < size; i ++) {
            T item = get(i);
            if (i == size - 1) {
                System.out.println(item);
            } else {
                System.out.print(item + " ");
            }
        }
    }

    /** Removes and returns the item at the front of the deque.
     * If no such item exists, returns null. */
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
}
