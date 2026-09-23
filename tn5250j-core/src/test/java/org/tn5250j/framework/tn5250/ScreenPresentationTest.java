package org.tn5250j.framework.tn5250;

import org.junit.Test;
import org.tn5250j.TN5250jConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScreenPresentationTest {

    @Test
    public void detectsUnderlineAttrs() {
        assertTrue(ScreenPresentation.isUnderline5250Attr(36));
        assertTrue(ScreenPresentation.isUnderline5250Attr(62));
        assertFalse(ScreenPresentation.isUnderline5250Attr(32));
    }

    @Test
    public void derivesExtendedUnderlineFromAttrPlane() {
        char extended = ScreenPresentation.underlineExtendedBits((char) 36, (char) 0);
        assertTrue((extended & TN5250jConstants.EXTENDED_5250_UNDERLINE) != 0);
    }
}
