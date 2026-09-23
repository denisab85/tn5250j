package org.tn5250j.session.client.remote;

import org.tn5250j.session.api.ScreenPlaneConstants;

/**
 * Best-effort screen operations on the client frame buffer for remote sessions.
 * Mirrors {@code Screen5250} behavior where the server-side field model is unavailable.
 */
final class ScreenBufferOps {

    private static final char EXTENDED_5250_NON_DSP = 0x01;

    private ScreenBufferOps() {
    }

    static boolean checkHotSpots(ScreenFrameBuffer buffer) {
        int lenScreen = buffer.getScreenLength();
        boolean found = false;
        for (int x = 1; x < lenScreen - 3; x++) {
            if (ScreenTextOps.isInField(buffer, x)) {
                continue;
            }
            if (buffer.getTextChar(x) != 'F') {
                continue;
            }
            if (buffer.getTextChar(x - 1) > ' ') {
                continue;
            }
            if ((buffer.getExtendedChar(x) & EXTENDED_5250_NON_DSP) != 0) {
                continue;
            }
            char digit = buffer.getTextChar(x + 1);
            if (digit < '0' || digit > '9') {
                continue;
            }
            char third = buffer.getTextChar(x + 2);
            char fourth = x + 3 < lenScreen ? buffer.getTextChar(x + 3) : 0;
            if ((third >= '0' && third <= '9' && (fourth == '=' || fourth == '-' || fourth == '/'))
                    || third == '='
                    || fourth == '-'
                    || fourth == '/') {
                found = true;
            }
        }
        return found;
    }

    static String copyTextField(ScreenFrameBuffer buffer, int position) {
        int length = buffer.getScreenLength();
        if (position < 0 || position >= length) {
            return "";
        }
        int pos = position;
        if (!ScreenTextOps.isInField(buffer, pos)) {
            if (pos + 1 < length && ScreenTextOps.isInField(buffer, pos + 1)) {
                pos++;
            } else {
                return "";
            }
        }
        int end = pos;
        while (end + 1 < length && ScreenTextOps.isInField(buffer, end + 1)) {
            end++;
        }
        int start = pos;
        while (start - 1 >= 0 && ScreenTextOps.isInField(buffer, start - 1)) {
            start--;
        }
        if (ScreenTextOps.isAttributePlace(buffer, start) && start + 1 <= end) {
            start++;
        }
        int fieldLen = end - start + 1;
        StringBuilder text = new StringBuilder(fieldLen);
        for (int i = start; i <= end; i++) {
            if (ScreenTextOps.isAttributePlace(buffer, i)) {
                text.append(' ');
            } else {
                text.append(buffer.getTextChar(i));
            }
        }
        while (text.length() < fieldLen) {
            text.append(' ');
        }
        return text.toString();
    }

    static void pasteText(ScreenFrameBuffer buffer, String content, boolean special) {
        int lastPos = buffer.getPos(buffer.getCurrentRow() - 1, buffer.getCurrentCol() - 1);
        int lr = buffer.getRow(lastPos);
        int lc = buffer.getCol(lastPos);
        int cpos = lastPos;
        int length = buffer.getScreenLength();

        buffer.setCursorActive(false);
        for (int x = 0; x < content.length(); x++) {
            char c = content.charAt(x);
            if (c == '\n' || c == '\r') {
                cpos = buffer.getPos(buffer.getRow(cpos) + 1, lc);
                if (cpos >= length) {
                    cpos = 0;
                }
                continue;
            }
            boolean setIt = true;
            if (special && !Character.isLetter(c) && !Character.isDigit(c)) {
                setIt = false;
            }
            if (ScreenTextOps.isInField(buffer, cpos) && setIt) {
                buffer.setTextChar(cpos, c);
            }
            if (setIt) {
                cpos++;
            }
        }
        buffer.setCurrentPosition(lr + 1, lc + 1);
        buffer.setCursorActive(true);
    }
}
