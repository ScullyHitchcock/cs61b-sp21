package bstmap;

import org.junit.Test;

public class MyTest {
    @Test
    public void test() {
        AVLBSTMap<Integer, Integer> avlbstMap = new AVLBSTMap<>();
//        BSTMap<Integer, Integer> bstMap = new BSTMap<>();
        for (int i = 0; i < 15; i++) {
            avlbstMap.put(i, i);
//            bstMap.put(i, i);
        }
        avlbstMap.printInOrder();
//        bstMap.printInOrder();
    }
}
