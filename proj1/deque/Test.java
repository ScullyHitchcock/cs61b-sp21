package deque;

public class Test {
    public static void main(String[] args) {
        LinkedListDeque<Integer> l = new LinkedListDeque();
        boolean x = l.isEmpty();
        l.printDeque();
        l.addFirst(1);
        l.printDeque();
        x = l.isEmpty();
        l.addLast(-1);
        l.printDeque();
        x = l.isEmpty();
    }
}
