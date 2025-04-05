package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] table;
    // You should probably define some more!
    private final double maxLoadFactor;
    private int numOfBucket;
    private int numOfNode;

    /** Constructors */
    public MyHashMap() {
        maxLoadFactor = 0.75;
        numOfBucket = 16;
        table = createTable(numOfBucket);
    }

    public MyHashMap(int initialSize) {
        numOfBucket = initialSize;
        maxLoadFactor = 0.75;
        table = createTable(numOfBucket);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        numOfBucket = initialSize;
        maxLoadFactor = maxLoad;
        table = createTable(numOfBucket);
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new ArrayList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        // 创建新 Collection 数组 table
        Collection<Node>[] table = new Collection[tableSize];
        // 遍历 table，调用 createBucket()
        for (int i = 0; i < table.length; i++) {
            table[i] = createBucket();
        }
        // 返回 table
        return table;
    }

    // TODO: Implement the methods of the Map61B Interface below
    // Your code won't compile until you do so!
    @Override
    public void clear() {
        numOfBucket = 16;
        numOfNode = 0;
        table = createTable(numOfBucket);
    }

    private Node getNode(K key) {
        for (Node node : table[indexOf(key)]) {
            if (node.key.equals(key)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(K key) {
        return (getNode(key) != null);
    }

    @Override
    public V get(K key) {
        Node node = getNode(key);
        if (node != null) {
            return node.value;
        }
        return null;
    }

    @Override
    public int size() {
        return numOfNode;
    }

    @Override
    public void put(K key, V value) {
        // 创建新 newNode 储存键值对，获取哈希值，以确定 newNode 应在 table 中的位置
        Node newNode = createNode(key, value);
        int index = indexOf(key);

        // 遍历相应位置的 bucket 查询是否存在相同的 node
        // 如果已经存在则更新 value
        for (Node node : table[index]) {
            if (node.key.equals(newNode.key)) {
                node.value = newNode.value;
                return;
            }
        }

        // 否则将 newNode 加入到 bucket 中，更新 nodeNum
        table[index].add(newNode);
        numOfNode++;
        // 如果此时 loadFactor 到达阈值，则调用 resize()
        double lf = loadFactor();
        if (lf > maxLoadFactor) {
            resize();
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> s = new HashSet<>();
        for (K key : this) {
            s.add(key);
        }
        return s;
    }

    @Override
    public V remove(K key) {
        Node node = getNode(key);
        if (node != null) {
            table[indexOf(key)].remove(node);
            return node.value;
        }
        return null;
    }

    @Override
    public V remove(K key, V value) {
        Node node = getNode(key);
        if (node != null && node.value.equals(value)) {
            table[indexOf(key)].remove(node);
            return node.value;
        }
        return null;
    }

    @Override
    public Iterator<K> iterator() {
        return new MyHashMapIterator();
    }

    private class MyHashMapIterator implements Iterator<K> {
        Iterator<Node> bucketIterator;
        int index;

        public MyHashMapIterator() {
            index = 0;
            bucketIterator = table[index].iterator();
            moveToNext();
        }

        @Override
        public boolean hasNext() {
            return index < numOfBucket;
        }

        @Override
        public K next() {
            K res = getCur();
            moveToNext();
            return res;
        }

        private K getCur() {
            return bucketIterator.next().key;
        }
        private void moveToNext() {
            while (true) {
                if (bucketIterator.hasNext()) {
                    break;
                }
                if (index >= numOfBucket) {
                    break;
                }
                index++;
                if (index < numOfBucket) {
                    bucketIterator = table[index].iterator();
                }
            }
        }
    }

    private double loadFactor() {
        return (double) numOfNode / numOfBucket;
    }

    private void resize() {
        numOfBucket *= 2;
        Collection<Node>[] newTable = createTable(numOfBucket);
        for (Collection<Node> bucket : table) {
            for (Node node : bucket) {
                newTable[indexOf(node.key)].add(node);
            }
        }
        table = newTable;
    }

    private int indexOf(K key) {
        return Math.floorMod(key.hashCode(), numOfBucket);
    }
}
