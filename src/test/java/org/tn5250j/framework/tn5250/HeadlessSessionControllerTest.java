package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.event.ScreenListener;
import org.tn5250j.event.ScreenOIAListener;
import org.tn5250j.event.SessionChangeEvent;
import org.tn5250j.event.SessionListener;
import org.tn5250j.interfaces.HeadlessSessionUiHooks;
import org.tn5250j.interfaces.SessionController;
import org.tn5250j.interfaces.SessionUiHooks;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A client can drive a session and screen through {@link SessionController}
 * without constructing Swing views.
 */
public class HeadlessSessionControllerTest {

    private Tn5250TestHarness harness;
    private Screen5250 screen;

    @Before
    public void setUp() {
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
    public void headlessControllerUsesNoOpHooksAndLifecycleWithoutAGui() {
        Tn5250TestHarness fresh = Tn5250TestHarness.create();
        SessionController controller = fresh.session();

        assertNull(fresh.session().getView());
        assertSame(HeadlessSessionUiHooks.INSTANCE, controller.getUiHooks());
        assertFalse(controller.isConnected());
        assertNull(controller.getHostName());
        assertNull(controller.getAllocatedDeviceName());
        assertEquals("test-session", controller.getSessionName());
        assertSame(fresh.screen(), controller.getScreen());

        controller.getUiHooks().signalBell();
        assertNull(controller.getUiHooks().promptSystemRequest());
        assertFalse(controller.getUiHooks().acceptUntrustedCertificate("untrusted"));

        controller.setUiHooks(null);
        assertSame(HeadlessSessionUiHooks.INSTANCE, controller.getUiHooks());

        fresh.markConnected();
        assertTrue(controller.isConnected());
    }

    @Test
    public void installedHooksReceiveBellAndSystemRequest() {
        RecordingHooks hooks = new RecordingHooks();
        SessionController controller = harness.session();
        controller.setUiHooks(hooks);

        harness.session().signalBell();
        harness.vt().signalBell();
        harness.vt().systemRequest();

        assertEquals(2, hooks.bells);
        assertEquals(1, hooks.prompts);
        assertEquals("9", hooks.sysReqReturned);
        assertEquals(harness.ebcdic('9') & 0xff, harness.outboundByte(10));
    }

    @Test
    public void sendKeysAndCursorRunWithoutSwingListeners() {
        RecordingScreenListener screenListener = new RecordingScreenListener();
        RecordingOiaListener oiaListener = new RecordingOiaListener();
        screen.addScreenListener(screenListener);
        screen.getOIA().addOIAListener(oiaListener);

        screen.sendKeys("AB");
        screen.setCursor(1, 4);

        assertEquals("AB", harness.screenTextAt(1, 2, 2));
        assertEquals(1, screen.getCurrentRow());
        assertEquals(4, screen.getCurrentCol());
        assertTrue(screenListener.updates > 0);

        screen.getOIA().setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                ScreenOIA.OIA_LEVEL_INPUT_INHIBITED, "X - test");
        assertTrue(oiaListener.changes.contains(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED));
    }

    @Test
    public void screenAndSessionListenersAreObservedByAHeadlessClient() {
        RecordingScreenListener screenListener = new RecordingScreenListener();
        RecordingSessionListener sessionListener = new RecordingSessionListener();
        SessionController controller = harness.session();
        screen.addScreenListener(screenListener);
        controller.addSessionListener(sessionListener);

        harness.process(harness.outputOnly(TN5250jConstants.CMD_CLEAR_UNIT_ALTERNATE, (byte) 0));
        harness.session().fireSessionChanged(TN5250jConstants.STATE_REMOVE);

        assertEquals(27, screen.getRows());
        assertEquals(132, screen.getColumns());
        assertTrue(screenListener.sizeChanges > 0);
        assertTrue(sessionListener.states.contains(TN5250jConstants.STATE_REMOVE));

        int delivered = sessionListener.states.size();
        controller.removeSessionListener(sessionListener);
        controller.disconnect();
        assertFalse(controller.isConnected());
        assertEquals(delivered, sessionListener.states.size());
    }

    @Test
    public void inlineDispatcherRunsOnTheCallerThread() {
        final Thread caller = Thread.currentThread();
        final boolean[] sameThread = new boolean[1];
        InlineVtEventDispatcher.INSTANCE.dispatch(new Runnable() {
            @Override
            public void run() {
                sameThread[0] = Thread.currentThread() == caller;
            }
        });
        assertTrue(sameThread[0]);
    }

    private static final class RecordingHooks implements SessionUiHooks {
        private int bells;
        private int prompts;
        private String sysReqReturned;

        @Override
        public void signalBell() {
            bells++;
        }

        @Override
        public String promptSystemRequest() {
            prompts++;
            sysReqReturned = "9";
            return sysReqReturned;
        }

        @Override
        public boolean acceptUntrustedCertificate(String info) {
            return false;
        }
    }

    private static final class RecordingScreenListener implements ScreenListener {
        private int updates;
        private int sizeChanges;

        @Override
        public void onScreenChanged(int inUpdate, int startRow, int startCol, int endRow, int endCol) {
            updates++;
        }

        @Override
        public void onScreenSizeChanged(int rows, int cols) {
            sizeChanges++;
        }
    }

    private static final class RecordingOiaListener implements ScreenOIAListener {
        private final List<Integer> changes = new ArrayList<>();

        @Override
        public void onOIAChanged(ScreenOIA oia, int change) {
            changes.add(change);
        }
    }

    private static final class RecordingSessionListener implements SessionListener {
        private final List<Integer> states = new ArrayList<>();

        @Override
        public void onSessionChanged(SessionChangeEvent changeEvent) {
            states.add(changeEvent.getState());
        }
    }
}
