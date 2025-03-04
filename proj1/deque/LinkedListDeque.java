package deque;

public class LinkedListDeque<T> {
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
    private int size;
    private Node sentinel;

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
        Node node = sentinel.next;
        while (node != sentinel) {
            if (node.next == sentinel) {
                System.out.println(node.item);
            } else {
                System.out.print(node.item + " ");
            }
            node = node.next;
        }
    }

    /** Removes and returns the item at the front of the deque.
     * If no such item exists, returns null. */
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
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        Node curr;
        int steps;
        boolean forward;
        if (index < size / 2) {
            curr = sentinel.next;
            steps = index;
            forward = true;
        } else {
            curr = sentinel.prev;
            steps = size - index - 1;
            forward = false;
        }

        for (int i = 0; i < steps; i++) {
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
}


