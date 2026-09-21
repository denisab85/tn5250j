package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.tn5250j.GuiGraphicBuffer;
import org.tn5250j.SessionPanel;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.event.ScreenOIAListener;
import org.tn5250j.event.SessionChangeEvent;
import org.tn5250j.event.SessionListener;
import org.tn5250j.tools.LangTool;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Session / screen / OIA events consumed by view listeners (e.g. GuiGraphicBuffer).
 */
public class SessionEventFanoutTest {

    private Tn5250TestHarness harness;
    private Screen5250 screen;

    @Before
    public void setUp() {
        LangTool.init();
        harness = Tn5250TestHarness.create();
        screen = harness.screen();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 8))));
        harness.unlockKeyboard();
        harness.markConnected();
        harness.clearOutbound();
    }

    @Test
    public void sessionPanelRegistersGuiGraphicBufferForScreenAndOiaCallbacks() throws Exception {
        SessionPanel panel = new SessionPanel(harness.session());
        drainEventQueue();

        assertTrue(hasListener(screenListeners(), GuiGraphicBuffer.class));
        assertTrue(hasListener(oiaListeners(), GuiGraphicBuffer.class));

        harness.process(harness.outputOnly(TN5250jConstants.CMD_CLEAR_UNIT_ALTERNATE, (byte) 0));
        drainEventQueue();

        assertEquals(27, screen.getRows());
        assertEquals(132, screen.getColumns());
        panel.setRunningHeadless(true);
        drainEventQueue();
    }

    @Test
    public void screenOiaFiresKeyboardInsertBufferedClearInhibitedAndBellChanges() {
        RecordingOIAListener listener = new RecordingOIAListener();
        screen.getOIA().addOIAListener(listener);

        screen.getOIA().setKeyBoardLocked(true);
        screen.getOIA().setKeyBoardLocked(false);
        screen.getOIA().setInsertMode(true);
        screen.getOIA().setInsertMode(false);
        screen.getOIA().setKeysBuffered(true);
        screen.getOIA().setKeysBuffered(false);
        screen.clearScreen();
        screen.getOIA().setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                ScreenOIA.OIA_LEVEL_INPUT_INHIBITED, "X - test");
        screen.getOIA().setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                ScreenOIA.OIA_LEVEL_NOT_INHIBITED);
        screen.getOIA().setAudibleBell();

        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED));
        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_INSERT_MODE));
        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_KEYS_BUFFERED));
        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_CLEAR_SCREEN));
        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED));
        assertTrue(listener.changes.contains(ScreenOIAListener.OIA_CHANGED_BELL));
        assertFalse(screen.getOIA().isKeyBoardLocked());
        assertFalse(screen.getOIA().isInsertMode());
        assertFalse(screen.getOIA().isKeysBuffered());
    }

    @Test
    public void sessionControllerFiresDisconnectAndRemoveCallbacksConsumedByViews() {
        RecordingSessionListener listener = new RecordingSessionListener();
        harness.session().addSessionListener(listener);

        harness.vt().disconnect();
        harness.session().fireSessionChanged(TN5250jConstants.STATE_REMOVE);

        assertTrue(listener.states.contains(TN5250jConstants.STATE_DISCONNECTED));
        assertTrue(listener.states.contains(TN5250jConstants.STATE_REMOVE));
    }

    private Vector<?> screenListeners() throws Exception {
        Field listeners = Screen5250.class.getDeclaredField("screenListeners");
        listeners.setAccessible(true);
        return (Vector<?>) listeners.get(screen);
    }

    private Vector<?> oiaListeners() throws Exception {
        Field listeners = ScreenOIA.class.getDeclaredField("listeners");
        listeners.setAccessible(true);
        return (Vector<?>) listeners.get(screen.getOIA());
    }

    private boolean hasListener(Vector<?> listeners, Class<?> listenerType) {
        if (listeners == null) {
            return false;
        }
        for (Object listener : listeners) {
            if (listenerType.isInstance(listener)) {
                return true;
            }
        }
        return false;
    }

    private void drainEventQueue() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // Drain pending resize/repaint work created by headless Swing components.
            }
        });
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
