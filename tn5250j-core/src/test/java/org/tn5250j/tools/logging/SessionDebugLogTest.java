package org.tn5250j.tools.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionDebugLogTest {

    @Before
    public void setUp() {
        SessionDebugLog.enableFullPlaneLogging();
    }

    @After
    public void tearDown() {
        SessionDebugLog.enableDebugLogging();
    }

    @Test
    public void detailPhasesIncludeWireAndSessionStages() {
        assertTrue(SessionDebugLog.shouldEmitPlaneDetail("changed", true));
        assertTrue(SessionDebugLog.shouldEmitPlaneDetail("send", true));
        assertTrue(SessionDebugLog.shouldEmitPlaneDetail("received", true));
        assertFalse(SessionDebugLog.shouldEmitPlaneDetail("buffer", true));
        assertFalse(SessionDebugLog.shouldEmitPlaneDetail("paint", true));
    }

    @Test
    public void updateKindLabelsCursorAndRegionUpdates() {
        assertTrue(SessionDebugLog.updateKind(3).equals("cursor"));
        assertTrue(SessionDebugLog.updateKind(1).equals("region"));
    }

    @Test
    public void compactDetailForSingleCellUpdates() {
        Map<String, String> encoded = new LinkedHashMap<>();
        encoded.put("text", Base64.getEncoder().encodeToString(new byte[] {'K'}));
        encoded.put("attr", Base64.getEncoder().encodeToString(new byte[] {0x24}));
        encoded.put("field", Base64.getEncoder().encodeToString(new byte[] {0x40}));

        String detail = ScreenPlaneDebugFormatter.formatDetailForLog(1, encoded, 1, 1);

        assertTrue(detail.contains("text=\"K\""));
        assertTrue(detail.contains("attr=24"));
        assertTrue(detail.contains("field=40"));
    }

    @Test
    public void largeRegionsUseTextOnlyDetail() {
        Map<String, char[]> planes = new LinkedHashMap<>();
        char[] text = new char[80 * 24];
        text[0] = 'A';
        planes.put("text", text);
        planes.put("attr", new char[text.length]);

        String detail = ScreenPlaneDebugFormatter.formatDetailForDecodedLog(80, planes, 80 * 24, 1);

        assertTrue(detail.startsWith("mode=text-only"));
        assertTrue(detail.contains("A"));
        assertFalse(detail.contains("attr:"));
    }
}
