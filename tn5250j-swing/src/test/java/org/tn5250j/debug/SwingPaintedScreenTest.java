package org.tn5250j.debug;

import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwingPaintedScreenTest {

    @Test
    public void detectsPaintedCursorLineFromRasterPixels() {
        Color[] palette = {Color.BLACK, Color.WHITE, Color.GREEN};
        Color cursorEffective = Color.WHITE;

        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, Color.BLACK.getRGB());
            }
        }
        image.setRGB(10, 18, Color.WHITE.getRGB());

        assertTrue(SwingPaintedScreen.isCursorLinePainted(
                SwingPaintedScreen.sampleRgb(image, 10, 18), cursorEffective, palette));
        assertFalse(SwingPaintedScreen.isCursorLinePainted(
                SwingPaintedScreen.sampleRgb(image, 10, 10), cursorEffective, palette));
    }

    @Test
    public void renderCursorGridLookupIsRowMajor() {
        assertTrue(SwingPaintedScreen.isRenderCursorMarked("000\n010\n000", 1, 1));
        assertFalse(SwingPaintedScreen.isRenderCursorMarked("000\n010\n000", 0, 1));
    }

    @Test
    public void nearestColorNameMapsGreenForeground() {
        Color[] palette = {Color.BLACK, Color.WHITE, Color.GREEN};
        assertEquals("green", SwingPaintedScreen.nearestColorName(new Color(0, 180, 0), palette));
    }
}
