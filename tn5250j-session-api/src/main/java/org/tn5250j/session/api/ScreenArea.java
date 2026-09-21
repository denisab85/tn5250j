package org.tn5250j.session.api;

/**
 * Inclusive screen bounds using the same 1-based row/column convention as {@code Rect}
 * in the core screen (x = start row, y = start column, width/height = span).
 */
public final class ScreenArea {

    private final int startRow;
    private final int startCol;
    private final int endRow;
    private final int endCol;

    private ScreenArea(int startRow, int startCol, int endRow, int endCol) {
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
    }

    public static ScreenArea fromOriginSize(int startRow, int startCol, int width, int height) {
        return new ScreenArea(startRow, startCol, startRow + height - 1, startCol + width - 1);
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndCol() {
        return endCol;
    }
}
