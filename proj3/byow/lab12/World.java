package byow.lab12;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.Set;

public class World {
    private int width;
    private int height;
    public TETile[][] tiles;
    public Set<Hex> Hexes;


    public World(int width, int height) {
        this.width = width;
        this.height = height;
        tiles = initTiles(width, height);
    }

    private TETile[][] initTiles(int width, int height) {
        TETile[][] res = new TETile[width][height];
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                res[x][y] = Tileset.NOTHING;
            }
        }
        return res;
    }

    public Hex drawHex(int startX, int startY) {
        Hex res = new Hex(startX, startY);
        Hexes.add(res);
        return res;
    }

    public void drawHex(Hex adj) {

    }
}
