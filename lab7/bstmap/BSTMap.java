package bstmap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V>{

    /* 内部类（二叉树结构）*/
    private Node node;
    private int size = 0;

    private class Node implements Comparable<Node>{
        private K key;
        private V val;
        private Node left;
        private Node right;

        /** 构造空节点 */
        public Node(K k, V v) {
            key = k;
            val = v;
        }

        @Override
        public int compareTo(Node o) {
            return this.key.compareTo(o.key);
        }

        /** 当 node 子节点数为1时，返回子节点。 */
        private Node child(){
            if (this.left == null) {
                return this.right;
            } else return this.left;
        }

        /** 返回子节点数 */
        private int children() {
            int res = 0;
            if (left != null) {
                res += 1;
            }
            if (right != null) {
                res += 1;
            }
            return res;
        }
    }

    @Override
    public void clear() {
        node = null;
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return (get(node, key) != null);
    }

    @Override
    public V get(K key) {
        Node n = get(node, key);
        if (n == null) return null;
        return n.val;
    }

    private Node get(Node n, K key) {
        if (n == null) return null;
        int res = key.compareTo(n.key);
        if (res == 0) return n;
        if (res > 0) return get(n.right, key);
        return get(n.left,key);
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * 从根节点出发开始检测每个 node ：
     * 1，if node 的子节点数量为 0 :
     *      if key > node.key: key 放在 node.right，结束。
     *      否则放在node.left，结束。
     * 2，if node 的子节点数量为 1 :
     *      将 key, node.key, node.child.key 进行排序，
     *      创建新节点树 Tree(最大值, 中间值, 最小值)，替换原 node ，结束。
     *      if key < node.key 且 node.left 不为空:
     * 3，if node 的子节点数量为 2 :
     *      if key > node.key: node = node.right
     *      if key < node.key: node = node.left
     */
    @Override
    public void put(K key, V value) {
        Node newNode = new Node(key, value);
        if (size == 0) { // 处理初始化
            node = newNode;
            size += 1;
        } else if (rebalance(node, newNode)) {
            size += 1;
        }
    }

    private boolean rebalance(Node node, Node newNode) {
        int children = node.children(); // 子节点数
        int res = newNode.compareTo(node); // 新节点与当前节点的比较结果
        if (res == 0) return false;
        if (children == 2) { // 当 node 有两个子节点，继续递归，直到 node 子节点数不足2个。
            if (res > 0) {
                return rebalance(node.right, newNode);
            } else {
                return rebalance(node.left, newNode);
            }
        }
        // 开始重新分配节点链接情况
        else if (children == 1) { // 当 node 只有一个子节点，进行平衡节点左右操作。
            Node curCopy = new Node(node.key, node.val);
            Object[] arr = new Object[]{newNode, curCopy, node.child()};
            Arrays.sort(arr);
            Node root = (Node) arr[1];
            node.key = root.key;
            node.val = root.val;
            node.left = (Node) arr[0];
            node.right = (Node) arr[2];
            return true;
        } else { // 当 node 没有子节点，直接加入。
            if (res > 0) node.right = newNode;
            else node.left = newNode;
            return true;
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
}
