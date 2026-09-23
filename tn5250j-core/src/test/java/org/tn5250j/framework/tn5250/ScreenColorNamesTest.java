package org.tn5250j.framework.tn5250;

import org.junit.Test;
import org.tn5250j.TN5250jConstants;

import static org.junit.Assert.assertEquals;

public class ScreenColorNamesTest {

    @Test
    public void namesStandard5250Colors() {
        assertEquals("green", ScreenColorNames.foregroundName(TN5250jConstants.COLOR_FG_GREEN));
        assertEquals("black", ScreenColorNames.backgroundName(TN5250jConstants.COLOR_BG_BLACK));
        assertEquals("lightRed", ScreenColorNames.foregroundName(TN5250jConstants.COLOR_FG_LIGHT_RED));
    }

    @Test
    public void extractsForegroundAndBackgroundFromColorPlane() {
        char color = TN5250jConstants.ATTR_40;
        assertEquals(TN5250jConstants.COLOR_FG_RED, ScreenColorNames.foregroundIndex(color));
        assertEquals(TN5250jConstants.COLOR_BG_BLACK, ScreenColorNames.backgroundIndex(color));
    }
}
