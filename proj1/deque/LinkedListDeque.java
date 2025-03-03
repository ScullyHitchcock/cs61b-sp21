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
        return sentinel.next == sentinel && sentinel.prev == sentinel;
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
        if (nodeToRemove == sentinel) {
            return null;
        }
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
        if (nodeToRemove == sentinel) {
            return null;
        }
        Node secendLastNode = sentinel.prev.prev;
        sentinel.prev = secendLastNode;
        secendLastNode.next = sentinel;
        size -= 1;
        return nodeToRemove.item;
    }

    /** Gets the item at the given index, where 0 is the front, 1 is the next item, and so forth.
     * If no such item exists, returns null. */
    public T get(int index) {
        if (index >= size || index < 0) {
            return null;
        }
        Node node = sentinel.next;
        for (int i = 0; i < index; i++) {
            node = node.next;
        }
        return node.item;
    }

    /** Gets the item at the given index recursively. */
    public T getRecursive(int index) {
        if (index >= size || index < 0) {
            return null;
        }
        return getRecursive(index, sentinel.next);
    }
    /** A helper method for getRecursive. */
    private T getRecursive(int index, Node node) {
        if (index == 0) {
            return node.item;
        }
        return getRecursive(index - 1, node.next);
    }
}


