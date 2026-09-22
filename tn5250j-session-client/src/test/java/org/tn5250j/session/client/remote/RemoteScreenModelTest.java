package org.tn5250j.session.client.remote;

import org.junit.Test;
import org.tn5250j.session.wire.ScreenPlaneCodec;
import org.tn5250j.session.wire.ScreenSnapshotDto;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RemoteScreenModelTest {

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
}
