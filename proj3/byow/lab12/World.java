package byow.lab12;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.HashSet;
import java.util.Set;

/**
 * World 是一个用于生成由六边形区域组成的地图的类。
 * 它负责初始化地图、生成随机六边形区域并递归扩展，填充不同的 tile 类型。
 */
public class World {

    /** 地图的宽度（单位：tile） */
    private final int width;

    /** 地图的高度（单位：tile） */
    private final int height;

    /** 地图上的 tile 网格，用于绘制所有六边形 */
    public TETile[][] tiles;

    /** 每个六边形的边长（size） */
    private final int size;

    /**
     * 构造函数，初始化地图宽度、高度和六边形边长，并填充空白 tile。
     */
    public World(int width, int height, int size) {
        this.width = width;
        this.height = height;
        this.size = size;
        tiles = initTiles(width, height);
    }

    /**
     * 初始化地图，将所有 tile 设置为 Tileset.NOTHING。
     */
    private TETile[][] initTiles(int width, int height) {
        TETile[][] res = new TETile[width][height];
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                res[x][y] = Tileset.NOTHING;
            }
        }
        return res;
    }

    /**
     * 在指定位置创建初始六边形，并递归向周围扩展生成六边形地图。
     */
    public void generateWorld(int startX, int startY) {
        Hex initHex = new Hex(startX, startY, size);
        spreadFilling(initHex);
    }

    /**
     * 返回一个随机的非空 TETile，用于填充六边形区域。
     */
    public TETile randomTile() {
        List<TETile> tiles = List.of(
                Tileset.AVATAR,
                Tileset.WALL,
                Tileset.FLOOR,
                Tileset.GRASS,
                Tileset.WATER,
                Tileset.FLOWER,
                Tileset.LOCKED_DOOR,
                Tileset.UNLOCKED_DOOR,
                Tileset.SAND,
                Tileset.MOUNTAIN,
                Tileset.TREE
        );
        Random rand = new Random();
        return tiles.get(rand.nextInt(tiles.size()));
    }

    /**
     * 迭代方式填充当前六边形，并向其所有邻接六边形扩展。
     */
    private void spreadFilling(Hex startHex) {
        Stack<Hex> stack = new Stack<>();
        Set<Hex> visited = new HashSet<>();
        stack.push(startHex);

        while (!stack.isEmpty()) {
            Hex current = stack.pop();
            if (!isValid(current) || visited.contains(current) || !isEmpty(current)) {
                continue;
            }

            visited.add(current);
            TETile tile = randomTile();
            fill(current, tile);

            for (Hex neighbor : current.adjHexes().values()) {
                stack.push(neighbor);
            }
        }
    }


//    /**
//     * 递归方式填充当前六边形，并向其所有邻接六边形扩展。
//     */
//    private void spreadFilling(Hex hex) {
//        TETile tile = randomTile();
//        fill(hex, tile);
//        for (Hex adj : hex.adjHexes().values()) {
//            if (isValid(adj) && isEmpty(adj)) {
//                spreadFilling(adj);
//            }
//        }
//    }

    /**
     * 按照六边形填充规则将 tile 应用于地图。
     */
    private void fill(Hex hex, TETile tile) {
        for (int x = hex.westX(); x <= hex.eastX(); x++) {
            for (int y = hex.southY(); y <= hex.northY(); y++) {
                int distToHorizontalEdge = Math.min(hex.northY() - y, y - hex.southY());
                int distToVerticalEdge = Math.min(hex.eastX() - x, x - hex.westX());
                int dist = distToHorizontalEdge + distToVerticalEdge;
                if (dist >= hex.size - 1) {
                    if (dist == hex.size - 1 || distToHorizontalEdge == 0) {
                        tiles[x][y] = Tileset.WALL;
                    } else {
                        tiles[x][y] = Tileset.FLOOR;
                    }
                }
            }
        }
    }

    /**
     * 检查该 hex 的中心位置是否尚未填充，即为“nothing”。
     */
    private boolean isEmpty(Hex hex) {
        return tiles[hex.baseX][hex.baseY].description().equals("nothing");
    }

    /**
     * 判断一个六边形是否在地图边界内，确保不会越界绘制。
     */
    private boolean isValid(Hex hex) {
        return (hex.eastX() <= width && hex.westX() >= 0) &&
                (hex.northY() <= height && hex.southY() >= 0);
    }
}
