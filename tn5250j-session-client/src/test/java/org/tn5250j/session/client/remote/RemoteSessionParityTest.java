package org.tn5250j.session.client.remote;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.framework.tn5250.Tn5250TestHarness;
import org.tn5250j.session.api.ConnectionProfile;
import org.tn5250j.session.api.SessionStateConstants;
import org.tn5250j.session.wire.ScreenPlaneCodec;
import org.tn5250j.session.wire.ScreenSnapshotDto;
import org.tn5250j.session.wire.WsEnvelope;
import org.tn5250j.session.wire.WsMessageType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Asserts remote-session parity with monolith behavior for paste, copy, and hotspots. */
public class RemoteSessionParityTest {

    @Test
    public void checkHotSpotsDetectsFunctionKeyLabelsLikeMonolith() {
        Tn5250TestHarness harness = connectedHarness();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 5),
                harness.wtdText(5, 1, "F3=Exit"))));
        assertTrue("Monolith baseline: F-key labels are hotspots", harness.screen().checkHotSpots());

        RemoteScreenModel remote = remoteFromScreen(harness.screen());
        assertTrue("Remote checkHotSpots should scan buffered text like Screen5250",
                remote.checkHotSpots());
    }

    @Test
    public void copyTextFieldReturnsCurrentFieldTextLikeMonolith() {
        Tn5250TestHarness harness = connectedHarness();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 10))));
        Screen5250 screen = harness.screen();
        screen.pasteText("HELLO", false);

        int fieldPos = screen.getPos(0, 0);
        assertEquals("Monolith baseline: copy field text", "HELLO     ", screen.copyTextField(fieldPos));

        RemoteScreenModel remote = remoteFromScreen(screen);
        assertEquals("Remote copyTextField should return server-side field text",
                "HELLO     ", remote.copyTextField(fieldPos));
    }

    @Test
    public void pasteTextHonorsSpecialCharacterFilteringLikeMonolith() {
        Tn5250TestHarness harness = connectedHarness();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 10))));
        Screen5250 screen = harness.screen();
        RemoteScreenModel remote = remoteFromScreen(screen);

        screen.pasteText("A@B", true);
        assertEquals("Monolith baseline: special paste filters punctuation", "AB ",
                harness.screenTextAt(1, 2, 3));

        remote.pasteText("A@B", true);
        assertEquals("Remote pasteText(special=true) should filter non-alphanumeric characters",
                "AB ", remoteFieldText(remote, 1, 2, 3));
    }

    @Test
    public void pasteTextSpecialFlagChangesBehaviorLikeMonolith() {
        Tn5250TestHarness harness = connectedHarness();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 10))));
        Screen5250 screen = harness.screen();
        screen.pasteText("A@B", true);
        String specialPaste = harness.screenTextAt(1, 2, 3);

        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 10))));
        screen.pasteText("A@B", false);
        String normalPaste = harness.screenTextAt(1, 2, 3);
        assertNotEquals("Monolith baseline: special flag changes pasted content",
                specialPaste, normalPaste);

        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 10))));
        RemoteScreenModel specialRemote = remoteFromScreen(harness.screen());
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 10))));
        RemoteScreenModel normalRemote = remoteFromScreen(harness.screen());
        specialRemote.pasteText("A@B", true);
        normalRemote.pasteText("A@B", false);
        assertNotEquals("Remote pasteText should honor the special flag",
                remoteFieldText(specialRemote, 1, 2, 3),
                remoteFieldText(normalRemote, 1, 2, 3));
    }

    @Test
    public void pasteTextHandlesCarriageReturnLikeMonolith() {
        Tn5250TestHarness harness = connectedHarness();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdInputField(1, 1, 10),
                harness.wtdInputField(2, 1, 10))));
        Screen5250 screen = harness.screen();
        RemoteScreenModel remote = remoteFromScreen(screen);

        screen.setCursor(1, 2);
        screen.pasteText("A\nB", false);
        assertEquals("Monolith baseline: first row", "A  ", harness.screenTextAt(1, 2, 3));
        assertEquals("Monolith baseline: wrapped to next row", "B  ", harness.screenTextAt(2, 2, 3));

        remote.setCursor(1, 2);
        remote.pasteText("A\nB", false);
        assertEquals("Remote pasteText should wrap at CR/LF like Screen5250.pasteText",
                "A  ", remoteFieldText(remote, 1, 2, 3));
        assertEquals("Remote pasteText should wrap at CR/LF like Screen5250.pasteText",
                "B  ", remoteFieldText(remote, 2, 2, 3));
    }

    @Test
    public void getAllocatedDeviceNameExposesServerAllocatedName() {
        Map<String, String> props = new HashMap<>();
        props.put("SESSION_HOST", "127.0.0.1");
        ConnectionProfile profile = ConnectionProfile.remote(
                "ws://127.0.0.1:65535", "", "remote-parity", props);
        RemoteSessionClient client = new RemoteSessionClient(profile);

        JsonObject payload = new JsonObject();
        payload.addProperty("state", SessionStateConstants.STATE_CONNECTED);
        payload.addProperty("allocatedDeviceName", "TERM01");
        client.onMessage(WsEnvelope.event(WsMessageType.SESSION_STATE_CHANGED, "session-1", payload));

        assertEquals("Remote sessions should expose the server-allocated device name for tab titles",
                "TERM01", client.getAllocatedDeviceName());
    }

    private static Tn5250TestHarness connectedHarness() {
        Tn5250TestHarness harness = Tn5250TestHarness.create();
        harness.unlockKeyboard();
        harness.markConnected();
        return harness;
    }

    private static RemoteScreenModel remoteFromScreen(Screen5250 screen) {
        return remoteFromScreen(screen, text -> { });
    }

    private static RemoteScreenModel remoteFromScreen(Screen5250 screen, java.util.function.Consumer<String> sendKeysFn) {
        RemoteScreenModel remote = new RemoteScreenModel(sendKeysFn, pos -> { }, aid -> { }, () -> { });
        remote.applySnapshot(snapshotFromScreen(screen));
        return remote;
    }

    private static ScreenSnapshotDto snapshotFromScreen(Screen5250 screen) {
        ScreenSnapshotDto snapshot = new ScreenSnapshotDto();
        snapshot.setRows(screen.getRows());
        snapshot.setCols(screen.getColumns());
        snapshot.setCurrentRow(screen.getCurrentRow());
        snapshot.setCurrentCol(screen.getCurrentCol());
        snapshot.setCursorActive(screen.isCursorActive());
        snapshot.setPlanes(encodeAllPlanes(screen));
        return snapshot;
    }

    private static Map<String, String> encodeAllPlanes(Screen5250 screen) {
        int size = screen.getScreenLength();
        Map<String, String> planes = new HashMap<>();
        planes.put("text", encodePlane(screen, size, TN5250jConstants.PLANE_TEXT));
        planes.put("attr", encodePlane(screen, size, TN5250jConstants.PLANE_ATTR));
        planes.put("isAttr", encodePlane(screen, size, TN5250jConstants.PLANE_IS_ATTR_PLACE));
        planes.put("color", encodePlane(screen, size, TN5250jConstants.PLANE_COLOR));
        planes.put("extended", encodePlane(screen, size, TN5250jConstants.PLANE_EXTENDED));
        planes.put("graphic", encodePlane(screen, size, TN5250jConstants.PLANE_EXTENDED_GRAPHIC));
        planes.put("field", encodePlane(screen, size, TN5250jConstants.PLANE_FIELD));
        return planes;
    }

    private static String encodePlane(Screen5250 screen, int size, int plane) {
        char[] buffer = new char[size];
        screen.GetScreen(buffer, size, plane);
        return ScreenPlaneCodec.encodeChars(buffer);
    }

    private static String remoteFieldText(RemoteScreenModel remote, int row, int col, int length) {
        char[] region = new char[length];
        int copied = remote.GetScreenRect(region, region.length, row, col, row, col + length - 1,
                org.tn5250j.session.api.ScreenPlaneConstants.PLANE_TEXT);
        assertTrue("Expected field text in remote buffer", copied >= length);
        return new String(region, 0, length);
    }
}
