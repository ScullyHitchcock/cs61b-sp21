package bstmap;
import java.util.Iterator;
import java.util.Set;
import java.util.NoSuchElementException;
import java.util.Stack;

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
        return rebalanceNode(node);
    }

    /**
     * 对给定的节点进行平衡调整，以维护 AVL 树的平衡性。
     * 该方法检查节点的平衡因子，并在必要时执行旋转操作：
     * 1. 如果左子树比右子树高 2（左倾），则需要进行右旋。
     *    - 如果左子节点本身是右倾的（即其右子树更高），
     *      先对左子节点进行左旋，再对当前节点右旋（LR 旋转）。
     *    - 否则，直接对当前节点进行右旋（LL 旋转）。
     * 2. 如果右子树比左子树高 2（右倾），则需要进行左旋。
     *    - 如果右子节点本身是左倾的（即其左子树更高），
     *      先对右子节点进行右旋，再对当前节点左旋（RL 旋转）。
     *    - 否则，直接对当前节点进行左旋（RR 旋转）。
     *
     * @param node 需要检查并可能进行调整的节点
     * @return 经过平衡调整后的新子树根节点
     */
    private Node rebalanceNode(Node node) {
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

    /**
     * 删除指定的键及其对应的值。
     * 如果该键不存在，则返回 null。
     * 删除操作会先查找到与指定键关联的节点，
     * 然后调用辅助方法从 AVL 树中删除该节点，并保持树的平衡，
     * 同时更新树中元素的数量，最后返回被删除节点的值。
     *
     * @param key 要删除的键
     * @return 被删除的键对应的值，若键不存在则返回 null
     */
    @Override
    public V remove(K key) {
        Node nodeToRemove = getNode(root, key);
        if (nodeToRemove == null) return null; // 不存在该键，返回 null
        V valToRemove = nodeToRemove.val;
        root = removeNode(root, nodeToRemove);
        size--;
        return valToRemove;
    }

    /**
     * 辅助方法：在以 node 为根的子树中删除目标节点 rm，
     * 并在删除后保持 AVL 树的平衡性。
     * 删除操作分三种情况：
     * 1. 如果目标节点在当前节点的左侧，则递归删除左子树；
     * 2. 如果目标节点在当前节点的右侧，则递归删除右子树；
     * 3. 如果当前节点即为目标节点：
     *    - 当左子树为空时，返回右子树（相当于用右子树替换当前节点，并更新 size）；
     *    - 当右子树为空时，返回左子树；
     *    - 当左右子树都存在时，找到右子树中最小的节点（后继），
     *      用后继节点替换当前节点的键和值，并递归删除后继节点。
     *
     * @param node 当前子树的根节点
     * @param rm   要删除的目标节点
     * @return 删除目标节点后调整平衡的子树根节点
     */
    private Node removeNode(Node node, Node rm) {
        if (node == null) return null;

        int cmp = rm.key.compareTo(node.key);
        if (cmp < 0) {
            node.left = removeNode(node.left, rm);
        } else if (cmp > 0) {
            node.right = removeNode(node.right, rm);
        } else {
            // 找到要删除的节点
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;

            // 右子树中找到后继点替代当前节点
            Node successor = findMin(node.right);
            node.key = successor.key;
            node.val = successor.val;
            node.right = removeNode(node.right, successor);
        }

        node.updateHeight();
        return rebalanceNode(node);
    }

    // 找到树的最小节点（左子树最左边的节点）
    private Node findMin(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    @Override
    public V remove(K key, V value) {
        Node node = getNode(root, key);
        if (node == null || !node.val.equals(value)) return null; // 不存在或者值不匹配
        return remove(key);
    }

    /**
     * BSTMap 的迭代器，使用中序遍历来依次遍历所有键，
     * 保证输出的键按照从小到大的顺序排列。
     */
    @Override
    public Iterator<K> iterator() {
        return new BSTIterator();
    }
    private class BSTIterator implements Iterator<K> {
        private Stack<Node> stack;
        // 初始化时，将从根节点开始一路向左的所有节点压入栈中
        public BSTIterator() {
            stack = new Stack<>();
            pushLeftBranch(root);
        }
        /**
         * 将从当前节点开始一直到最左端的所有节点依次压入栈中。
         * 这样栈顶总是当前子树中最小的节点，
         * 便于中序遍历时按照升序输出所有键。
         *
         * @param node 当前起始节点
         */
        private void pushLeftBranch(Node node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }
        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        /**
         * 返回当前中序遍历中的下一个键。
         * 每次从栈中弹出栈顶节点，并将其右子树中的所有左侧节点压入栈中，
         * 从而确保中序遍历顺序正确。
         *
         * @return 下一个键
         * @throws NoSuchElementException 如果没有更多的元素可返回
         */
        @Override
        public K next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node current = stack.pop();
            K key = current.key;
            // 处理右子树
            if (current.right != null) {
                pushLeftBranch(current.right);
            }
            return key;
        }
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
