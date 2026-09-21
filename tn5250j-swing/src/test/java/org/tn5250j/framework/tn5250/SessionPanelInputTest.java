package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.tn5250j.SessionConfig;
import org.tn5250j.SessionPanel;
import org.tn5250j.SessionScroller;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.keyboard.KeyMapper;
import org.tn5250j.tools.LangTool;

import javax.swing.Action;
import javax.swing.JButton;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Swing SessionPanel / keypad / scroller input into the session screen and VT.
 */
public class SessionPanelInputTest {

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
    public void sessionPanelKeyPressedEnterSendsAidAndConsumesEvent() {
        SessionPanel panel = headlessPanel();
        KeyEvent event = keyPressed(panel, KeyEvent.VK_ENTER, 0, KeyEvent.CHAR_UNDEFINED);

        panel.processKeyEvent(event);

        assertTrue(event.isConsumed());
        assertEquals(TN5250jConstants.AID_ENTER, harness.lastOutboundAid());
    }

    @Test
    public void sessionPanelKeyPressedMappedPfKeySendsAidAndConsumesEvent() {
        SessionPanel panel = headlessPanel();
        KeyEvent event = keyPressed(panel, KeyEvent.VK_F3, 0, KeyEvent.CHAR_UNDEFINED);
        KeyMapper.setKeyStroke("[pf3]", event);
        resetKeyMapperCache(panel);

        panel.processKeyEvent(event);

        assertTrue(event.isConsumed());
        assertEquals(TN5250jConstants.AID_PF3, harness.lastOutboundAid());
    }

    @Test
    public void sessionPanelKeyTypedCharacterUpdatesCurrentFieldAndConsumesEvent() {
        SessionPanel panel = headlessPanel();
        KeyEvent event = keyTyped(panel, 'Z');

        panel.processKeyEvent(event);

        assertTrue(event.isConsumed());
        assertEquals("Z", harness.screenTextAt(1, 2, 1));
        assertTrue(screen.getScreenFields().isCurrentFieldModified());
    }

    @Test
    public void linuxAltGrStateIsPassedToKeyMapperPath() {
        SessionPanel panel = headlessPanel();
        KeyEvent altGraph = keyPressed(panel, KeyEvent.VK_ALT_GRAPH, InputEvent.ALT_GRAPH_DOWN_MASK,
                KeyEvent.CHAR_UNDEFINED);
        KeyEvent mapped = keyPressed(panel, KeyEvent.VK_Q, InputEvent.ALT_GRAPH_DOWN_MASK,
                KeyEvent.CHAR_UNDEFINED);

        KeyMapper.setKeyStroke("[pf4]", mapped, true);
        resetKeyMapperCache(panel);
        try {
            panel.processKeyEvent(altGraph);
            panel.processKeyEvent(mapped);

            assertTrue(mapped.isConsumed());
            assertEquals(TN5250jConstants.AID_PF4, harness.lastOutboundAid());
        } finally {
            KeyMapper.setKeyStroke("[pf4]", keyPressed(panel, KeyEvent.VK_F4, 0, KeyEvent.CHAR_UNDEFINED));
            resetKeyMapperCache(panel);
        }
    }

    @Test
    public void keypadButtonActionCommandSendsKeysToScreen() {
        harness.session().getConfiguration().setProperty(SessionConfig.CONFIG_KEYPAD_ENABLED, SessionConfig.YES);
        SessionPanel panel = headlessPanel();
        JButton pf5 = findKeypadButton(panel, "[pf5]");

        assertNotNull(pf5);
        pf5.doClick();

        assertEquals(TN5250jConstants.AID_PF5, harness.lastOutboundAid());
    }

    @Test
    public void pasteTextCrossesFromUiPasteTargetIntoFields() {
        screen.pasteText("PASTE", false);

        assertEquals("PASTE", harness.screenTextAt(1, 2, 5));
        assertTrue(screen.getScreenFields().isCurrentFieldModified());
    }

    @Test
    public void keyboardHandlerDispatchesMacroNameInsteadOfSendingKeys() {
        RecordingSessionPanel panel = new RecordingSessionPanel(harness.session());
        panel.setRunningHeadless(true);
        KeyEvent macroKey = keyPressed(panel, KeyEvent.VK_F9, 0, KeyEvent.CHAR_UNDEFINED);

        KeyMapper.setKeyStroke("unit-test-macro", macroKey);
        resetKeyMapperCache(panel);
        try {
            panel.processKeyEvent(macroKey);

            assertTrue(macroKey.isConsumed());
            assertEquals("unit-test-macro", panel.executedMacro);
            assertEquals(0, harness.outboundLength());
        } finally {
            KeyMapper.setKeyStroke("[pf9]", macroKey);
            resetKeyMapperCache(panel);
        }
    }

    @Test
    public void displayMessagesEmulatorActionUsesSystemRequestControllerPath() {
        SessionPanel panel = headlessPanel();
        Action action = panel.getActionMap().get("[dspmsgs]");

        assertNotNull(action);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "[dspmsgs]"));

        assertEquals(0, harness.lastOutboundOpcode());
        assertEquals(4, harness.lastOutboundFlags());
        assertEquals(harness.ebcdic('4') & 0xff, harness.outboundByte(10));
    }

    @Test
    public void sessionScrollerMouseWheelSendsPageAids() throws Exception {
        SessionScroller scroller = scrollerForScreen();

        scroller.mouseWheelMoved(mouseWheelEvent(-1));
        assertEquals(TN5250jConstants.AID_ROLL_UP, harness.lastOutboundAid());

        harness.clearOutbound();
        harness.unlockKeyboard();
        scroller.mouseWheelMoved(mouseWheelEvent(1));

        assertEquals(TN5250jConstants.AID_ROLL_DOWN, harness.lastOutboundAid());
    }

    @Test
    public void sessionPanelDoubleClickSendsEnterAidWhenEnabled() {
        harness.session().getConfiguration().setProperty("doubleClick", SessionConfig.YES);
        SessionPanel panel = headlessPanel();

        clickPanel(panel, doubleClickEvent(panel));

        assertEquals(3, harness.lastOutboundOpcode());
        assertEquals(TN5250jConstants.AID_ENTER, harness.lastOutboundAid());
    }

    private SessionPanel headlessPanel() {
        SessionPanel panel = new SessionPanel(harness.session());
        panel.setRunningHeadless(true);
        return panel;
    }

    private KeyEvent keyPressed(Component source, int keyCode, int modifiers, char keyChar) {
        return new KeyEvent(source, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode,
                keyChar, KeyEvent.KEY_LOCATION_STANDARD);
    }

    private KeyEvent keyTyped(Component source, char keyChar) {
        return new KeyEvent(source, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED,
                keyChar, KeyEvent.KEY_LOCATION_UNKNOWN);
    }

    private void resetKeyMapperCache(Component source) {
        KeyMapper.getKeyStrokeMnemonic(keyPressed(source, KeyEvent.VK_F8, 0, KeyEvent.CHAR_UNDEFINED));
    }

    private JButton findKeypadButton(Container container, String actionCommand) {
        for (Component child : container.getComponents()) {
            if (child instanceof JButton && actionCommand.equals(((JButton) child).getActionCommand())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                JButton found = findKeypadButton((Container) child, actionCommand);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private SessionScroller scrollerForScreen() throws Exception {
        SessionPanel panel = headlessPanel();
        SessionScroller scroller = new SessionScroller();
        Field screenField = SessionScroller.class.getDeclaredField("screen");
        screenField.setAccessible(true);
        screenField.set(scroller, panel.getScreen());
        return scroller;
    }

    private MouseWheelEvent mouseWheelEvent(int rotation) {
        return new MouseWheelEvent(
                new Canvas(),
                MouseWheelEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,
                1,
                1,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                rotation);
    }

    private MouseEvent doubleClickEvent(SessionPanel panel) {
        return new MouseEvent(
                panel,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                1,
                1,
                1,
                1,
                2,
                false,
                MouseEvent.BUTTON1);
    }

    private void clickPanel(SessionPanel panel, MouseEvent event) {
        for (MouseListener listener : panel.getMouseListeners()) {
            listener.mouseClicked(event);
        }
    }

    private static final class RecordingSessionPanel extends SessionPanel {
        private String executedMacro;

        private RecordingSessionPanel(org.tn5250j.Session5250 session) {
            super(session);
        }

        @Override
        public void executeMacro(String macro) {
            executedMacro = macro;
        }
    }
}
