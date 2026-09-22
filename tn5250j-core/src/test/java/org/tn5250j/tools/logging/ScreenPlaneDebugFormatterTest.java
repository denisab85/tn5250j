package org.tn5250j.tools.logging;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class ScreenPlaneDebugFormatterTest {

    @Test
    public void formatsTextAsReadableGridAndOtherPlanesAsHex() {
        Map<String, char[]> planes = new LinkedHashMap<>();
        planes.put("text", "Hi".toCharArray());
        planes.put("attr", new char[] {0x20, 0x22});

        String formatted = ScreenPlaneDebugFormatter.formatRegion(2, planes);

        assertTrue(formatted.contains("text:\nHi\n"));
        assertTrue(formatted.contains("attr:\n20 22\n"));
    }

    @Test
    public void decodesEncodedPlanesBeforeFormatting() {
        Map<String, String> encoded = new LinkedHashMap<>();
        encoded.put("text", java.util.Base64.getEncoder().encodeToString(new byte[] {'A', 'B'}));

        String formatted = ScreenPlaneDebugFormatter.formatEncodedRegion(2, encoded);

        assertTrue(formatted.contains("text:\nAB\n"));
    }

    @Test
    public void compactFormatUsesSingleLineKeyValuePairs() {
        Map<String, char[]> planes = new LinkedHashMap<>();
        planes.put("text", "k".toCharArray());
        planes.put("attr", new char[] {0x24});

        String formatted = ScreenPlaneDebugFormatter.formatCompact(planes);

        assertTrue(formatted.contains("text=\"k\""));
        assertTrue(formatted.contains("attr=24"));
    }

}
