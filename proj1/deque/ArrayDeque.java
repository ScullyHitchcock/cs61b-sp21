package deque;

public class ArrayDeque<T> {
    private T[] items;
    private int size;
    private int start;
    private int end;

    public ArrayDeque() {
        items = (T []) new Object[8];
        size = 0;
        start = 3;
        end = 4;
    }

    public int prevIndex(int index) {
        if (index - 1 < 0) {
            return items.length - 1;
        }
        return index - 1;
    }

    public int nextIndex(int index) {
        if (index + 1 >= items.length) {
            return 0;
        }
        return index + 1;
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
        T[] a = (T[]) new Object[capacity];
        int zeroIndex = nextIndex(start);
        int endIndex = prevIndex(end);
        if (zeroIndex <= endIndex) {
            System.arraycopy(items, zeroIndex, a, 0, size);
        } else {
            int numOfFirstPart = size - zeroIndex;
            int numOfRest = size - numOfFirstPart;
            System.arraycopy(items, zeroIndex, a, 0, numOfFirstPart);
            System.arraycopy(items, 0, a, numOfFirstPart, numOfRest);
        }
        items = a;
        start = prevIndex(0);
        end = nextIndex(size - 1);
    }

    /** Returns true if deque is empty, false otherwise. */
    public boolean isEmpty() {
        return size() == 0;
    }

    /** Returns the number of items in the deque. */
    public int size() {
        return size;
    }

    /** Prints the items in the deque from first to last, separated by a space.
     * Once all the items have been printed, print out a new line. */
    public void printDeque() {
        for (int i = 0; i < size; i ++) {
            T item = items[realIndex(i)];
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
        if (items[nextIndex(start)] == null) { return null; }
        T first = items[nextIndex(start)];
        start = nextIndex(start);
        items[start] = null;
        size -= 1;
        if (size < items.length / 4 && size > 8) {
            resize(size);
        }
        return first;
    }

    /** Removes and returns the item at the back of the deque.
     * If no such item exists, returns null. */
    public T removeLast() {
        if (items[prevIndex(end)] == null) { return null; }
        T last = items[prevIndex(end)];
        end = prevIndex(end);
        items[end] = null;
        size -= 1;
        if (size < items.length / 4 && size > 8) {
            resize(size);
        }
        return last;
    }

    /** Gets the item at the given index, where 0 is the front, 1 is the next item, and so forth.
     * If no such item exists, returns null. */
    public T get(int index) {
        return items[realIndex(index)];
    }
    public int realIndex(int index) {
        int zeroIndex = nextIndex(start);
        int endIndex = prevIndex(end);
        int realIndex = zeroIndex + index;
        if (zeroIndex > endIndex) {
            if (realIndex >= items.length) {
                realIndex -= items.length;
            }
        }
        return realIndex;
    }
}
