package org.tn5250j.debug;

import org.tn5250j.TN5250jConstants;
import org.tn5250j.session.api.ScreenModel;
import org.tn5250j.session.wire.ScreenPlaneCodec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encodes all queryable 5250 screen planes from a {@link ScreenModel}.
 */
public final class ScreenModelPlaneEncoder {

    private ScreenModelPlaneEncoder() {
    }

    public static Map<String, String> encodeAllPlanes(ScreenModel screen) {
        int size = screen.getScreenLength();
        Map<String, String> planes = new LinkedHashMap<>();
        planes.put("text", encodePlane(screen, size, TN5250jConstants.PLANE_TEXT));
        planes.put("attr", encodePlane(screen, size, TN5250jConstants.PLANE_ATTR));
        planes.put("isAttr", encodePlane(screen, size, TN5250jConstants.PLANE_IS_ATTR_PLACE));
        planes.put("color", encodePlane(screen, size, TN5250jConstants.PLANE_COLOR));
        planes.put("extended", encodePlane(screen, size, TN5250jConstants.PLANE_EXTENDED));
        planes.put("graphic", encodePlane(screen, size, TN5250jConstants.PLANE_EXTENDED_GRAPHIC));
        planes.put("field", encodePlane(screen, size, TN5250jConstants.PLANE_FIELD));
        return planes;
    }

    private static String encodePlane(ScreenModel screen, int size, int plane) {
        char[] buffer = new char[size];
        screen.GetScreen(buffer, size, plane);
        return ScreenPlaneCodec.encodeChars(buffer);
    }
}
