package org.tn5250j.session.wire;

import java.util.Base64;

public final class ScreenPlaneCodec {

    private ScreenPlaneCodec() {
    }

    public static char[] decode(String base64) {
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

    public static String encodeChars(char[] buffer) {
        byte[] bytes = new byte[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            bytes[i] = (byte) (buffer[i] & 0xFF);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
