package deque;

import java.util.Comparator;

/** 带比较器的ArrayDeque */
public class MaxArrayDeque<T> extends ArrayDeque<T> {

    /* 比较器 */
    private Comparator<T> comparator;

    public MaxArrayDeque(Comparator<T> c) {
        comparator = c;
    }

    /* 利用实例内部自带的比较器，使用其比较逻辑求出队列中最大的元素 */
    public T max() {
        if (isEmpty()) {
            return null;
        }
        T maxItem = get(0);
        for (T item: this) {
            if (comparator.compare(item, maxItem) > 0) {
                maxItem = item;
            }
        }
        return maxItem;
    }

    /* 利用实例外部传入的比较器，使用其比较逻辑求出队列中最大的元素 */
    public T max(Comparator<T> c) {
        if (isEmpty()) {
            return null;
        }
        T maxItem = get(0);
        for (T item: this) {
            if (c.compare(item, maxItem) > 0) {
                maxItem = item;
            }
        }
        return maxItem;
    }
}
