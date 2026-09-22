package org.tn5250j.session.client.remote;

import org.junit.Test;
import org.tn5250j.session.wire.ScreenPlaneCodec;
import org.tn5250j.session.wire.ScreenSnapshotDto;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ScreenFrameBufferTest {

    @Test
    public void applySnapshotAllocatesPlaneBuffers() {
        ScreenFrameBuffer buffer = new ScreenFrameBuffer();
        ScreenSnapshotDto snapshot = new ScreenSnapshotDto();
        snapshot.setRows(24);
        snapshot.setCols(80);
        snapshot.setCurrentRow(1);
        snapshot.setCurrentCol(1);
        snapshot.setCursorActive(true);
        Map<String, String> planes = new HashMap<>();
        char[] text = new char[24 * 80];
        text[0] = 'A';
        planes.put("text", ScreenPlaneCodec.encodeChars(text));
        snapshot.setPlanes(planes);

        buffer.applySnapshot(snapshot);

        char[] decoded = new char[24 * 80];
        assertEquals(24 * 80, buffer.getScreen(decoded, decoded.length,
                org.tn5250j.session.api.ScreenPlaneConstants.PLANE_TEXT));
        assertEquals('A', decoded[0]);
    }

    @Test
    public void getScreenRectReadsZeroBasedRegion() {
        ScreenFrameBuffer buffer = new ScreenFrameBuffer();
        ScreenSnapshotDto snapshot = new ScreenSnapshotDto();
        snapshot.setRows(24);
        snapshot.setCols(80);
        char[] text = new char[24 * 80];
        text[0] = 'A';
        text[79] = 'B';
        text[80] = 'C';
        Map<String, String> planes = new HashMap<>();
        planes.put("text", ScreenPlaneCodec.encodeChars(text));
        snapshot.setPlanes(planes);
        buffer.applySnapshot(snapshot);

        char[] region = new char[80];
        int copied = buffer.getScreenRect(region, region.length, 0, 0, 0, 79,
                org.tn5250j.session.api.ScreenPlaneConstants.PLANE_TEXT);

        assertEquals(80, copied);
        assertEquals('A', region[0]);
        assertEquals('B', region[79]);
    }

    @Test
    public void getScreenRectClampsOutOfBoundsCoordinates() {
        ScreenFrameBuffer buffer = new ScreenFrameBuffer();
        ScreenSnapshotDto snapshot = new ScreenSnapshotDto();
        snapshot.setRows(24);
        snapshot.setCols(80);
        char[] text = new char[24 * 80];
        text[23 * 80] = 'Z';
        Map<String, String> planes = new HashMap<>();
        planes.put("text", ScreenPlaneCodec.encodeChars(text));
        snapshot.setPlanes(planes);
        buffer.applySnapshot(snapshot);

        char[] region = new char[80];
        int copied = buffer.getScreenRect(region, region.length, 23, 0, 24, 79,
                org.tn5250j.session.api.ScreenPlaneConstants.PLANE_TEXT);

        assertEquals(80, copied);
        assertEquals('Z', region[0]);
    }

    @Test
    public void applySnapshotCopiesPartialPlaneDataWhenLengthsDiffer() {
        ScreenFrameBuffer buffer = new ScreenFrameBuffer();
        ScreenSnapshotDto snapshot = new ScreenSnapshotDto();
        snapshot.setRows(24);
        snapshot.setCols(80);
        char[] text = new char[24 * 80];
        text[0] = 'X';
        Map<String, String> planes = new HashMap<>();
        planes.put("text", ScreenPlaneCodec.encodeChars(text));
        snapshot.setPlanes(planes);
        buffer.applySnapshot(snapshot);

        char[] decoded = new char[24 * 80];
        buffer.getScreen(decoded, decoded.length,
                org.tn5250j.session.api.ScreenPlaneConstants.PLANE_TEXT);
        assertEquals('X', decoded[0]);
    }
}
