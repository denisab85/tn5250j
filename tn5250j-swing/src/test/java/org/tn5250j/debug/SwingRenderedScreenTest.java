package org.tn5250j.debug;

import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import static org.junit.Assert.assertEquals;

public class SwingRenderedScreenTest {

    @Test
    public void renderCellMapsDupHiddenAndGuiBorderCharacters() {
        assertEquals("*", SwingRenderedScreen.renderCell((char) 0x1C, (char) 0, (char) 0, (char) 0, (char) 0));
        assertEquals(" ", SwingRenderedScreen.renderCell('A', (char) 1, (char) 0, (char) 0, (char) 0));
        assertEquals(" ", SwingRenderedScreen.renderCell('A', (char) 0, (char) 0,
                (char) TN5250jConstants.EXTENDED_5250_NON_DSP, (char) 0));
        assertEquals(" ", SwingRenderedScreen.renderCell((char) 0, (char) 0, (char) 0, (char) 0, (char) 0));
        assertEquals("+", SwingRenderedScreen.renderCell('.', (char) 0, (char) 0, (char) 0,
                (char) TN5250jConstants.UPPER_LEFT));
        assertEquals("-", SwingRenderedScreen.renderCell('.', (char) 0, (char) 0, (char) 0,
                (char) TN5250jConstants.UPPER));
        assertEquals("|", SwingRenderedScreen.renderCell(':', (char) 0, (char) 0, (char) 0,
                (char) TN5250jConstants.GUI_LEFT));
        assertEquals("Z", SwingRenderedScreen.renderCell('Z', (char) 0, (char) 0, (char) 0, (char) 0));
    }

    @Test
    public void renderCellKeepsUnderlinedCharactersVisible() {
        char underline = (char) TN5250jConstants.EXTENDED_5250_UNDERLINE;
        assertEquals(" ", SwingRenderedScreen.renderCell(' ', (char) 0, (char) 0, underline, (char) 0));
        assertEquals("T", SwingRenderedScreen.renderCell('T', (char) 0, (char) 0, underline, (char) 0));
        assertEquals("E", SwingRenderedScreen.renderCell('E', (char) 0, (char) 36, (char) 0, (char) 0));
        assertEquals(" ", SwingRenderedScreen.renderCell('T', (char) 1, (char) 0, underline, (char) 0));
    }

    @Test
    public void formatBooleanGridUsesOnesAndZerosWithNewlinesBetweenRows() {
        assertEquals("1100001", SwingRenderedScreen.formatBooleanGrid(
                new boolean[] {true, true, false, false, false, false, true}, 1, 7));
        assertEquals("10\n01", SwingRenderedScreen.formatBooleanGrid(
                new boolean[] {true, false, false, true}, 2, 2));
    }

    @Test
    public void xorCursorColorMatchesSwingLineCursor() {
        SwingCursorPresentation presentation = new SwingCursorPresentation(
                "line",
                java.awt.Color.WHITE,
                java.awt.Color.BLACK);
        assertEquals("white", presentation.color);
        assertEquals("black", presentation.xorBase);
        assertEquals("white", presentation.effectiveColor);
    }
}
