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
    public void renderCellMaps5250UnderlineAttributeToUnderscore() {
        char underline = (char) TN5250jConstants.EXTENDED_5250_UNDERLINE;
        assertEquals("_", SwingRenderedScreen.renderCell(' ', (char) 0, (char) 0, underline, (char) 0));
        assertEquals("_", SwingRenderedScreen.renderCell('T', (char) 0, (char) 0, underline, (char) 0));
        assertEquals("_", SwingRenderedScreen.renderCell((char) 0, (char) 0, (char) 0, underline, (char) 0));
        assertEquals(" ", SwingRenderedScreen.renderCell('T', (char) 1, (char) 0, underline, (char) 0));
    }

    @Test
    public void renderCellUsesAttrPlaneWhenExtendedUnderlineMissing() {
        assertEquals("_", SwingRenderedScreen.renderCell('T', (char) 0, (char) 36, (char) 0, (char) 0));
        assertEquals("_", SwingRenderedScreen.renderCell('E', (char) 0, (char) 37, (char) 0, (char) 0));
    }
}
