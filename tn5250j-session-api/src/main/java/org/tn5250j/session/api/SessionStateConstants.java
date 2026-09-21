package org.tn5250j.session.api;

/** Session lifecycle states aligned with core {@code TN5250jConstants}. */
public final class SessionStateConstants {

    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 2;
    public static final int STATE_REMOVE = 3;

    private SessionStateConstants() {
    }
}
