package org.tn5250j.session.api;

/** OIA change types aligned with core {@code ScreenOIAListener}. */
public final class OiaChangeConstants {

    public static final int OIA_CHANGED_INSERT_MODE = 0;
    public static final int OIA_CHANGED_KEYS_BUFFERED = 1;
    public static final int OIA_CHANGED_KEYBOARD_LOCKED = 2;
    public static final int OIA_CHANGED_MESSAGELIGHT = 3;
    public static final int OIA_CHANGED_SCRIPT = 4;
    public static final int OIA_CHANGED_BELL = 5;
    public static final int OIA_CHANGED_CLEAR_SCREEN = 6;
    public static final int OIA_CHANGED_INPUTINHIBITED = 7;
    public static final int OIA_CHANGED_CURSOR = 8;

    private OiaChangeConstants() {
    }
}
