package byow.lab12;

import byow.TileEngine.TERenderer;

import java.util.*;

/**
 * Draws a world consisting of hexagonal regions.
 */
public class HexWorld {

    private static final int WIDTH = 27;
    private static final int HEIGHT = 30;
    private static final int HEX_SIZE = 3;

    public static void main(String args[]) {
        // 初始化窗口
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);
        World world = new World(WIDTH, HEIGHT);
        // 确定初始落笔点
        int startX = (WIDTH - HEX_SIZE) / 2;
        int startY = 0;
        // 在落笔点画一个六边形
        Hex startHex = world.drawHex(startX, startY);
        // 以六边形为起点扩散画六边形
        Queue<Hex> q = new LinkedList<>();
        q.add(startHex);
        Set<Hex> visited = new HashSet<>();
        while (!q.isEmpty()) {
            Hex cur = q.poll();
            visited.add(cur);
            for (Hex adj : cur.adj()) {
                if (!visited.contains(adj)) {
                    q.add(adj);
                }
                if (!adj.exists()) {
                    world.drawHex(adj);
                }
            }
        }
        // draws the world to the screen
        ter.renderFrame(world.tiles);
    }
}
