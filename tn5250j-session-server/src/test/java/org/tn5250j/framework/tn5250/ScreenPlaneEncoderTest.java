package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.session.server.wire.ScreenPlaneEncoder;
import org.tn5250j.session.wire.ScreenPlaneCodec;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScreenPlaneEncoderTest {

    private Tn5250TestHarness harness;
    private Screen5250 screen;

    @Before
    public void setUp() {
        harness = Tn5250TestHarness.create();
        screen = harness.screen();
        harness.process(harness.outputOnly(harness.concat(
                new byte[]{TN5250jConstants.CMD_CLEAR_UNIT},
                harness.wtdText(1, 1, "HELLO"))));
    }

    @Test
    public void encodeRegionUsesZeroBasedListenerCoordinates() {
        Map<String, String> planes = ScreenPlaneEncoder.encodeRegion(screen, 0, 0, 0, 4);
        assertNotNull(planes.get("text"));
        char[] encoded = ScreenPlaneCodec.decode(planes.get("text"));
        assertEquals('H', encoded[0]);
        assertEquals('E', encoded[1]);
        assertEquals('L', encoded[2]);
        assertEquals('L', encoded[3]);
        assertEquals('O', encoded[4]);
    }
}
