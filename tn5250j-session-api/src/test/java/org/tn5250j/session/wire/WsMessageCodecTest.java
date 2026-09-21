package org.tn5250j.session.wire;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WsMessageCodecTest {

    @Test
    public void roundTripEnvelope() {
        JsonObject payload = new JsonObject();
        payload.addProperty("keys", "[enter]");
        WsEnvelope original = WsEnvelope.command(WsMessageType.SEND_KEYS, "sess-1", payload);

        String json = WsMessageCodec.encode(original);
        WsEnvelope decoded = WsMessageCodec.decode(json);

        assertEquals(WsMessageType.SEND_KEYS, decoded.getType());
        assertEquals("sess-1", decoded.getSessionId());
        assertNotNull(decoded.getPayload());
        assertEquals("[enter]", decoded.getPayload().get("keys").getAsString());
    }
}
