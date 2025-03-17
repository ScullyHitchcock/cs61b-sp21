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
        root = put(root, key, value);
    }

    private Node put(Node node, K key, V value) {
        if (node == null) {
            size++;
            return new Node(key, value);
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = put(node.left, key, value);
        } else if (cmp > 0) {
            node.right = put(node.right, key, value);
        } else {  // 重复 key，更新值
            node.val = value;
            return node;
        }
        // 更新当前节点高度
        node.height = 1 + Math.max(height(node.left), height(node.right));
        // 获取平衡因子
        int balance = getBalance(node);

        // 左左情况
        if (balance > 1 && key.compareTo(node.left.key) < 0) {
            return rightRotate(node);
        }
        // 右右情况
        if (balance < -1 && key.compareTo(node.right.key) > 0) {
            return leftRotate(node);
        }
        // 左右情况
        if (balance > 1 && key.compareTo(node.left.key) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        // 右左情况
        if (balance < -1 && key.compareTo(node.right.key) < 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }
        return node;
    }

    // 返回节点的高度
    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    // 计算节点的平衡因子（左子树高度 - 右子树高度）
    private int getBalance(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    // 右旋操作
    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;
        // 旋转过程
        x.right = y;
        y.left = T2;
        // 更新高度
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        return x;
    }

    // 左旋操作
    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;
        // 旋转过程
        y.left = x;
        x.right = T2;
        // 更新高度
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        return y;
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
