package byow.lab12;

import byow.TileEngine.TERenderer;
/**
 * Draws a world consisting of hexagonal regions.
 */
public class HexWorld {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 50;
    private static final int HEX_SIZE = 4;

    public static void main(String args[]) {
        // 初始化窗口
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);
        World world = new World(WIDTH, HEIGHT, HEX_SIZE);
        // 确定初始落笔点
        int startX = (WIDTH - HEX_SIZE) / 2;
        int startY = 0;
        // 在落笔点画一个六边形
        // 以六边形为起点扩散画六边形
        world.generateWorld(startX, startY);
        // draws the world to the screen
        ter.renderFrame(world.tiles);
    }
}
