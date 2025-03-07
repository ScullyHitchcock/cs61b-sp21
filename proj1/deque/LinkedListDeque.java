package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {
    private int size;
    private Node sentinel;
    private class Node {
        public T item;
        public Node next;
        public Node prev;
        public Node(T i, Node p, Node n) {
            item = i;
            next = n;
            prev = p;
        }
    }
    private class DLListIterator implements Iterator<T> {
        private Node curNode;
        public DLListIterator() {
            curNode = sentinel.next;
        }
        @Override
        public boolean hasNext() {
            return curNode != sentinel;
        }
        @Override
        public T next() {
            T item = curNode.item;
            curNode = curNode.next;
            return item;
        }
    }

    public LinkedListDeque() {
        sentinel = new Node(null, null, null);
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        size = 0;
    }

    /** Adds a new Node behind the sentinel.
     * ... <--> sentinel <--> originalNextNode <--> ...
     * ... <--> sentinel <--> newItem <--> originalNextNode <--> ...
     * */
    @Override
    public void addFirst(T item) {
        Node originalNextNode = sentinel.next;

        // Create a new Node,
        // whose prev is sentinel and next is the original sentinel.next.
        Node newItem = new Node(item, sentinel, originalNextNode);

        // Relink.
        sentinel.next = newItem;
        originalNextNode.prev = newItem;
        size += 1;
    }

    /** Adds a new Node in front of the sentinel.
     * ... <--> originalPrevNode <--> sentinel <--> ...
     * ... <--> originalPrevNode <--> newItem <--> sentinel <--> ...
     * */
    @Override
    public void addLast(T item) {
        Node originalPrevNode = sentinel.prev;

        // Create a new Node,
        // whose prev is the original sentinel.next and next is sentinel.
        Node newItem = new Node(item, originalPrevNode, sentinel);

        // Relink.
        sentinel.prev = newItem;
        originalPrevNode.next = newItem;
        size += 1;
    }

    /** Returns true if deque is empty, false otherwise. */
    @Override
    public boolean isEmpty() {
        return size == 0;
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
        Node node = sentinel.next;
        while (node != sentinel) {
            if (node.next == sentinel) {
                System.out.print(node.item);
            } else {
                System.out.print(node.item + " ");
            }
            node = node.next;
        }
        System.out.println();
    }

    /** Removes and returns the item at the front of the deque.
     * If no such item exists, returns null. */
    @Override
    public T removeFirst() {
        Node nodeToRemove = sentinel.next;
        if (this.isEmpty()) { return null; }
        Node secendNextNode = sentinel.next.next;
        sentinel.next = secendNextNode;
        secendNextNode.prev = sentinel;
        size -= 1;
        return nodeToRemove.item;
    }

    /** Removes and returns the item at the back of the deque.
     * If no such item exists, returns null. */
    @Override
    public T removeLast() {
        Node nodeToRemove = sentinel.prev;
        if (this.isEmpty()) { return null; }
        Node secendLastNode = sentinel.prev.prev;
        sentinel.prev = secendLastNode;
        secendLastNode.next = sentinel;
        size -= 1;
        return nodeToRemove.item;
    }

    /** Gets the item at the given index, where 0 is the front, 1 is the next item, and so forth.
     * If no such item exists, returns null. */
    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        Node curr; // Starting pointer
        int steps; // The number of steps the pointer needs to move
        boolean forward; // The flag that determines whether the pointer goes forward or backward.

        if (index < size / 2) { // Forward
            curr = sentinel.next;
            steps = index;
            forward = true;
        } else { // Backward
            curr = sentinel.prev;
            steps = size - index - 1;
            forward = false;
        }

        for (int i = 0; i < steps; i++) { // Moving the pointer
            curr = forward ? curr.next : curr.prev;
        }

        return curr.item;
    }

    /** Gets the item at the given index recursively. */
    public T getRecursive(int index) {
        if (index >= size || index < 0) { return null; }
        if (index < size / 2) {
            return getRecursiveForward(index, sentinel.next);
        } else {
            return getRecursiveBackward(size - index - 1, sentinel.prev);
        }
    }
    /** helper methods for getRecursive. */
    private T getRecursiveForward(int i, Node n) {
        if (i == 0) { return n.item; }
        return getRecursiveForward(i - 1, n.next);
    }
    private T getRecursiveBackward(int i, Node n) {
        if (i == 0) { return n.item; }
        return getRecursiveBackward(i - 1, n.prev);
    }

    @Override
    public Iterator<T> iterator() { return new DLListIterator(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (!(other instanceof LinkedListDeque)) { return false; }

        LinkedListDeque<T> o = (LinkedListDeque<T>) other;
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


