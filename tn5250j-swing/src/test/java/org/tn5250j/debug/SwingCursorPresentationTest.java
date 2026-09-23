package org.tn5250j.debug;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;

public class SwingCursorPresentationTest {

    @Test
    public void whiteXorBlackIsWhite() {
        assertEquals("white", SwingCursorPresentation.colorName(
                SwingCursorPresentation.xorColor(Color.WHITE, Color.BLACK)));
    }

    @Test
    public void whiteXorGreenIsMagenta() {
        assertEquals("magenta", SwingCursorPresentation.colorName(
                SwingCursorPresentation.xorColor(Color.WHITE, Color.GREEN)));
    }
}
