package org.tn5250j.tools.logging;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formats decoded screen plane data for debug logging.
 */
public final class ScreenPlaneDebugFormatter {

    private static final String[] PLANE_ORDER = {
            "text", "attr", "isAttr", "color", "extended", "graphic", "field"
    };
    private static final int LARGE_REGION_CELLS = 16;

    private ScreenPlaneDebugFormatter() {
    }

    public static String formatEncodedRegion(int cols, Map<String, String> encoded) {
        Map<String, char[]> decoded = decodeAll(encoded);
        return formatRegion(cols, decoded);
    }

    public static String formatDetailForLog(int cols, Map<String, String> encoded,
                                            int cellCount, int inUpdate) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        return formatDetailForDecodedLog(cols, decodeAll(encoded), cellCount, inUpdate);
    }

    public static String formatDetailForDecodedLog(int cols, Map<String, char[]> decoded,
                                                   int cellCount, int inUpdate) {
        if (decoded == null || decoded.isEmpty()) {
            return "";
        }
        if (cellCount == 1 || inUpdate == 3 || inUpdate == 4) {
            return formatCompact(decoded);
        }
        if (cellCount > LARGE_REGION_CELLS) {
            return formatTextOnlyRegion(cols, decoded);
        }
        return formatRegion(cols, decoded);
    }

    public static String formatRegion(int cols, Map<String, char[]> planes) {
        if (planes == null || planes.isEmpty()) {
            return "";
        }
        StringBuilder message = new StringBuilder();
        for (String planeName : PLANE_ORDER) {
            char[] data = planes.get(planeName);
            if (data == null || data.length == 0) {
                continue;
            }
            message.append(planeName).append(":\n");
            message.append(formatPlane(planeName, data, cols));
        }
        return message.toString();
    }

    public static String formatCompact(Map<String, char[]> planes) {
        if (planes == null || planes.isEmpty()) {
            return "";
        }
        StringBuilder compact = new StringBuilder();
        for (String planeName : PLANE_ORDER) {
            char[] data = planes.get(planeName);
            if (data == null || data.length == 0) {
                continue;
            }
            if (compact.length() > 0) {
                compact.append(' ');
            }
            compact.append(planeName).append('=');
            if ("text".equals(planeName)) {
                compact.append('"').append(escapeTextValue(data)).append('"');
            } else if (data.length == 1) {
                compact.append(String.format("%02x", data[0] & 0xFF));
            } else {
                compact.append('[').append(formatHexInline(data)).append(']');
            }
        }
        return compact.toString();
    }

    private static String formatTextOnlyRegion(int cols, Map<String, char[]> planes) {
        char[] text = planes.get("text");
        if (text == null || text.length == 0) {
            return "text=(empty)";
        }
        StringBuilder message = new StringBuilder("mode=text-only cols=").append(cols).append('\n');
        message.append(formatTextGrid(text, cols));
        return message.toString();
    }

    private static Map<String, char[]> decodeAll(Map<String, String> encoded) {
        Map<String, char[]> decoded = new LinkedHashMap<>();
        if (encoded != null) {
            for (Map.Entry<String, String> entry : encoded.entrySet()) {
                decoded.put(entry.getKey(), decodePlane(entry.getValue()));
            }
        }
        return decoded;
    }

    private static char[] decodePlane(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new char[0];
        }
        byte[] bytes = Base64.getDecoder().decode(base64);
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) (bytes[i] & 0xFF);
        }
        return chars;
    }

    private static String formatPlane(String planeName, char[] data, int cols) {
        int width = cols > 0 ? cols : data.length;
        if ("text".equals(planeName)) {
            return formatTextGrid(data, width);
        }
        return formatHexGrid(data, width);
    }

    private static String formatTextGrid(char[] data, int cols) {
        StringBuilder grid = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0 && i % cols == 0) {
                grid.append('\n');
            }
            char character = data[i];
            grid.append(character >= 32 && character < 127 ? character : '.');
        }
        if (data.length > 0) {
            grid.append('\n');
        }
        return grid.toString();
    }

    private static String formatHexGrid(char[] data, int cols) {
        StringBuilder grid = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                if (i % cols == 0) {
                    grid.append('\n');
                } else {
                    grid.append(' ');
                }
            }
            grid.append(String.format("%02x", data[i] & 0xFF));
        }
        if (data.length > 0) {
            grid.append('\n');
        }
        return grid.toString();
    }

    private static String formatHexInline(char[] data) {
        StringBuilder inline = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                inline.append(' ');
            }
            inline.append(String.format("%02x", data[i] & 0xFF));
        }
        return inline.toString();
    }

    private static String escapeTextValue(char[] data) {
        StringBuilder text = new StringBuilder();
        for (char character : data) {
            if (character == '\\' || character == '"') {
                text.append('\\');
            }
            if (character >= 32 && character < 127) {
                text.append(character);
            } else if (character == '\n') {
                text.append("\\n");
            } else if (character == '\r') {
                text.append("\\r");
            } else if (character == '\t') {
                text.append("\\t");
            } else {
                text.append('.');
            }
        }
        return text.toString();
    }
}
