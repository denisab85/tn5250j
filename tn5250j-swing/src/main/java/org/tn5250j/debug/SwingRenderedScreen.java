package org.tn5250j.debug;

import org.tn5250j.TN5250jConstants;
import org.tn5250j.framework.tn5250.ScreenPresentation;
import org.tn5250j.session.api.ScreenModel;

/**
 * Captures the 5250 screen as Swing renders it, expressed as plain text.
 * Applies the same visibility rules as {@link org.tn5250j.GuiGraphicBuffer#drawChar}.
 */
public final class SwingRenderedScreen {

    private static final char DUP_CHAR = '*';
    /** Visible marker for 5250 underline attribute (matches email/print export). */
    private static final char UNDERLINE_CHAR = '_';

    private SwingRenderedScreen() {
    }

    public static Snapshot capture(ScreenModel screen) {
        int rows = screen.getRows();
        int cols = screen.getColumns();
        int size = rows * cols;

        char[] text = new char[size];
        char[] isAttr = new char[size];
        char[] attr = new char[size];
        char[] extended = new char[size];
        char[] graphic = new char[size];

        screen.GetScreen(text, size, TN5250jConstants.PLANE_TEXT);
        screen.GetScreen(isAttr, size, TN5250jConstants.PLANE_IS_ATTR_PLACE);
        screen.GetScreen(attr, size, TN5250jConstants.PLANE_ATTR);
        screen.GetScreen(extended, size, TN5250jConstants.PLANE_EXTENDED);
        screen.GetScreen(graphic, size, TN5250jConstants.PLANE_EXTENDED_GRAPHIC);

        StringBuilder grid = new StringBuilder(size + rows);
        for (int row = 0; row < rows; row++) {
            if (row > 0) {
                grid.append('\n');
            }
            for (int col = 0; col < cols; col++) {
                int pos = row * cols + col;
                grid.append(renderCell(text[pos], isAttr[pos], attr[pos], extended[pos], graphic[pos]));
            }
        }

        return new Snapshot(
                rows,
                cols,
                grid.toString(),
                screen.getCurrentRow(),
                screen.getCurrentCol(),
                screen.isCursorActive(),
                screen.isUsingGuiInterface());
    }

    static String renderCell(char text, char isAttrPlane, char attrPlane, char extendedPlane, char graphicPlane) {
        if (isAttributePlace(isAttrPlane)) {
            return " ";
        }
        boolean underlined = ScreenPresentation.isCellUnderlined(attrPlane, extendedPlane);
        if (isNonDisplay(extendedPlane)) {
            return underlined ? "_" : " ";
        }
        if (text == 0x0) {
            return underlined ? "_" : " ";
        }
        if (text == 0x1C) {
            return String.valueOf(DUP_CHAR);
        }

        int whichGui = graphicPlane & 0xFF;
        boolean useGui = whichGui != 0;
        if (useGui && whichGui < TN5250jConstants.FIELD_LEFT) {
            return String.valueOf(mapGuiBorder(text, whichGui));
        }
        if (underlined) {
            return String.valueOf(UNDERLINE_CHAR);
        }
        return String.valueOf(printable(text));
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

    public static final class Snapshot {
        public final int rows;
        public final int cols;
        public final String text;
        public final int cursorRow;
        public final int cursorCol;
        public final boolean cursorVisible;
        public final boolean guiMode;

        public Snapshot(int rows, int cols, String text, int cursorRow, int cursorCol,
                        boolean cursorVisible, boolean guiMode) {
            this.rows = rows;
            this.cols = cols;
            this.text = text;
            this.cursorRow = cursorRow;
            this.cursorCol = cursorCol;
            this.cursorVisible = cursorVisible;
            this.guiMode = guiMode;
        }
    }
}
