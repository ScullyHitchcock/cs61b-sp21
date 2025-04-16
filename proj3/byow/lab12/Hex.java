package byow.lab12;


import java.util.HashMap;
import java.util.Map;

/**
 * 枚举类型 Direction 表示六边形相邻的六个方向。
 */
enum Direction {
    /** 上方六边形 */
    UP,
    /** 下方六边形 */
    DOWN,
    /** 左上方六边形 */
    LEFT_UP,
    /** 左下方六边形 */
    LEFT_DOWN,
    /** 右上方六边形 */
    RIGHT_UP,
    /** 右下方六边形 */
    RIGHT_DOWN
}

/**
 * Hex 表示一个六边形的基本单位。
 * 每个 Hex 由其底部中心坐标 (baseX, baseY) 和边长 size 确定。
 */
public class Hex {

    /** 六边形的边长 */
    public final int size;

    /** 六边形底边中心的 x 坐标 */
    public final int baseX;

    /** 六边形底边中心的 y 坐标 */
    public final int baseY;

    /**
     * 构造一个 Hex 实例。
     * @param x 六边形底边中心的 x 坐标
     * @param y 六边形底边中心的 y 坐标
     * @param s 六边形边长
     */
    public Hex(int x, int y, int s) {
        baseX = x;
        baseY = y;
        size = s;
    }

    /**
     * 返回六边形的宽度（以 tile 数计算）。
     */
    public int width() {
        return 3 * size - 2;
    }

    /**
     * 返回六边形的高度（以 tile 数计算）。
     */
    public int height() {
        return 2 * size;
    }

    /**
     * 返回六边形最左边的 x 坐标。
     */
    public int westX() {
        return baseX - (size - 1);
    }

    /**
     * 返回六边形底边的 y 坐标。
     */
    public int southY() {
        return baseY;
    }

    /**
     * 返回六边形最右边的 x 坐标。
     */
    public int eastX() {
        return westX() + width() - 1;
    }

    /**
     * 返回六边形顶边的 y 坐标。
     */
    public int northY() {
        return southY() + height() - 1;
    }

    /**
     * 返回当前六边形相邻的六个方向上的六边形。
     */
    public Map<Direction, Hex> adjHexes() {
        Map<Direction, Hex> adj = new HashMap<>();
        adj.put(Direction.UP, new Hex(baseX, northY() + 1, size));
        adj.put(Direction.DOWN, new Hex(baseX, southY() - height(), size));
        adj.put(Direction.LEFT_UP, new Hex(westX() - size, southY() + size, size));
        adj.put(Direction.LEFT_DOWN, new Hex(westX() - size, southY() - size, size));
        adj.put(Direction.RIGHT_UP, new Hex(eastX() + 1, southY() + size, size));
        adj.put(Direction.RIGHT_DOWN, new Hex(eastX() + 1, southY() - size, size));
        return adj;
    }
}
