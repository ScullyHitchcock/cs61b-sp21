package bstmap;
import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {

    private Node root;
    private int size = 0;

    private class Node {
        private K key;
        private V val;
        private Node left;
        private Node right;
        private int height;  // AVL 树中每个节点的高度
        public Node(K key, V val) {
            this.key = key;
            this.val = val;
            this.height = 1;  // 新节点高度为 1
        }
        // 更新当前节点的高度
        public void updateHeight() {
            int leftHeight = (left == null) ? 0 : left.height;
            int rightHeight = (right == null) ? 0 : right.height;
            this.height = Math.max(leftHeight, rightHeight) + 1;
        }
        // 计算当前节点的平衡因子
        public int getBalance() {
            int leftHeight = (left == null) ? 0 : left.height;
            int rightHeight = (right == null) ? 0 : right.height;
            return leftHeight - rightHeight;
        }
        // 如果左边数的高度与右边树的高度差在1内，则该树平衡。
        public boolean isBalanced() {
            int balance = getBalance();
            return (balance <= 1 && balance >= -1);
        }
        // 如果左边数的高度比右边树的高度高2，则左倾。
        public boolean isLeftLeaning() {
            return (getBalance() > 1);
        }
        // 如果左边数的高度比右边树的高度低2，则右倾。
        public boolean isRightLeaning() {
            return (!isBalanced() && !isLeftLeaning());
        }
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return getNode(root, key) != null;
    }

    @Override
    public V get(K key) {
        Node n = getNode(root, key);
        return n == null ? null : n.val;
    }

    private Node getNode(Node node, K key) {
        if (node == null) return null;
        int cmp = key.compareTo(node.key);
        if (cmp == 0) return node;
        else if (cmp < 0) return getNode(node.left, key);
        else return getNode(node.right, key);
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * 插入新键值对，同时保持 AVL 树的平衡性。
     * 如果 key 已存在，则更新其值。
     */
    @Override
    public void put(K key, V value) {
        root = insert(root, key, value);
    }

    private Node insert(Node node, K key, V value) {
        // 一些 base case
        if (node == null) {
            size++;
            return new Node(key, value);
        }
        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            node.val = value;
            return node;
        }

        // 递归主体
        if (cmp < 0) {
            node.left = insert(node.left, key, value);
        } else {
            node.right = insert(node.right, key, value);
        }

        // 递归完成后，开始rebalance操作。
        node.updateHeight();
        if (node.isBalanced()) return node;
        if (node.isLeftLeaning()) {
            // 如果当前节点左倾（左边比右边重），则需要对该节点进行右旋操作
            if (node.left.getBalance() < 0) {
                // 如果当前插入的key在node.child的右侧，则在node左旋之前，增加一步对child的左旋操作
                node.left = rotate(node.left, true);
            }
            return rotate(node, false);
        }
        else {
            // 如果当前节点右倾（右边比左边重），则需要对该节点进行左旋操作
            if (node.right.getBalance() > 0) {
                // 如果当前插入的key在node.child的左侧，则在对node右旋前，增加一部对child的右旋操作
                node.right = rotate(node.right, false);
            }
            return rotate(node, true);
        }
    }

    /**
     * 通用旋转函数，根据参数决定进行左旋还是右旋。
     * @param node 需要旋转的节点
     * @param leftRotation 如果为 true，则进行左旋；否则进行右旋。
     * @return 旋转后的子树根节点
     */
    private Node rotate(Node node, boolean leftRotation) {
        Node child;
        if (leftRotation) {
            // 防御性检查：确保右子节点存在
            if (node.right == null) return node;
            // 左旋操作：将 node 的右子节点提升为新的根节点
            //     1 (node)            2
            //    / \                 / \
            //   x   2 (child) -->   1   z
            //      / \             / \
            //    y    z           x   y
            child = node.right;
            node.right = child.left;
            child.left = node;
        } else {
            // 防御性检查：确保左子节点存在
            if (node.left == null) return node;
            // 右旋操作：将 node 的左子节点提升为新的根节点
            //    (node) 2             1
            //          / \           / \
            // (child) 1   z   -->   x   2
            //        / \               / \
            //       x   y             y   z
            child = node.left;
            node.left = child.right;
            child.right = node;
        }
        node.updateHeight();
        child.updateHeight();
        return child;
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<K> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Prints an in-order diagram of the AVL tree. The tree is printed sideways (rotated 90° clockwise).
     */
    public void printInOrder() {
        printInOrder(root, 0);
    }

    /**
     * Helper method to print the tree in order with indentation representing depth.
     * The tree is printed sideways, so the right subtree is printed first.
     *
     * @param node The current node.
     * @param level The depth level for indentation.
     */
    private void printInOrder(Node node, int level) {
        if (node == null) return;
        printInOrder(node.right, level + 1);
        for (int i = 0; i < level; i++) {
            System.out.print("    ");
        }
        System.out.println(node.key + " (" + node.val + ")");
        printInOrder(node.left, level + 1);
    }
}
