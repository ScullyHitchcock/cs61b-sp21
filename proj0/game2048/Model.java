package game2048;

import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @CST
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;
        // TODO: Modify this.board (and perhaps this.score) to account
        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.
        int size = this.board.size();
        this.board.setViewingPerspective(side);
        for (int col = 0; col < size; col++) {
            Tile[] mergedTiles = new Tile[size];
            for (int row = size - 1; row >= 0; row--) {
                Tile tile = this.board.tile(col, row);
                if (tile != null && validMove(tile, mergedTiles, col, row)) {
                    changed = true;
                }
            }
        }

        this.board.setViewingPerspective(Side.NORTH);


        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }
    /** Helper method for tilt.
     * while tile is not on the top place, if:
     *  1. the upper tile is null, move toward it.
     *  2. the upper tile has the same value of tile, move toward it.
     * Return true if tile is moved.*/

    public boolean validMove(Tile tile, Tile[] merged, int pcol, int prow) {
        int size = this.board.size();
        int curRow = prow;
        while (curRow < (size - 1)) {
            int nextRow = curRow + 1;
            Tile upperTile = this.board.tile(pcol, nextRow);
            if (upperTile != null) { // If the upper tile is not null.
                if (upperTile.value() == tile.value() && !tileIsMerged(upperTile, merged)) { // And if upper tile is equal to the tile.
                    this.board.move(pcol, nextRow, tile); // Move toward it.
                    merged[nextRow] = this.board.tile(pcol, nextRow);
                    this.score += this.board.tile(pcol, nextRow).value();
                    return true;
                } else { // if upper tile is unequal to the tile.
                    this.board.move(pcol, curRow, tile); // Move tile right under to the upper tile.
                    return curRow != prow; // if curCol != col, means tile is moved, return true, false otherwise.
                }
            } else {
                if (nextRow == size - 1) { // If reach to the top
                    this.board.move(pcol, nextRow, tile); // Move to the top
                    return true;
                }
            }
            curRow = nextRow;
        }
        return false;
    }

//    public boolean validMove(Tile tile, Tile[] merged) {
//        int size = this.board.size();
//        int col = tile.col();
//        int row = tile.row();
//        int curRow = row;
//        while (curRow < (size - 1)) {
//            int nextRow = curRow + 1;
//            Tile upperTile = this.board.tile(col, nextRow);
//            if (upperTile != null) { // If the upper tile is not null.
//                if (upperTile.value() == tile.value() && !tileIsMerged(upperTile, merged)) { // And if upper tile is equal to the tile.
//                    this.board.move(col, nextRow, tile); // Move toward it.
//                    merged[nextRow] = this.board.tile(col, nextRow);
//                    this.score += this.board.tile(col, nextRow).value();
//                    return true;
//                } else { // if upper tile is unequal to the tile.
//                    this.board.move(col, curRow, tile); // Move tile right under to the upper tile.
//                    return curRow != row; // if curCol != col, means tile is moved, return true, false otherwise.
//                }
//            } else {
//                if (nextRow == size - 1) { // If reach to the top
//                    this.board.move(col, nextRow, tile); // Move to the top
//                    return true;
//                }
//            }
//            curRow = nextRow;
//        }
//        return false;
//    }

    public static boolean tileIsMerged(Tile tile, Tile[] merged) {
        for (Tile t: merged) {
            if (tile == t) {
                return true;
            }
        }
        return false;
    }
    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        int size = b.size();
        for (int col = 0; col < size; col++) {
            for (int row = 0; row < size; row++) {
                Tile tile = b.tile(col, row);
                if (tile == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        int size = b.size();
        for (int col = 0; col < size; col++) {
            for (int row = 0; row < size; row++) {
                Tile tile = b.tile(col, row);
                if (tile != null && tile.value() == MAX_PIECE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if there are any two neighboring tiles
     * on Board b with equal values.
     */
    public static boolean adjacentEqualTileExists(Board b) {
        int size = b.size();
        for (int col = 0; col < size; col++) {
            for (int row = 0; row < size; row++) {
                Tile tile = b.tile(col, row);
                if (hasSameNeighbor(tile, b)) {
                    return true;
                }
            }
        }
        return false;
    }
    /** Just a helper method 1 for adjacentEqualTileExists*/
    private static boolean hasSameNeighbor(Tile tile, Board b) {
        // 定义上下左右四个方向的偏移量
        int[] colOffsets = {-1, 1, 0, 0};  // 上下左右的列偏移量
        int[] rowOffsets = {0, 0, -1, 1};  // 上下左右的行偏移量

        // 遍历上下左右
        for (int i = 0; i < 4; i++) {
            int newCol = tile.col() + colOffsets[i];
            int newRow = tile.row() + rowOffsets[i];
            if (isValidCoordinate(newCol, newRow, b.size()) && b.tile(newCol, newRow).value() == tile.value()) {
                return true;
            }
        }
        return false;
    }
    /** Still a helper method 2 for adjacentEqualTileExists*/
    private static boolean isValidCoordinate(int col, int row, int size) {
        return col >= 0 && col < size && row >= 0 && row < size;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        // If there is at least one empty space or two adjacent neighbor tiles on the board.
        return (emptySpaceExists(b) || (adjacentEqualTileExists(b)));
    }

    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
