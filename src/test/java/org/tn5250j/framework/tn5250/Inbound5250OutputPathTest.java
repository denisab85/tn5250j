package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.event.ScreenListener;
import org.tn5250j.event.ScreenOIAListener;
import org.tn5250j.event.SessionChangeEvent;
import org.tn5250j.event.SessionListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Inbound5250OutputPathTest {

    private Tn5250TestHarness harness;
    private Screen5250 screen;

    @Before
    public void setUp() {
        harness = Tn5250TestHarness.create();
        screen = harness.screen();
    }

    @Test
    public void clearUnitAndWriteToDisplayShowsEbcdicText() {
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdText(1, 1, "HELLO"))));

        assertEquals("HELLO", harness.screenTextAt(1, 1, 5));
    }

    @Test
    public void writeToDisplayStartFieldCreatesInputField() {
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(2, 10, 6))));

        assertEquals(1, screen.getScreenFields().getSize());
        assertTrue(screen.isInField(1, 10));
        assertFalse(screen.getScreenFields().getCurrentField().isBypassField());
    }

    @Test
    public void hostUpdateFiresScreenChangedListener() {
        RecordingScreenListener listener = new RecordingScreenListener();
        screen.addScreenListener(listener);

        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdText(3, 1, "DIRTY"))));

        assertTrue(listener.changed.size() > 0);
    }

    @Test
    public void messageLightOpcodeUpdatesOiaAndNotifiesListener() {
        RecordingOIAListener listener = new RecordingOIAListener();
        screen.getOIA().addOIAListener(listener);

        harness.process(harness.messageLightOn());

        assertTrue(screen.getOIA().isMessageWait());
        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_MESSAGELIGHT));
    }

    @Test
    public void messageLightOffOpcodeClearsOiaMessageWait() {
        harness.process(harness.messageLightOn());
        assertTrue(screen.getOIA().isMessageWait());

        harness.process(harness.gds(12));

        assertFalse(screen.getOIA().isMessageWait());
    }

    @Test
    public void clearUnitAlternateSwitchesToTwentySevenByOneThirtyTwoAndNotifiesListener() {
        RecordingScreenListener listener = new RecordingScreenListener();
        screen.addScreenListener(listener);

        harness.process(harness.outputOnly(TN5250jConstants.CMD_CLEAR_UNIT_ALTERNATE, (byte) 0));

        assertEquals(27, screen.getRows());
        assertEquals(132, screen.getColumns());
        assertTrue(listener.sizes.contains("27x132"));
    }

    @Test
    public void rollCommandMovesRowsWithinRequestedArea() {
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdText(1, 1, "ROW1"),
                harness.wtdText(2, 1, "ROW2"))));

        harness.process(harness.outputOnly(TN5250jConstants.CMD_ROLL, (byte) 1, (byte) 1, (byte) 2));

        assertEquals("ROW2", harness.screenTextAt(1, 1, 4));
    }

    @Test
    public void setBufferAddressAndInsertCursorAffectScreenAndCursorPosition() {
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                new byte[]{TN5250jConstants.CMD_WRITE_TO_DISPLAY, 0, 0, 0x11, 3, 5},
                new byte[]{harness.ebcdic('P'), harness.ebcdic('O'), harness.ebcdic('S')},
                new byte[]{0x13, 4, 7})));

        assertEquals("POS", harness.screenTextAt(3, 5, 3));
        assertEquals(4, screen.getCurrentRow());
        assertEquals(7, screen.getCurrentCol());
    }

    @Test
    public void writeErrorCodePutsScreenInErrorCodeStatus() {
        harness.process(harness.outputOnly(new byte[]{
                TN5250jConstants.CMD_WRITE_ERROR_CODE,
                harness.ebcdic('E'), harness.ebcdic('R'), harness.ebcdic('R')}));

        assertTrue(screen.isStatusErrorCode());
    }

    @Test
    public void inviteOperationUnlocksKeyboardAfterHostUpdate() {
        RecordingOIAListener listener = new RecordingOIAListener();
        screen.getOIA().addOIAListener(listener);

        harness.process(harness.invite(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdText(1, 1, "READY"))));

        assertFalse(screen.getOIA().isKeyBoardLocked());
        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED));
    }

    @Test
    public void readMdtInviteMarksWaitingForInputAndUnlocksKeyboard() {
        harness.process(harness.invite(TN5250jConstants.CMD_READ_MDT_FIELDS, (byte) 0, (byte) 0));

        assertTrue(harness.vt().waitingForInput());
        assertFalse(screen.getOIA().isKeyBoardLocked());
    }

    @Test
    public void putGetOperationFiresSessionConnectedCallback() {
        RecordingSessionListener listener = new RecordingSessionListener();
        harness.session().addSessionListener(listener);

        harness.process(harness.putGet(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdText(1, 1, "SIGNON"))));

        assertTrue(listener.states.contains(TN5250jConstants.STATE_CONNECTED));
    }

    @Test
    public void writeToDisplayStructuredFieldCreateWindowDrawsGuiPlane() {
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdCreateWindow(4, 10, 2, 8))));

        assertEquals(TN5250jConstants.UPPER_LEFT, harness.guiAt(4, 11));
        assertEquals(TN5250jConstants.UPPER, harness.guiAt(4, 12));
        assertEquals(TN5250jConstants.GUI_LEFT, harness.guiAt(5, 11));
        assertEquals(TN5250jConstants.LOWER_LEFT, harness.guiAt(7, 11));
    }

    private static final class RecordingScreenListener implements ScreenListener {
        private final List<String> changed = new ArrayList<>();
        private final List<String> sizes = new ArrayList<>();

        public void onScreenChanged(int inUpdate, int startRow, int startCol, int endRow, int endCol) {
            changed.add(inUpdate + ":" + startRow + ":" + startCol + ":" + endRow + ":" + endCol);
        }

        public void onScreenSizeChanged(int rows, int cols) {
            sizes.add(rows + "x" + cols);
        }
    }

    private static final class RecordingOIAListener implements ScreenOIAListener {
        private final List<Integer> changes = new ArrayList<>();

        public void onOIAChanged(ScreenOIA oia, int change) {
            changes.add(change);
        }
    }

    private static final class RecordingSessionListener implements SessionListener {
        private final List<Integer> states = new ArrayList<>();

        public void onSessionChanged(SessionChangeEvent changeEvent) {
            states.add(changeEvent.getState());
        }
    }
}
