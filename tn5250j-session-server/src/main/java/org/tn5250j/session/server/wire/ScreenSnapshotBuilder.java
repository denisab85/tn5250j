package org.tn5250j.session.server.wire;

import org.tn5250j.Session5250;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.framework.tn5250.ScreenOIA;
import org.tn5250j.session.wire.OiaStateDto;
import org.tn5250j.session.wire.ScreenSnapshotDto;

public final class ScreenSnapshotBuilder {

    private ScreenSnapshotBuilder() {
    }

    public static ScreenSnapshotDto build(Session5250 session) {
        Screen5250 screen = session.getScreen();
        ScreenSnapshotDto dto = new ScreenSnapshotDto();
        dto.setRows(screen.getRows());
        dto.setCols(screen.getColumns());
        dto.setCurrentRow(screen.getCurrentRow());
        dto.setCurrentCol(screen.getCurrentCol());
        dto.setCursorActive(screen.isCursorActive());
        dto.setUsingGuiInterface(screen.isUsingGuiInterface());
        dto.setAllocatedDeviceName(session.getAllocatedDeviceName());
        dto.setOia(buildOia(screen.getOIA()));
        dto.setPlanes(ScreenPlaneEncoder.encodeAllPlanes(screen));
        return dto;
    }

    public static OiaStateDto buildOia(ScreenOIA oia) {
        OiaStateDto dto = new OiaStateDto(oia.isInsertMode(), oia.getLevel(), oia.getInputInhibited(),
                oia.getInhibitedText());
        dto.setKeysBuffered(oia.isKeysBuffered());
        dto.setScriptActive(oia.isScriptActive());
        dto.setMessageWait(oia.isMessageWait());
        return dto;
    }
}
