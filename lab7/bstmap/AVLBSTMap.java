package bstmap;

import java.util.Iterator;
import java.util.Set;

public class AVLBSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {

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
        return get(key) != null;
    }

    @Override
    public V get(K key) {
        Node n = get(root, key);
        return n == null ? null : n.val;
    }

    private Node get(Node node, K key) {
        if (node == null) return null;
        int cmp = key.compareTo(node.key);
        if (cmp == 0) return node;
        else if (cmp < 0) return get(node.left, key);
        else return get(node.right, key);
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
        if (node == null) {
            size++;
            return new Node(key, value);
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = get(node.left, key, value);
        } else if (cmp > 0) {
            node.right = get(node.right, key, value);
        } else {  // 重复 key，更新值
            node.val = value;
            return node;
        }
        // 更新当前节点高度
        updateHeight(node);
        // 获取平衡因子
        int balance = getBalance(node);

        // 左左情况：新键插入到左子树的左侧，直接进行右旋
        if (balance > 1 && key.compareTo(node.left.key) < 0) {
            return rotate(node, false);
        }
        // 右右情况：新键插入到右子树的右侧，直接进行左旋
        if (balance < -1 && key.compareTo(node.right.key) > 0) {
            return rotate(node, true);
        }
        // 左右情况：先对左子节点进行左旋，再对当前节点进行右旋
        if (balance > 1 && key.compareTo(node.left.key) > 0) {
            node.left = rotate(node.left, true);
            return rotate(node, false);
        }
        // 右左情况：先对右子节点进行右旋，再对当前节点进行左旋
        if (balance < -1 && key.compareTo(node.right.key) < 0) {
            node.right = rotate(node.right, false);
            return rotate(node, true);
        }
        return node;
    }

    // 返回节点的高度
    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    /**
     * 更新节点的高度。高度等于左右子树中较大者的高度加 1。
     * @param node 要更新高度的节点
     */
    private void updateHeight(Node node) {
        node.height = Math.max(height(node.left), height(node.right)) + 1;
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
            Node child = node.right;
            node.right = child.left;
            child.left = node;
            updateHeight(node);
            updateHeight(child);
            return child;
        } else {
            // 右旋操作：将 node 的左子节点提升为新的根节点
            Node child = node.left;
            node.left = child.right;
            child.right = node;
            updateHeight(node);
            updateHeight(child);
            return child;
        }
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
