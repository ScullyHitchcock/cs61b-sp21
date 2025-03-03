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

    public boolean isEmpty() {
        return sentinel.next == sentinel && sentinel.prev == sentinel;
    }

    public int size() {
        return size;
    }

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

    public T get(int index) {
        if (index >= size || index < 0) {
            return null;
        }
        int i = 0;
        Node node = sentinel.next;
        for (i = 0; i < index; i++) {
            node = node.next;
        }
        return node.item;
    }
}


