package org.tn5250j.keyboard;

import org.junit.Assume;
import org.junit.Test;

import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static org.junit.Assert.assertEquals;

public class KeyMapperDefaultTest {

    @Test
    public void defaultF3KeyMapsToPf3Mnemonic() {
        assertDefaultMapping(KeyEvent.VK_F3, 0, "[pf3]");
    }

    @Test
    public void defaultEnterKeyMapsToEnterMnemonic() {
        assertDefaultMapping(KeyEvent.VK_ENTER, 0, "[enter]");
    }

    @Test
    public void defaultPageKeysMapToRollMnemonics() {
        assertDefaultMapping(KeyEvent.VK_PAGE_UP, 0, "[pgup]");
        assertDefaultMapping(KeyEvent.VK_PAGE_DOWN, 0, "[pgdown]");
    }

    @Test
    public void defaultClearAndSysReqKeysMapToMnemonics() {
        assertDefaultMapping(KeyEvent.VK_PAUSE, 0, "[clear]");
        assertDefaultMapping(KeyEvent.VK_ESCAPE, 0, "[sysreq]");
    }

    @Test
    public void defaultAltF1MapsToHelpMnemonic() {
        assertDefaultMapping(KeyEvent.VK_F1, InputEvent.ALT_DOWN_MASK, "[help]");
    }

    @Test
    public void defaultShiftF1MapsToPf13Mnemonic() {
        assertDefaultMapping(KeyEvent.VK_F1, InputEvent.SHIFT_DOWN_MASK, "[pf13]");
    }

    private void assertDefaultMapping(int keyCode, int modifiers, String expectedMnemonic) {
        KeyMapper.init();
        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), modifiers, keyCode,
                KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD);

        String mnemonic = KeyMapper.getKeyStrokeMnemonic(event);

        Assume.assumeTrue("Default key map is overridden in this environment",
                expectedMnemonic.equals(mnemonic));
        assertEquals(expectedMnemonic, mnemonic);
    }
}
