package org.tn5250j.debug;

import org.tn5250j.TN5250jConstants;
import org.tn5250j.framework.tn5250.ScreenColorNames;
import org.tn5250j.framework.tn5250.ScreenPresentation;
import org.tn5250j.session.api.ScreenModel;

import java.util.Map;

/**
 * Captures the 5250 screen as Swing renders it, plus per-cell presentation metadata
 * derived from the same planes {@link org.tn5250j.GuiGraphicBuffer} reads when painting.
 */
public final class SwingRenderedScreen {

    private static final char DUP_CHAR = '*';

    private SwingRenderedScreen() {
    }

    public static Snapshot capture(ScreenModel screen) {
        return capture(screen, false, null);
    }

    public static Snapshot capture(ScreenModel screen, boolean includePlanes) {
        return capture(screen, includePlanes, null);
    }

    public static Snapshot capture(ScreenModel screen, boolean includePlanes, CursorState cursor) {
        int rows = screen.getRows();
        int cols = screen.getColumns();
        int size = rows * cols;

        char[] text = new char[size];
        char[] isAttr = new char[size];
        char[] attr = new char[size];
        char[] color = new char[size];
        char[] extended = new char[size];
        char[] graphic = new char[size];
        char[] field = new char[size];

        screen.GetScreen(text, size, TN5250jConstants.PLANE_TEXT);
        screen.GetScreen(isAttr, size, TN5250jConstants.PLANE_IS_ATTR_PLACE);
        screen.GetScreen(attr, size, TN5250jConstants.PLANE_ATTR);
        screen.GetScreen(color, size, TN5250jConstants.PLANE_COLOR);
        screen.GetScreen(extended, size, TN5250jConstants.PLANE_EXTENDED);
        screen.GetScreen(graphic, size, TN5250jConstants.PLANE_EXTENDED_GRAPHIC);
        screen.GetScreen(field, size, TN5250jConstants.PLANE_FIELD);

        StringBuilder visibleGrid = new StringBuilder(size + rows);
        StringBuilder rawGrid = new StringBuilder(size + rows);
        String[] foreground = new String[size];
        String[] background = new String[size];
        boolean[] hidden = new boolean[size];
        boolean[] underline = new boolean[size];
        boolean[] columnSeparator = new boolean[size];
        boolean[] attributePosition = new boolean[size];
        boolean[] reverseVideo = new boolean[size];
        boolean[] cursorGrid = new boolean[size];
        int[] attrCode = new int[size];
        int[] guiType = new int[size];
        int[] fieldCode = new int[size];

        if (cursor != null && cursor.visible && cursor.row >= 1 && cursor.col >= 1) {
            int cursorPos = (cursor.row - 1) * cols + (cursor.col - 1);
            if (cursorPos >= 0 && cursorPos < size) {
                cursorGrid[cursorPos] = true;
            }
        }

        for (int row = 0; row < rows; row++) {
            if (row > 0) {
                visibleGrid.append('\n');
                rawGrid.append('\n');
            }
            for (int col = 0; col < cols; col++) {
                int pos = row * cols + col;
                visibleGrid.append(renderCell(text[pos], isAttr[pos], attr[pos], extended[pos], graphic[pos]));
                rawGrid.append(rawTextCell(text[pos]));
                foreground[pos] = ScreenColorNames.foregroundName(ScreenColorNames.foregroundIndex(color[pos]));
                background[pos] = ScreenColorNames.backgroundName(ScreenColorNames.backgroundIndex(color[pos]));
                hidden[pos] = isNonDisplay(extended[pos]);
                underline[pos] = ScreenPresentation.isCellUnderlined(attr[pos], extended[pos]);
                columnSeparator[pos] = (extended[pos] & TN5250jConstants.EXTENDED_5250_COL_SEP) != 0;
                attributePosition[pos] = isAttributePlace(isAttr[pos]);
                reverseVideo[pos] = isReverseVideo(attr[pos]);
                attrCode[pos] = attr[pos] & 0xFF;
                guiType[pos] = graphic[pos] & 0xFF;
                fieldCode[pos] = field[pos] & 0xFF;
            }
        }

        Map<String, String> planes = includePlanes ? ScreenModelPlaneEncoder.encodeAllPlanes(screen) : null;

        int cursorRow = cursor != null ? cursor.row : screen.getCurrentRow();
        int cursorCol = cursor != null ? cursor.col : screen.getCurrentCol();
        boolean cursorVisible = cursor != null ? cursor.visible : screen.isCursorActive();
        SwingCursorPresentation cursorPresentation = cursor != null ? cursor.presentation : null;

        return new Snapshot(
                rows,
                cols,
                visibleGrid.toString(),
                rawGrid.toString(),
                new RenderInfo(
                        foreground,
                        background,
                        formatBooleanGrid(hidden, rows, cols),
                        formatBooleanGrid(underline, rows, cols),
                        formatBooleanGrid(columnSeparator, rows, cols),
                        formatBooleanGrid(attributePosition, rows, cols),
                        formatBooleanGrid(reverseVideo, rows, cols),
                        formatBooleanGrid(cursorGrid, rows, cols),
                        attrCode,
                        guiType,
                        fieldCode),
                planes,
                cursorRow,
                cursorCol,
                cursorVisible,
                cursorPresentation,
                screen.isUsingGuiInterface());
    }

    static String renderCell(char text, char isAttrPlane, char attrPlane, char extendedPlane, char graphicPlane) {
        if (isAttributePlace(isAttrPlane)) {
            return " ";
        }
        if (isNonDisplay(extendedPlane)) {
            return " ";
        }
        if (text == 0x0) {
            return " ";
        }
        if (text == 0x1C) {
            return String.valueOf(DUP_CHAR);
        }

        int whichGui = graphicPlane & 0xFF;
        boolean useGui = whichGui != 0;
        if (useGui && whichGui < TN5250jConstants.FIELD_LEFT) {
            return String.valueOf(mapGuiBorder(text, whichGui));
        }
        return String.valueOf(printable(text));
    }

    private static char rawTextCell(char text) {
        if (text == 0x0) {
            return ' ';
        }
        if (text == 0x1C) {
            return DUP_CHAR;
        }
        return printable(text);
    }

    private static char mapGuiBorder(char text, int whichGui) {
        switch (whichGui) {
            case TN5250jConstants.UPPER_LEFT:
                return text == '.' ? '+' : printable(text);
            case TN5250jConstants.UPPER:
                return text == '.' ? '-' : printable(text);
            case TN5250jConstants.UPPER_RIGHT:
                return text == '.' ? '+' : printable(text);
            case TN5250jConstants.GUI_LEFT:
            case TN5250jConstants.GUI_RIGHT:
                return text == ':' ? '|' : printable(text);
            case TN5250jConstants.LOWER_LEFT:
            case TN5250jConstants.LOWER_RIGHT:
                return text == ':' ? '+' : printable(text);
            case TN5250jConstants.BOTTOM:
                return text == '.' ? '-' : printable(text);
            default:
                return printable(text);
        }
    }

    private static char printable(char character) {
        return character >= 32 && character < 127 ? character : '.';
    }

    private static boolean isAttributePlace(char isAttrPlane) {
        return (isAttrPlane & 0xFF) != 0;
    }

    private static boolean isNonDisplay(char extendedPlane) {
        return (extendedPlane & TN5250jConstants.EXTENDED_5250_NON_DSP) != 0;
    }

    private static boolean isReverseVideo(char attrPlane) {
        int attr = attrPlane & 0xFF;
        return attr >= 32 && attr <= 63 && (attr & 1) == 1;
    }

    static String formatBooleanGrid(boolean[] values, int rows, int cols) {
        StringBuilder grid = new StringBuilder(values.length + rows);
        for (int row = 0; row < rows; row++) {
            if (row > 0) {
                grid.append('\n');
            }
            int rowStart = row * cols;
            for (int col = 0; col < cols; col++) {
                grid.append(values[rowStart + col] ? '1' : '0');
            }
        }
        return grid.toString();
    }

    public static final class CursorState {
        public final int row;
        public final int col;
        public final boolean visible;
        public final SwingCursorPresentation presentation;

        public CursorState(int row, int col, boolean visible, SwingCursorPresentation presentation) {
            this.row = row;
            this.col = col;
            this.visible = visible;
            this.presentation = presentation;
        }
    }

    public static final class RenderInfo {
        public final String[] foreground;
        public final String[] background;
        public final String hidden;
        public final String underline;
        public final String columnSeparator;
        public final String attributePosition;
        public final String reverseVideo;
        public final String cursor;
        public final int[] attr;
        public final int[] gui;
        public final int[] field;

        public RenderInfo(String[] foreground, String[] background, String hidden, String underline,
                          String columnSeparator, String attributePosition, String reverseVideo, String cursor,
                          int[] attr, int[] gui, int[] field) {
            this.foreground = foreground;
            this.background = background;
            this.hidden = hidden;
            this.underline = underline;
            this.columnSeparator = columnSeparator;
            this.attributePosition = attributePosition;
            this.reverseVideo = reverseVideo;
            this.cursor = cursor;
            this.attr = attr;
            this.gui = gui;
            this.field = field;
        }
    }

    public static final class Snapshot {
        public final int rows;
        public final int cols;
        public final String text;
        public final String rawText;
        public final RenderInfo render;
        public final Map<String, String> planes;
        public final int cursorRow;
        public final int cursorCol;
        public final boolean cursorVisible;
        public final SwingCursorPresentation cursorPresentation;
        public final boolean guiMode;

        public Snapshot(int rows, int cols, String text, String rawText, RenderInfo render,
                        Map<String, String> planes, int cursorRow, int cursorCol,
                        boolean cursorVisible, SwingCursorPresentation cursorPresentation, boolean guiMode) {
            this.rows = rows;
            this.cols = cols;
            this.text = text;
            this.rawText = rawText;
            this.render = render;
            this.planes = planes;
            this.cursorRow = cursorRow;
            this.cursorCol = cursorCol;
            this.cursorVisible = cursorVisible;
            this.cursorPresentation = cursorPresentation;
            this.guiMode = guiMode;
        }
    }
}
