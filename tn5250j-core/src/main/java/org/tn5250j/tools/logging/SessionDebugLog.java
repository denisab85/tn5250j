package org.tn5250j.tools.logging;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug logging for session input and screen plane updates on client and server.
 * <p>
 * With {@link #enableFullPlaneLogging()}, screen events emit a single-line {@code SDEBUG}
 * summary (easy to grep) and optional detail blocks for plane contents. Detail dumps are
 * emitted only at canonical wire/session phases ({@code changed}, {@code send},
 * {@code received}) to avoid triplicate dumps at {@code buffer}/{@code paint}.
 */
public final class SessionDebugLog {

    private static final TN5250jLogger log = TN5250jLogFactory.getLogger(SessionDebugLog.class);
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final Set<String> DETAIL_PHASES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("changed", "send", "received")));
    private static final int LARGE_REGION_CELLS = 16;

    private static boolean fullPlanesEnabled;

    private SessionDebugLog() {
    }

    public static final class CursorSnapshot {
        private final int row;
        private final int col;
        private final boolean active;

        public CursorSnapshot(int row, int col, boolean active) {
            this.row = row;
            this.col = col;
            this.active = active;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static void enableDebugLogging() {
        TN5250jLogFactory.setLogLevels(TN5250jLogger.DEBUG);
    }

    public static void enableFullPlaneLogging() {
        fullPlanesEnabled = true;
        enableDebugLogging();
    }

    public static boolean isFullPlanesEnabled() {
        return fullPlanesEnabled;
    }

    public static void keyStroke(String side, String phase, String detail) {
        if (log.isDebugEnabled()) {
            log.debug(formatEvent("key", side, phase, "detail=" + detail));
        }
    }

    public static void keyEvent(String side, String phase, KeyEvent event) {
        if (!log.isDebugEnabled()) {
            return;
        }
        String keyText = KeyEvent.getKeyText(event.getKeyCode());
        String mods = KeyEvent.getKeyModifiersText(event.getModifiers());
        char keyChar = event.getKeyChar();
        String charText = Character.isISOControl(keyChar)
                ? "(control)"
                : "'" + keyChar + "'";
        keyStroke(side, phase, keyText + " char=" + charText
                + (mods.isEmpty() ? "" : " mods=" + mods));
    }

    public static void mouse(String side, String action, String detail) {
        if (log.isDebugEnabled()) {
            log.debug(formatEvent("mouse", side, action, detail));
        }
    }

    public static void screenPlanes(String side, String phase, int inUpdate,
                                    int startRow, int startCol, int endRow, int endCol,
                                    Map<String, String> planes) {
        screenPlanes(side, phase, inUpdate, startRow, startCol, endRow, endCol, planes, null);
    }

    public static void screenPlanes(String side, String phase, int inUpdate,
                                    int startRow, int startCol, int endRow, int endCol,
                                    Map<String, String> planes, CursorSnapshot cursor) {
        logScreenEvent(side, phase, inUpdate, startRow, startCol, endRow, endCol,
                planes, null, cursor);
    }

    public static void screenPlanesDecoded(String side, String phase, int inUpdate,
                                           int startRow, int startCol, int endRow, int endCol,
                                           Map<String, char[]> planes, CursorSnapshot cursor) {
        logScreenEvent(side, phase, inUpdate, startRow, startCol, endRow, endCol,
                null, planes, cursor);
    }

    static boolean shouldEmitPlaneDetail(String phase, boolean hasPlanes) {
        return fullPlanesEnabled && hasPlanes && DETAIL_PHASES.contains(phase);
    }

    private static void logScreenEvent(String side, String phase, int inUpdate,
                                       int startRow, int startCol, int endRow, int endCol,
                                       Map<String, String> encodedPlanes,
                                       Map<String, char[]> decodedPlanes,
                                       CursorSnapshot cursor) {
        if (!log.isDebugEnabled()) {
            return;
        }
        int seq = SEQUENCE.incrementAndGet();
        int width = regionWidth(startRow, startCol, endRow, endCol);
        int height = regionHeight(startRow, startCol, endRow, endCol);
        int cells = width * height;
        boolean hasPlanes = encodedPlanes != null && !encodedPlanes.isEmpty()
                || decodedPlanes != null && !decodedPlanes.isEmpty();

        StringBuilder summary = new StringBuilder();
        summary.append("SDEBUG seq=").append(seq)
                .append(" event=screen")
                .append(" side=").append(side)
                .append(" phase=").append(phase)
                .append(" kind=").append(updateKind(inUpdate))
                .append(" update=").append(inUpdate)
                .append(" region=").append(startRow).append(',').append(startCol)
                .append('-').append(endRow).append(',').append(endCol)
                .append(" size=").append(width).append('x').append(height);
        if (cursor != null) {
            summary.append(" cursor=").append(cursor.getRow()).append(',').append(cursor.getCol())
                    .append(" active=").append(cursor.isActive());
        }
        if (encodedPlanes != null && !encodedPlanes.isEmpty()) {
            summary.append(" planes=").append(formatPlaneSizes(encodedPlanes));
        } else if (decodedPlanes != null && !decodedPlanes.isEmpty()) {
            summary.append(" planes=").append(formatDecodedPlaneSizes(decodedPlanes));
        }
        log.debug(summary.toString());

        if (!shouldEmitPlaneDetail(phase, hasPlanes)) {
            return;
        }
        String detail = encodedPlanes != null && !encodedPlanes.isEmpty()
                ? ScreenPlaneDebugFormatter.formatDetailForLog(width, encodedPlanes, cells, inUpdate)
                : ScreenPlaneDebugFormatter.formatDetailForDecodedLog(width, decodedPlanes, cells, inUpdate);
        if (detail == null || detail.isEmpty()) {
            return;
        }
        if (detail.indexOf('\n') >= 0) {
            log.debug("SDEBUG seq=" + seq + " detail=planes phase=" + phase + "\n" + detail);
        } else {
            log.debug("SDEBUG seq=" + seq + " detail=planes phase=" + phase + " " + detail);
        }
    }

    static String updateKind(int inUpdate) {
        switch (inUpdate) {
            case 3:
            case 4:
                return "cursor";
            case 1:
                return "region";
            case 2:
                return "host";
            default:
                return "other";
        }
    }

    private static String formatEvent(String event, String side, String phase, String detail) {
        return "SDEBUG seq=" + SEQUENCE.incrementAndGet()
                + " event=" + event
                + " side=" + side
                + " phase=" + phase
                + " " + detail;
    }

    private static int regionWidth(int startRow, int startCol, int endRow, int endCol) {
        return endCol - startCol + 1;
    }

    private static int regionHeight(int startRow, int startCol, int endRow, int endCol) {
        return endRow - startRow + 1;
    }

    private static String formatPlaneSizes(Map<String, String> planes) {
        StringBuilder sizes = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : planes.entrySet()) {
            if (!first) {
                sizes.append(',');
            }
            first = false;
            String encoded = entry.getValue();
            sizes.append(entry.getKey()).append(':')
                    .append(encoded == null ? 0 : encoded.length()).append('b');
        }
        return sizes.toString();
    }

    private static String formatDecodedPlaneSizes(Map<String, char[]> planes) {
        StringBuilder sizes = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, char[]> entry : planes.entrySet()) {
            if (!first) {
                sizes.append(',');
            }
            first = false;
            char[] data = entry.getValue();
            sizes.append(entry.getKey()).append(':')
                    .append(data == null ? 0 : data.length).append('c');
        }
        return sizes.toString();
    }
}
