package org.tn5250j;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuiGraphicBufferCursorTest {

    @Test
    public void regionContainsCellDetectsOverlap() {
        assertTrue(GuiGraphicBuffer.regionContainsCell(0, 0, 23, 79, 5, 53));
        assertTrue(GuiGraphicBuffer.regionContainsCell(5, 50, 5, 60, 5, 53));
        assertFalse(GuiGraphicBuffer.regionContainsCell(5, 54, 5, 60, 5, 53));
        assertFalse(GuiGraphicBuffer.regionContainsCell(0, 0, 23, 79, 24, 0));
    }
}
