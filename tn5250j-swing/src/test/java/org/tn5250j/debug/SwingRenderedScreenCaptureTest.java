package org.tn5250j.debug;

import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.session.api.OiaModel;
import org.tn5250j.session.api.ScreenArea;
import org.tn5250j.session.api.ScreenListener;
import org.tn5250j.session.api.ScreenModel;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SwingRenderedScreenCaptureTest {

    @Test
    public void captureIncludesHiddenCharsInRawTextButNotVisibleText() {
        StubScreen screen = new StubScreen(1, 3);
        screen.text[0] = 'P';
        screen.text[1] = '*';
        screen.text[2] = 'W';
        screen.extended[1] = (char) TN5250jConstants.EXTENDED_5250_NON_DSP;
        screen.color[0] = TN5250jConstants.ATTR_32;
        screen.color[1] = TN5250jConstants.ATTR_40;
        screen.color[2] = TN5250jConstants.ATTR_34;

        SwingRenderedScreen.Snapshot snapshot = SwingRenderedScreen.capture(screen);

        assertEquals("P W", snapshot.text);
        assertEquals("P*W", snapshot.rawText);
        assertEquals("010", snapshot.render.hidden);
        assertEquals("green", snapshot.render.foreground[0]);
        assertEquals("red", snapshot.render.foreground[1]);
        assertEquals("white", snapshot.render.foreground[2]);
        assertNull(snapshot.planes);
    }

    @Test
    public void captureCanIncludeEncodedPlanes() {
        StubScreen screen = new StubScreen(1, 1);
        screen.text[0] = 'A';

        SwingRenderedScreen.Snapshot snapshot = SwingRenderedScreen.capture(screen, true);

        assertEquals(7, snapshot.planes.size());
        assertTrue(snapshot.planes.containsKey("text"));
        assertTrue(snapshot.planes.containsKey("color"));
    }

    private static final class StubScreen implements ScreenModel {
        private final int rows;
        private final int cols;
        private final char[] text;
        private final char[] isAttr;
        private final char[] attr;
        private final char[] color;
        private final char[] extended;
        private final char[] graphic;
        private final char[] field;

        private StubScreen(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            int size = rows * cols;
            text = new char[size];
            isAttr = new char[size];
            attr = new char[size];
            color = new char[size];
            extended = new char[size];
            graphic = new char[size];
            field = new char[size];
        }

        @Override
        public int getRows() {
            return rows;
        }

        @Override
        public int getColumns() {
            return cols;
        }

        @Override
        public int getScreenLength() {
            return rows * cols;
        }

        @Override
        public int getCurrentRow() {
            return 1;
        }

        @Override
        public int getCurrentCol() {
            return 1;
        }

        @Override
        public int getCurrentPos() {
            return 0;
        }

        @Override
        public int getRow(int pos) {
            return pos / cols;
        }

        @Override
        public int getCol(int pos) {
            return pos % cols;
        }

        @Override
        public int getPos(int row, int col) {
            return row * cols + col;
        }

        @Override
        public boolean isCursorActive() {
            return true;
        }

        @Override
        public void setCursorActive(boolean activate) {
        }

        @Override
        public boolean isUsingGuiInterface() {
            return false;
        }

        @Override
        public void setUseGUIInterface(boolean gui) {
        }

        @Override
        public void toggleGUIInterface() {
        }

        @Override
        public void setResetRequired(boolean reset) {
        }

        @Override
        public void setBackspaceError(boolean onError) {
        }

        @Override
        public boolean moveCursor(int pos) {
            return false;
        }

        @Override
        public void setCursor(int row, int col) {
        }

        @Override
        public void sendKeys(String text) {
        }

        @Override
        public void pasteText(String content, boolean special) {
        }

        @Override
        public void sendAid(int aidKey) {
        }

        @Override
        public void repaintScreen() {
        }

        @Override
        public boolean checkHotSpots() {
            return false;
        }

        @Override
        public int GetScreen(char[] buffer, int bufferLength, int plane) {
            char[] source;
            switch (plane) {
                case TN5250jConstants.PLANE_TEXT:
                    source = text;
                    break;
                case TN5250jConstants.PLANE_IS_ATTR_PLACE:
                    source = isAttr;
                    break;
                case TN5250jConstants.PLANE_ATTR:
                    source = attr;
                    break;
                case TN5250jConstants.PLANE_COLOR:
                    source = color;
                    break;
                case TN5250jConstants.PLANE_EXTENDED:
                    source = extended;
                    break;
                case TN5250jConstants.PLANE_EXTENDED_GRAPHIC:
                    source = graphic;
                    break;
                case TN5250jConstants.PLANE_FIELD:
                    source = field;
                    break;
                default:
                    return 0;
            }
            System.arraycopy(source, 0, buffer, 0, Math.min(bufferLength, source.length));
            return source.length;
        }

        @Override
        public int GetScreenRect(char[] buffer, int bufferLength, int startRow, int startCol,
                                 int endRow, int endCol, int plane) {
            return 0;
        }

        @Override
        public char[] getScreenAsChars() {
            return text.clone();
        }

        @Override
        public OiaModel getOia() {
            return null;
        }

        @Override
        public void addScreenListener(ScreenListener listener) {
        }

        @Override
        public void removeScreenListener(ScreenListener listener) {
        }

        @Override
        public String copyText(ScreenArea area) {
            return "";
        }

        @Override
        public String copyTextField(int position) {
            return "";
        }

        @Override
        public List<Double> sumThem(boolean formatOption, ScreenArea area) {
            return Collections.emptyList();
        }

        @Override
        public boolean isInField(int pos, boolean chgToField) {
            return false;
        }
    }
}
