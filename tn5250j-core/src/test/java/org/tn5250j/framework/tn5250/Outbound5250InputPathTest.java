package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.tn5250j.TN5250jConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Outbound5250InputPathTest {

    private Tn5250TestHarness harness;
    private Screen5250 screen;

    @Before
    public void setUp() {
        harness = Tn5250TestHarness.create();
        screen = harness.screen();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 5))));
        harness.unlockKeyboard();
        harness.clearOutbound();
    }

    @Test
    public void sendKeysCharacterUpdatesInputFieldAndMdt() {
        screen.sendKeys("A");

        assertEquals("A", harness.screenTextAt(1, 2, 1));
        assertTrue(screen.getScreenFields().isCurrentFieldModified());
        assertTrue(screen.getScreenFields().isMasterMDT());
    }

    @Test
    public void sendKeysEnterSendsEnterAid() {
        screen.sendKeys("[enter]");

        assertEquals(3, harness.lastOutboundOpcode());
        assertEquals(TN5250jConstants.AID_ENTER, harness.lastOutboundAid());
        assertTrue(screen.getOIA().isKeyBoardLocked());
    }

    @Test
    public void sendKeysPf3SendsPf3Aid() {
        screen.sendKeys("[pf3]");

        assertEquals(3, harness.lastOutboundOpcode());
        assertEquals(TN5250jConstants.AID_PF3, harness.lastOutboundAid());
    }

    @Test
    public void sendKeysPf13SendsPf13Aid() {
        screen.sendKeys("[pf13]");

        assertEquals(3, harness.lastOutboundOpcode());
        assertEquals(TN5250jConstants.AID_PF13, harness.lastOutboundAid());
    }

    @Test
    public void sendKeysAttentionUsesAttentionPath() {
        screen.sendKeys("[attn]");

        assertEquals(0, harness.lastOutboundOpcode());
        assertEquals(0x40, harness.lastOutboundFlags());
        assertFalse(harness.outboundBytes().length == 0);
    }

    @Test
    public void systemRequestStringSendsSysReqOperationWithEbcdicPayload() {
        harness.vt().systemRequest("90");

        assertEquals(0, harness.lastOutboundOpcode());
        assertEquals(4, harness.lastOutboundFlags());
        assertEquals(harness.ebcdic('9') & 0xff, harness.outboundByte(10));
        assertEquals(harness.ebcdic('0') & 0xff, harness.outboundByte(11));
    }

    @Test
    public void sendKeysHelpSendsHelpAid() {
        screen.sendKeys("[help]");

        assertEquals(3, harness.lastOutboundOpcode());
        assertEquals(TN5250jConstants.AID_HELP, harness.lastOutboundAid());
    }

    @Test
    public void sendKeysClearSendsClearAid() {
        screen.sendKeys("[clear]");

        assertEquals(3, harness.lastOutboundOpcode());
        assertEquals(TN5250jConstants.AID_CLEAR, harness.lastOutboundAid());
    }

    @Test
    public void sendKeysPageUpAndPageDownSendRollAids() {
        screen.sendKeys("[pgup]");
        assertEquals(TN5250jConstants.AID_ROLL_UP, harness.lastOutboundAid());

        harness.clearOutbound();
        screen.getOIA().setKeyBoardLocked(false);
        screen.sendKeys("[pgdown]");
        assertEquals(TN5250jConstants.AID_ROLL_DOWN, harness.lastOutboundAid());
    }

    @Test
    public void fieldExitBlanksRemainderOfSeededFieldAndMarksMdt() {
        screen.sendKeys("AB[fldext]");

        assertEquals("AB   ", harness.screenTextAt(1, 2, 5));
        assertTrue(screen.getScreenFields().getField(0).mdt);
        assertTrue(screen.getScreenFields().isMasterMDT());
    }

    @Test
    public void lockedKeyboardBuffersTextButAllowsAttention() {
        screen.getOIA().setKeyBoardLocked(true);
        screen.sendKeys("A");

        assertEquals(0, harness.outboundLength());
        assertEquals(" ", harness.screenTextAt(1, 2, 1));
        assertTrue(screen.getOIA().isKeysBuffered());

        screen.sendKeys("[attn]");

        assertEquals(0, harness.lastOutboundOpcode());
        assertEquals(0x40, harness.lastOutboundFlags());
        assertTrue(screen.getOIA().isKeyBoardLocked());
    }

    @Test
    public void resetWhileLockedIsProcessedInsteadOfBuffered() {
        screen.getOIA().setKeyBoardLocked(true);
        screen.sendKeys("[reset]");

        assertEquals(0, harness.outboundLength());
        assertFalse(screen.getOIA().isKeysBuffered());
    }

    @Test
    public void hotspotFunctionKeyClickSendsMappedPfAid() {
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 5),
                harness.wtdText(5, 1, "F3=Exit"))));
        harness.unlockKeyboard();
        assertTrue(screen.checkHotSpots());

        harness.clearOutbound();
        boolean moved = screen.moveCursor(screen.getPos(4, 0));

        assertFalse(moved);
        assertEquals(TN5250jConstants.AID_PF3, harness.lastOutboundAid());
    }

    @Test
    public void typedFieldDataIsIncludedWithEnterAid() {
        screen.sendKeys("ABC[enter]");

        assertEquals(TN5250jConstants.AID_ENTER, harness.lastOutboundAid());
        assertEquals(harness.ebcdic('A') & 0xff, harness.outboundByte(13));
        assertEquals(harness.ebcdic('B') & 0xff, harness.outboundByte(14));
        assertEquals(harness.ebcdic('C') & 0xff, harness.outboundByte(15));
    }

    @Test
    public void sohDataIncludedBitmapControlsPfKeyFieldData() {
        screen.sendKeys("ABC[pf3]");
        assertEquals(TN5250jConstants.AID_PF3, harness.lastOutboundAid());
        assertTrue(harness.outboundLength() > 15);
        assertEquals(harness.ebcdic('A') & 0xff, harness.outboundByte(13));

        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                new byte[]{TN5250jConstants.CMD_WRITE_TO_DISPLAY, 0, 0},
                harness.sohWithDataIncludedForPfKeys(0, 0, 0x04),
                harness.inputFieldOrders(1, 1, 5))));
        harness.unlockKeyboard();
        harness.clearOutbound();

        screen.sendKeys("ABC[pf3]");

        assertEquals(TN5250jConstants.AID_PF3, harness.lastOutboundAid());
        assertEquals(15, harness.outboundLength());
    }

    @Test
    public void optionHotspotFillsCurrentFieldAndSendsEnterAid() {
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 5),
                harness.wtdText(5, 5, "1. Start"))));
        harness.unlockKeyboard();
        assertTrue(screen.checkHotSpots());

        harness.clearOutbound();
        boolean moved = screen.moveCursor(screen.getPos(4, 4));

        assertFalse(moved);
        assertEquals("1", harness.screenTextAt(1, 2, 1));
        assertTrue(screen.getScreenFields().isCurrentFieldModified());
        assertEquals(TN5250jConstants.AID_ENTER, harness.lastOutboundAid());
    }
}
