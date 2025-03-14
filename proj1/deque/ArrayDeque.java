package deque;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {
    /* 初始数组中的长度，设置为8 */
    private static final int MIN_CAPACITY = 8;

    /* 初始数组 */
    private T[] items;

    /* Deque的长度 */
    private int size;

    /* Deque队列首位元素的前一个索引的指针。
    假设一个ArrayDeque的长度为10，在其items[3]到items[6]中有4个元素，
    首位元素是items[3]，那么start就是 3 - 1 = 2。
    假设首位元素是items[0]，那么start就是9，因为0的前面到了items的末端。
    示意图：[1, 2, 3, end, null, null, null, null, null, start]
     */
    private int start;

    /* Deque队列首位元素的后一个索引的指针。
    假设一个ArrayDeque的长度为10，在其items[3]到items[6]中有4个元素，
    首位元素是items[6]，那么start就是 6 + 1 = 7。
    假设首位元素是items[9]，那么start就是0，因为9的后面到了items的前端。
    示意图：[end, null, null, null, null, start, 1, 2, 3, 4]
     */
    private int end;

    /* 为了实现ArrayDeque作为一个可迭代对象，所必须实现的内部类：迭代器。 */
    private class AListIterator implements Iterator<T> {

        /* 指向当前迭代器处理元素的指针，默认为0 */
        private int curPos;

        /* 如果指针到达末端，返回false，否则true。 */
        @Override
        public boolean hasNext() {
            return curPos < size;
        }

        /* 取出当前指针指向下标的元素，然后移动指针，返回该元素。 */
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T item = get(curPos);
            curPos += 1;
            return item;
        }
    }

    /* 构造函数：以默认长度8创建items数组，初始化size，以及start、end位置。 */
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

        /* 如果items长度不足，则增长items的长度 */
        if (size == items.length) {
            resize(size * 2);
        }
        items[start] = item;
        start = prevIndex(start);
        size += 1;
    }

    @Override
    public void addLast(T item) {

        /* 如果items长度不足，则增长items的长度 */
        if (size == items.length) {
            resize(size * 2);
        }
        items[end] = item;
        end = nextIndex(end);
        size += 1;
    }

    /** Resizes the underlying array to the target capacity. */
    private void resize(int capacity) {
        /* 以传入的capacity为长度创建新数组 */
        T[] newItems = (T[]) new Object[capacity];

        /* Solution 1 简单遍历原数组提取元素放入新数组，优点：可读性较高。缺点：性能较差*/
//        for (int i = 0; i < size; i++) {
//            newItems[i] = get(i);
//        }
//        items = newItems;
//        start = capacity - 1;
//        end = size;



        /* Solution 2 使用 System.arraycopy 方法分两段复制数组，优点：性能较快。缺点：可读性较低。
        * 当需要调用resize时存在两种情况：
        * 1，原数组是连贯的，即队列首位元素 x 在最前面，末位元素 y 在最后面；
        * 2，原数组是不连贯的，即队列首位元素 x 在数组中间，末位元素在 x 前面；
        * 操作步骤：
        * 1，找出首位元素所在下标 first
        * 2，将items的元素进行分段处理：
        *    a，首先计算从首位元素到数组末端的元素之间的个数（注意是数组末端，不是队列末位元素），作为第一段元素，
                Math.min(size, items.length - first)解释：
                如果size比items.length - first小，说明当前队列的items是连续的，
                如果反过来，说明当前队列的items是不连续的。
                二者的最小值就是第一段元素的长度。
             b，计算剩余元素的个数（如果第一段元素的长度已经是size，那么第二段长度为0）。
        * 3，复制第一段元素。
        * 4，复制第二段元素（如果第一段元素已经把全部元素复制完成，这一步将空过）。
        * 5，将新数组赋值为items
        * 6，重新设置start，end指针
         */
        int first = nextIndex(start);
        int firstPartLength = Math.min(size, items.length - first); // 第一段长度：首位元素到数组末端的长度
        int secondPartLength = size - firstPartLength; // 第二段长度：size - 第一段长度
        System.arraycopy(items, first, newItems, 0, firstPartLength); // 复制第一段
        System.arraycopy(items, 0, newItems, firstPartLength, secondPartLength); // 复制第二段
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

        /* 如果size小于数组长度的1/4，则缩短items的长度。 */
        if (size < items.length / 4 && size > MIN_CAPACITY) {
            resize(size);
        }
        return first;
    }

    /** Removes and returns the item at the back of the deque.
     * If no such item exists, returns null. */
    @Override
    public T removeLast() {
        if (this.isEmpty()) {
            return null;
        }
        T last = items[prevIndex(end)];
        end = prevIndex(end);
        items[end] = null;
        size -= 1;

        /* 如果size小于数组长度的1/4，则缩短items的长度。 */
        if (size < items.length / 4 && size > MIN_CAPACITY) {
            resize(size);
        }
        return last;
    }

    /** 创建迭代器 */
    @Override
    public Iterator<T> iterator() {
        return new AListIterator();
    }

    /** 比较两个Deque是否相等 */
    @Override
    public boolean equals(Object other) {
        /* 如果相等直接返回true */
        if (this == other) {
            return true;
        }

        /* 如果不是同类型直接返回false */
        if (!(other instanceof Iterable) || !(other instanceof Deque)) {
            return false;
        }

        /* 如果两者size不一样直接返回false */
        Deque<T> o = (Deque<T>) other;
        if (this.size() != o.size()) {
            return false;
        }

        /* 两者创建各自的迭代器开始迭代比较 */
        Iterable<T> otherDeque = (Iterable<T>) other;
        Iterator<T> iterThis = this.iterator();
        Iterator<T> iterOther = otherDeque.iterator();
        while (iterThis.hasNext() && iterOther.hasNext()) {
            T thisItem = iterThis.next();
            T otherItem = iterOther.next();
            if (!thisItem.equals(otherItem)) {
                return false;
            }
        }
        return true;
    }
}
