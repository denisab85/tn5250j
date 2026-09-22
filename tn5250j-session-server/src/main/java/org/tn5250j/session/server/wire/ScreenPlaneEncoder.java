package org.tn5250j.session.server.wire;

import org.tn5250j.TN5250jConstants;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.session.wire.ScreenPlaneCodec;

import java.util.HashMap;
import java.util.Map;

public final class ScreenPlaneEncoder {

    private ScreenPlaneEncoder() {
    }

    public static Map<String, String> encodeAllPlanes(Screen5250 screen) {
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

    public static Map<String, String> encodeRegion(Screen5250 screen, int startRow, int startCol,
                                                   int endRow, int endCol) {
        int size = (endRow - startRow + 1) * (endCol - startCol + 1);
        Map<String, String> planes = new HashMap<>();
        planes.put("text", encodeRegionPlane(screen, size, startRow, startCol, endRow, endCol,
                TN5250jConstants.PLANE_TEXT));
        planes.put("attr", encodeRegionPlane(screen, size, startRow, startCol, endRow, endCol,
                TN5250jConstants.PLANE_ATTR));
        planes.put("isAttr", encodeRegionPlane(screen, size, startRow, startCol, endRow, endCol,
                TN5250jConstants.PLANE_IS_ATTR_PLACE));
        planes.put("color", encodeRegionPlane(screen, size, startRow, startCol, endRow, endCol,
                TN5250jConstants.PLANE_COLOR));
        planes.put("extended", encodeRegionPlane(screen, size, startRow, startCol, endRow, endCol,
                TN5250jConstants.PLANE_EXTENDED));
        planes.put("graphic", encodeRegionPlane(screen, size, startRow, startCol, endRow, endCol,
                TN5250jConstants.PLANE_EXTENDED_GRAPHIC));
        planes.put("field", encodeRegionPlane(screen, size, startRow, startCol, endRow, endCol,
                TN5250jConstants.PLANE_FIELD));
        return planes;
    }

    private static String encodePlane(Screen5250 screen, int size, int plane) {
        char[] buffer = new char[size];
        screen.GetScreen(buffer, size, plane);
        return ScreenPlaneCodec.encodeChars(buffer);
    }

    private static String encodeRegionPlane(Screen5250 screen, int size, int startRow, int startCol,
                                            int endRow, int endCol, int plane) {
        char[] buffer = new char[size];
        // ScreenListener coordinates are 0-based; Screen5250.GetScreenRect is 1-based.
        screen.GetScreenRect(buffer, size, startRow + 1, startCol + 1, endRow + 1, endCol + 1, plane);
        return ScreenPlaneCodec.encodeChars(buffer);
    }
}
