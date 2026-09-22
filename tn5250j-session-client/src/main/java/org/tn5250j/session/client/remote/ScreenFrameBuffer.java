package org.tn5250j.session.client.remote;

import org.tn5250j.session.api.ScreenPlaneConstants;
import org.tn5250j.session.wire.ScreenPlaneCodec;
import org.tn5250j.session.wire.ScreenSnapshotDto;

import java.util.Map;

final class ScreenFrameBuffer {

    private int rows = 24;
    private int cols = 80;
    private int currentRow = 1;
    private int currentCol = 1;
    private boolean cursorActive;
    private boolean usingGuiInterface;
    private char[] text = new char[0];
    private char[] attr = new char[0];
    private char[] isAttr = new char[0];
    private char[] color = new char[0];
    private char[] extended = new char[0];
    private char[] graphic = new char[0];
    private char[] field = new char[0];

    void applySnapshot(ScreenSnapshotDto snapshot) {
        rows = snapshot.getRows();
        cols = snapshot.getCols();
        currentRow = snapshot.getCurrentRow();
        currentCol = snapshot.getCurrentCol();
        cursorActive = snapshot.isCursorActive();
        usingGuiInterface = snapshot.isUsingGuiInterface();
        resize(rows, cols);
        applyPlanes(snapshot.getPlanes(), 0, 0, rows - 1, cols - 1);
    }

    void applyRegion(int startRow, int startCol, int endRow, int endCol,
                     int newCurrentRow, int newCurrentCol, boolean newCursorActive,
                     Map<String, String> planes) {
        currentRow = newCurrentRow;
        currentCol = newCurrentCol;
        cursorActive = newCursorActive;
        applyPlanes(planes, startRow, startCol, endRow, endCol);
    }

    void resize(int newRows, int newCols) {
        rows = newRows;
        cols = newCols;
        int size = rows * cols;
        text = resizePlane(text, size);
        attr = resizePlane(attr, size);
        isAttr = resizePlane(isAttr, size);
        color = resizePlane(color, size);
        extended = resizePlane(extended, size);
        graphic = resizePlane(graphic, size);
        field = resizePlane(field, size);
    }

    int getRows() {
        return rows;
    }

    int getCols() {
        return cols;
    }

    int getCurrentRow() {
        return currentRow;
    }

    int getCurrentCol() {
        return currentCol;
    }

    boolean isCursorActive() {
        return cursorActive;
    }

    void setCursorActive(boolean active) {
        cursorActive = active;
    }

    boolean isUsingGuiInterface() {
        return usingGuiInterface;
    }

    void setUsingGuiInterface(boolean usingGuiInterface) {
        this.usingGuiInterface = usingGuiInterface;
    }

    int getScreenLength() {
        return rows * cols;
    }

    int getPos(int row, int col) {
        return row * cols + col;
    }

    int getRow(int pos) {
        return pos / cols;
    }

    int getCol(int pos) {
        return pos % cols;
    }

    int getScreen(char[] buffer, int bufferLength, int plane) {
        char[] source = planeData(plane);
        int len = Math.min(bufferLength, source.length);
        System.arraycopy(source, 0, buffer, 0, len);
        return len;
    }

    int getScreenRect(char[] buffer, int bufferLength, int startRow, int startCol,
                      int endRow, int endCol, int plane) {
        char[] source = planeData(plane);
        int index = 0;
        for (int row = startRow; row <= endRow; row++) {
            int startPos = getPos(row, startCol);
            int length = endCol - startCol + 1;
            if (index + length > bufferLength) {
                length = bufferLength - index;
            }
            System.arraycopy(source, startPos, buffer, index, length);
            index += length;
            if (index >= bufferLength) {
                break;
            }
        }
        return index;
    }

    private char[] planeData(int plane) {
        switch (plane) {
            case ScreenPlaneConstants.PLANE_TEXT:
                return text;
            case ScreenPlaneConstants.PLANE_ATTR:
                return attr;
            case ScreenPlaneConstants.PLANE_IS_ATTR_PLACE:
                return isAttr;
            case ScreenPlaneConstants.PLANE_COLOR:
                return color;
            case ScreenPlaneConstants.PLANE_EXTENDED:
                return extended;
            case ScreenPlaneConstants.PLANE_EXTENDED_GRAPHIC:
                return graphic;
            case ScreenPlaneConstants.PLANE_FIELD:
                return field;
            default:
                return new char[getScreenLength()];
        }
    }

    private void applyPlanes(Map<String, String> planes, int startRow, int startCol,
                             int endRow, int endCol) {
        if (planes == null) {
            return;
        }
        putPlane("text", planes, startRow, startCol, endRow, endCol, text);
        putPlane("attr", planes, startRow, startCol, endRow, endCol, attr);
        putPlane("isAttr", planes, startRow, startCol, endRow, endCol, isAttr);
        putPlane("color", planes, startRow, startCol, endRow, endCol, color);
        putPlane("extended", planes, startRow, startCol, endRow, endCol, extended);
        putPlane("graphic", planes, startRow, startCol, endRow, endCol, graphic);
        putPlane("field", planes, startRow, startCol, endRow, endCol, field);
    }

    private void putPlane(String key, Map<String, String> planes, int startRow, int startCol,
                          int endRow, int endCol, char[] target) {
        if (!planes.containsKey(key)) {
            return;
        }
        char[] decoded = ScreenPlaneCodec.decode(planes.get(key));
        if (startRow == 0 && startCol == 0 && endRow == rows - 1 && endCol == cols - 1) {
            if (decoded.length == target.length) {
                System.arraycopy(decoded, 0, target, 0, decoded.length);
            }
            return;
        }
        int index = 0;
        for (int row = startRow; row <= endRow; row++) {
            int destPos = getPos(row, startCol);
            int length = endCol - startCol + 1;
            System.arraycopy(decoded, index, target, destPos, Math.min(length, decoded.length - index));
            index += length;
        }
    }

    private static char[] resizePlane(char[] existing, int size) {
        char[] resized = new char[size];
        System.arraycopy(existing, 0, resized, 0, Math.min(existing.length, size));
        return resized;
    }
}
