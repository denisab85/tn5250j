package org.tn5250j.session.client.remote;

import org.junit.Test;
import org.tn5250j.session.api.ScreenListener;
import org.tn5250j.session.wire.ScreenPlaneCodec;
import org.tn5250j.session.wire.ScreenSnapshotDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RemoteScreenModelTest {

    private static final class RecordingScreenListener implements ScreenListener {
        private final List<int[]> updates = new ArrayList<>();

        @Override
        public void onScreenChanged(int inUpdate, int startRow, int startCol, int endRow, int endCol) {
            updates.add(new int[] {inUpdate, startRow, startCol, endRow, endCol});
        }

        @Override
        public void onScreenSizeChanged(int rows, int cols) {
        }
    }

    @Test
    public void getScreenRectUsesOneBasedCoordinates() {
        RemoteScreenModel model = new RemoteScreenModel(keys -> { }, pos -> { }, aid -> { }, () -> { });
        ScreenSnapshotDto snapshot = new ScreenSnapshotDto();
        snapshot.setRows(24);
        snapshot.setCols(80);
        char[] text = new char[24 * 80];
        text[0] = 'A';
        text[79] = 'B';
        Map<String, String> planes = new HashMap<>();
        planes.put("text", ScreenPlaneCodec.encodeChars(text));
        snapshot.setPlanes(planes);
        model.applySnapshot(snapshot);

        char[] region = new char[80];
        int copied = model.GetScreenRect(region, region.length, 1, 1, 1, 80,
                org.tn5250j.session.api.ScreenPlaneConstants.PLANE_TEXT);

        assertEquals(80, copied);
        assertEquals('A', region[0]);
        assertEquals('B', region[79]);
    }

    @Test
    public void applyRegionRelaysCursorAfterScreenUpdateWhenCursorActive() {
        RemoteScreenModel model = new RemoteScreenModel(keys -> { }, pos -> { }, aid -> { }, () -> { });
        RecordingScreenListener listener = new RecordingScreenListener();
        model.addScreenListener(listener);

        model.applyRegion(1, 0, 0, 23, 79, 6, 53, true, new HashMap<String, String>());

        assertEquals(2, listener.updates.size());
        assertEquals(1, listener.updates.get(0)[0]);
        assertEquals(3, listener.updates.get(1)[0]);
        assertEquals(5, listener.updates.get(1)[1]);
        assertEquals(52, listener.updates.get(1)[2]);
    }

    @Test
    public void applyRegionRelaysOldAndNewCursorWhenPositionChanges() {
        RemoteScreenModel model = new RemoteScreenModel(keys -> { }, pos -> { }, aid -> { }, () -> { });
        RecordingScreenListener listener = new RecordingScreenListener();
        model.addScreenListener(listener);

        model.applyRegion(1, 5, 50, 5, 55, 6, 52, true, new HashMap<String, String>());
        listener.updates.clear();

        model.applyRegion(1, 5, 50, 5, 55, 6, 53, true, new HashMap<String, String>());

        assertEquals(3, listener.updates.size());
        assertEquals(1, listener.updates.get(0)[0]);
        assertEquals(3, listener.updates.get(1)[0]);
        assertEquals(5, listener.updates.get(1)[1]);
        assertEquals(51, listener.updates.get(1)[2]);
        assertEquals(3, listener.updates.get(2)[0]);
        assertEquals(5, listener.updates.get(2)[1]);
        assertEquals(52, listener.updates.get(2)[2]);
    }

    @Test
    public void applyRegionDoesNotRelayCursorWhenUpdateIsAlreadyCursorEvent() {
        RemoteScreenModel model = new RemoteScreenModel(keys -> { }, pos -> { }, aid -> { }, () -> { });
        RecordingScreenListener listener = new RecordingScreenListener();
        model.addScreenListener(listener);

        model.applyRegion(3, 5, 52, 5, 52, 6, 53, true, new HashMap<String, String>());

        assertEquals(1, listener.updates.size());
        assertEquals(3, listener.updates.get(0)[0]);
    }

    @Test
    public void applySnapshotPreservesActiveCursorFromLiveUpdates() {
        RemoteScreenModel model = new RemoteScreenModel(keys -> { }, pos -> { }, aid -> { }, () -> { });
        model.applyRegion(3, 5, 52, 5, 52, 6, 53, true, new HashMap<String, String>());

        ScreenSnapshotDto staleSnapshot = new ScreenSnapshotDto();
        staleSnapshot.setRows(24);
        staleSnapshot.setCols(80);
        staleSnapshot.setCurrentRow(6);
        staleSnapshot.setCurrentCol(53);
        staleSnapshot.setCursorActive(false);
        staleSnapshot.setPlanes(new HashMap<String, String>());
        model.applySnapshot(staleSnapshot);

        assertTrue(model.isCursorActive());
    }
}
