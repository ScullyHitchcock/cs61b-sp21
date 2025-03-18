package bstmap;

import static org.junit.Assert.*;
import org.junit.Test;

/** Tests by Brendan Hu, Spring 2015, revised for 2016 by Josh Hug */
public class TestBSTMap {

  	@Test
    public void sanityGenericsTest() {
    	try {
    		BSTMap<String, String> a = new BSTMap<String, String>();
	    	BSTMap<String, Integer> b = new BSTMap<String, Integer>();
	    	BSTMap<Integer, String> c = new BSTMap<Integer, String>();
	    	BSTMap<Boolean, Integer> e = new BSTMap<Boolean, Integer>();
	    } catch (Exception e) {
	    	fail();
	    }
    }

    //assumes put/size/containsKey/get work
    @Test
    public void sanityClearTest() {
    	BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        for (int i = 0; i < 455; i++) {
            b.put("hi" + i, 1+i);
            //make sure put is working via containsKey and get
            assertTrue( null != b.get("hi" + i) && (b.get("hi"+i).equals(1+i))
                        && b.containsKey("hi" + i));
        }
        assertEquals(455, b.size());
        b.clear();
        assertEquals(0, b.size());
        for (int i = 0; i < 455; i++) {
            assertTrue(null == b.get("hi" + i) && !b.containsKey("hi" + i));
        }
    }

    // assumes put works
    @Test
    public void sanityContainsKeyTest() {
    	BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        assertFalse(b.containsKey("waterYouDoingHere"));
        b.put("waterYouDoingHere", 0);
        assertTrue(b.containsKey("waterYouDoingHere"));
    }

    // assumes put works
    @Test
    public void sanityGetTest() {
    	BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        assertEquals(null,b.get("starChild"));
        assertEquals(0, b.size());
        b.put("starChild", 5);
        assertTrue(((Integer) b.get("starChild")).equals(5));
        b.put("KISS", 5);
        assertTrue(((Integer) b.get("KISS")).equals(5));
        assertNotEquals(null,b.get("starChild"));
        assertEquals(2, b.size());
    }

    // assumes put works
    @Test
    public void sanitySizeTest() {
    	BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        assertEquals(0, b.size());
        b.put("hi", 1);
        assertEquals(1, b.size());
        for (int i = 0; i < 455; i++)
            b.put("hi" + i, 1);
        assertEquals(456, b.size());
    }

    //assumes get/containskey work
    @Test
    public void sanityPutTest() {
    	BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        b.put("hi", 1);
        assertTrue(b.containsKey("hi") && b.get("hi") != null);
    }

    //assumes put works
    @Test
    public void containsKeyNullTest() {
        BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        b.put("hi", null);
        assertTrue(b.containsKey("hi"));
    }

    // Helper method to get the root key using reflection
    private int getRootKey(BSTMap<Integer, String> tree) throws Exception {
        java.lang.reflect.Field rootField = BSTMap.class.getDeclaredField("root");
        rootField.setAccessible(true);
        Object root = rootField.get(tree);
        java.lang.reflect.Field keyField = root.getClass().getDeclaredField("key");
        keyField.setAccessible(true);
        return (Integer) keyField.get(root);
    }

    // Helper method to get the left child's key using reflection
    private int getLeftChildKey(BSTMap<Integer, String> tree) throws Exception {
        java.lang.reflect.Field rootField = BSTMap.class.getDeclaredField("root");
        rootField.setAccessible(true);
        Object root = rootField.get(tree);
        java.lang.reflect.Field leftField = root.getClass().getDeclaredField("left");
        leftField.setAccessible(true);
        Object leftChild = leftField.get(root);
        java.lang.reflect.Field keyField = leftChild.getClass().getDeclaredField("key");
        keyField.setAccessible(true);
        return (Integer) keyField.get(leftChild);
    }

    // Helper method to get the right child's key using reflection
    private int getRightChildKey(BSTMap<Integer, String> tree) throws Exception {
        java.lang.reflect.Field rootField = BSTMap.class.getDeclaredField("root");
        rootField.setAccessible(true);
        Object root = rootField.get(tree);
        java.lang.reflect.Field rightField = root.getClass().getDeclaredField("right");
        rightField.setAccessible(true);
        Object rightChild = rightField.get(root);
        java.lang.reflect.Field keyField = rightChild.getClass().getDeclaredField("key");
        keyField.setAccessible(true);
        return (Integer) keyField.get(rightChild);
    }

    // Test for LL Rotation: Insert keys in descending order to trigger a right rotation.
    //        30
    //       /
    //     20   -->   20
    //    /          /  \
    //   10         10  30
    @Test
    public void testLLRotation() throws Exception {
        BSTMap<Integer, String> tree = new BSTMap<>();
        tree.put(30, "30");
        tree.put(20, "20");
        tree.put(10, "10");
        // After LL imbalance, expected structure: root=20, left=10, right=30
        assertEquals(20, getRootKey(tree));
        assertEquals(10, getLeftChildKey(tree));
        assertEquals(30, getRightChildKey(tree));
    }

    // Test for RR Rotation: Insert keys in ascending order to trigger a left rotation.
    //    10
    //      \
    //       20    -->    20
    //        \          /  \
    //         30       10   30
    @Test
    public void testRRRotation() throws Exception {
        BSTMap<Integer, String> tree = new BSTMap<>();
        tree.put(10, "10");
        tree.put(20, "20");
        tree.put(30, "30");
        // After RR imbalance, expected structure: root=20, left=10, right=30
        assertEquals(20, getRootKey(tree));
        assertEquals(10, getLeftChildKey(tree));
        assertEquals(30, getRightChildKey(tree));
    }

    // Test for LR Rotation: Insert keys to trigger left-right rotation.
    //    30
    //    /
    //   10    -->    20
    //    \          / \
    //    20        10  30
    @Test
    public void testLRRotation() throws Exception {
        BSTMap<Integer, String> tree = new BSTMap<>();
        tree.put(30, "30");
        tree.put(10, "10");
        tree.put(20, "20");
        // After LR imbalance, expected structure: root=20, left=10, right=30
        assertEquals(20, getRootKey(tree));
        assertEquals(10, getLeftChildKey(tree));
        assertEquals(30, getRightChildKey(tree));
    }

    // Test for RL Rotation: Insert keys to trigger right-left rotation.
    //    10
    //     \
    //     30  -->   20
    //     /        / \
    //    20       10  30
    @Test
    public void testRLRotation() throws Exception {
        BSTMap<Integer, String> tree = new BSTMap<>();
        tree.put(10, "10");
        tree.put(30, "30");
        tree.put(20, "20");
        // After RL imbalance, expected structure: root=20, left=10, right=30
        assertEquals(20, getRootKey(tree));
        assertEquals(10, getLeftChildKey(tree));
        assertEquals(30, getRightChildKey(tree));
    }

    // Helper method to get the left child's key of an arbitrary node using reflection
    private int getLeftChildKeyOfNode(Object node) throws Exception {
        java.lang.reflect.Field leftField = node.getClass().getDeclaredField("left");
        leftField.setAccessible(true);
        Object leftChild = leftField.get(node);
        java.lang.reflect.Field keyField = leftChild.getClass().getDeclaredField("key");
        keyField.setAccessible(true);
        return (Integer) keyField.get(leftChild);
    }

    // Helper method to get the right child's key of an arbitrary node using reflection
    private int getRightChildKeyOfNode(Object node) throws Exception {
        java.lang.reflect.Field rightField = node.getClass().getDeclaredField("right");
        rightField.setAccessible(true);
        Object rightChild = rightField.get(node);
        java.lang.reflect.Field keyField = rightChild.getClass().getDeclaredField("key");
        keyField.setAccessible(true);
        return (Integer) keyField.get(rightChild);
    }

    // Test for ascending order insertion of 5 elements:
    // Insert keys: 10, 20, 30, 40, 50 (ascending order)
    // Expected AVL tree structure after rebalancing:
    //        20
    //       /  \
    //     10    40
    //          /  \
    //         30   50
    @Test
    public void testAscendingFiveElements() throws Exception {
        BSTMap<Integer, String> tree = new BSTMap<>();
        tree.put(10, "10");
        tree.put(20, "20");
        tree.put(30, "30");
        tree.put(40, "40");
        tree.put(50, "50");
        // Check root, left, right keys from the tree's root
        assertEquals(20, getRootKey(tree));
        assertEquals(10, getLeftChildKey(tree));
        assertEquals(40, getRightChildKey(tree));
        // 获取 root.right 对象，并检查其左右子节点
        java.lang.reflect.Field rootField = BSTMap.class.getDeclaredField("root");
        rootField.setAccessible(true);
        Object root = rootField.get(tree);
        java.lang.reflect.Field rightField = root.getClass().getDeclaredField("right");
        rightField.setAccessible(true);
        Object rightChild = rightField.get(root);
        assertEquals(30, getLeftChildKeyOfNode(rightChild));
        assertEquals(50, getRightChildKeyOfNode(rightChild));
    }

    // Test for descending order insertion of 5 elements:
    // Insert keys: 50, 40, 30, 20, 10 (descending order)
    // Expected AVL tree structure after rebalancing:
    //        40
    //       /  \
    //     20    50
    //    /  \
    //  10    30
    @Test
    public void testDescendingFiveElements() throws Exception {
        BSTMap<Integer, String> tree = new BSTMap<>();
        tree.put(50, "50");
        tree.put(40, "40");
        tree.put(30, "30");
        tree.put(20, "20");
        tree.put(10, "10");
        // Check root, left, right keys from the tree's root
        assertEquals(40, getRootKey(tree));
        assertEquals(20, getLeftChildKey(tree));
        assertEquals(50, getRightChildKey(tree));
        // 获取 root.left 对象，并检查其左子节点
        java.lang.reflect.Field rootField = BSTMap.class.getDeclaredField("root");
        rootField.setAccessible(true);
        Object root = rootField.get(tree);
        java.lang.reflect.Field leftField = root.getClass().getDeclaredField("left");
        leftField.setAccessible(true);
        Object leftChild = leftField.get(root);
        assertEquals(10, getLeftChildKeyOfNode(leftChild));
        assertEquals(30, getRightChildKeyOfNode(leftChild));
    }

    // Test for descending order insertion of 7 elements:
    // Insert keys: 70, 60, 50, 40, 30, 20, 10 (descending order)
    // Expected AVL tree structure after rebalancing:
    //          40
    //       /     \
    //     20       60
    //    /  \      / \
    //  10    30   50  70
    @Test
    public void testDescendingSevenElements() throws Exception {
        BSTMap<Integer, String> tree = new BSTMap<>();
        tree.put(70, "70");
        tree.put(60, "60");
        tree.put(50, "50");
        tree.put(40, "40");
        tree.put(30, "30");
        tree.put(20, "20");
        tree.put(10, "10");
        // Check root, left, right keys from the tree's root
        assertEquals(40, getRootKey(tree));
        assertEquals(20, getLeftChildKey(tree));
        assertEquals(60, getRightChildKey(tree));
        // 获取 root.left 对象，并检查其左子节点
        java.lang.reflect.Field rootField = BSTMap.class.getDeclaredField("root");
        rootField.setAccessible(true);
        Object root = rootField.get(tree);
        java.lang.reflect.Field leftField = root.getClass().getDeclaredField("left");
        leftField.setAccessible(true);
        Object leftChild = leftField.get(root);
        // 获取 root.right 对象，并检查其右子节点
        java.lang.reflect.Field rightField = root.getClass().getDeclaredField("right");
        rightField.setAccessible(true);
        Object rightChild = rightField.get(root);
        assertEquals(10, getLeftChildKeyOfNode(leftChild));
        assertEquals(30, getRightChildKeyOfNode(leftChild));
        assertEquals(50, getLeftChildKeyOfNode(rightChild));
        assertEquals(70, getRightChildKeyOfNode(rightChild));
    }
}
