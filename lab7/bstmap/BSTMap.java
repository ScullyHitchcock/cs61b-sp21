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
        root = get(root, key, value);
    }

    private Node get(Node node, K key, V value) {
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
            node.left = get(node.left, key, value);
        } else {
            node.right = get(node.right, key, value);
        }

        // 递归完成后，开始rebalance操作。更新当前节点高度
        updateHeight(node);

        // 获取平衡因子：左边数的 height - 右边树的 height
        // 如果 balance 的绝对值大于1，说明不平衡
        int balanceFactor = getBalance(node);
        boolean isBalanced = (balanceFactor <= 1 && balanceFactor >= -1);
        boolean isLeftLeaning = (balanceFactor > 1);
        if (isBalanced) return node;
        if (isLeftLeaning) {
            if (cmp > 0) {
                node.left = rotate(node.left, true);
            }
            return rotate(node, false);
        }
        else {
            if (cmp < 0) {
                node.right = rotate(node.right, false);
            }
            return rotate(node, true);
        }
    }

    // 返回节点的高度
    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    // 计算节点的平衡因子（左子树高度 - 右子树高度）
    private int getBalance(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    /**
     * 通用旋转函数，根据参数决定进行左旋还是右旋。
     * @param node 需要旋转的节点
     * @param leftRotation 如果为 true，则进行左旋；否则进行右旋。
     * @return 旋转后的子树根节点
     */
    private Node rotate(Node node, boolean leftRotation) {
        if (leftRotation) {
            // 左旋操作：将 node 的右子节点提升为新的根节点
            //     1 (node)            2
            //    / \                 / \
            //   x   2 (child) -->   1   z
            //      / \             / \
            //    y    z           x   y
            Node child = node.right;
            node.right = child.left;
            child.left = node;
            updateHeight(node);
            updateHeight(child);
            return child;
        } else {
            // 右旋操作：将 node 的左子节点提升为新的根节点
            //    (node) 2             1
            //          / \           / \
            // (child) 1   z   -->   x   2
            //        / \               / \
            //       x   y             y   z
            Node child = node.left;
            node.left = child.right;
            child.right = node;
            updateHeight(node);
            updateHeight(child);
            return child;
        }
    }

    /**
     * 更新节点的高度。高度等于左右子树中较大者的高度加 1。
     * @param node 要更新高度的节点
     */
    private void updateHeight(Node node) {
        node.height = Math.max(height(node.left), height(node.right)) + 1;
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
