package bstmap;

import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable, V> implements Map61B<K, V>{

    int size = 0;

    /* 内部类（二叉树结构）*/
    Tree bst;
    private class Tree {
        private K key;
        private V val;
        private Tree left;
        private Tree right;

        /**
         * 构造带分叉的树
         * @param k：键
         * @param v：值
         * @param l：左分支，其分支内的节点的 K 对象都比 k 小
         * @param r：右分支，其分支内的节点的 K 对象都比 k 大
         */
        public Tree(K k, V v, Tree l, Tree r) {
            key = k;
            val = v;
            left = l;
            right = r;
        }

        /** 构造空树 */
        public Tree(K k, V v) {
            key = k;
            val = v;
        }

        /** 左接枝 */
        private void addLeft(Tree left) {
            this.left = left;
        }

        /** 右接枝 */
        private void addRight(Tree right) {
            this.right = right;
        }
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        Tree t = new Tree(key, value);
        if (size() == 0) {
            bst = t;
        } else {
            Tree cur = bst; // 设置指针，初始指向 root
            while(cur.left != null ||  cur.right != null) { // 只要当前node存在child
                int res = key.compareTo(cur.key); // 比较新key和指针指向的node的key大小
                if (res == 0) return; // 如果结果为0，说明新key与当前key相同，返回
                if (res > 0) { // 如果结果大于0，说明新key比当前key大，移动指针值node的右边
                    if (cur.right == null) {
                        cur.right = t;
                        break;
                    }
                    cur = cur.right;
                } else { // 如果结果小于0，说明新key比当前key小，移动指针值node的左边
                    if (cur.left == null) {
                        cur.left = t;
                        break;
                    }
                    cur = cur.left;
                }
            }
        }
    }

    @Override
    public Set<K> keySet() {
        return Set.of();
    }

    @Override
    public V remove(K key) {
        return null;
    }

    @Override
    public V remove(K key, V value) {
        return null;
    }

    @Override
    public Iterator<K> iterator() {
        return null;
    }
}
