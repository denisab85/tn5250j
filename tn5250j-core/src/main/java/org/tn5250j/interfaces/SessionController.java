package org.tn5250j.interfaces;

import org.tn5250j.event.SessionListener;
import org.tn5250j.framework.tn5250.Screen5250;

/**
 * GUI-agnostic session façade. Clients drive connect/disconnect and the
 * {@link Screen5250} screen model through this type. It does not reference
 * Swing, AWT, or {@code SessionPanel}.
 */
public interface SessionController {

    void connect();

    void disconnect();

    boolean isConnected();

    String getSessionName();

    /**
     * @return the host name once known, or null when no virtual terminal is attached
     * or the host has not been set
     */
    String getHostName();

    /**
     * @return the device name allocated by the host, or null when none has been allocated
     */
    String getAllocatedDeviceName();

    void addSessionListener(SessionListener listener);

    void removeSessionListener(SessionListener listener);

    Screen5250 getScreen();

    void setUiHooks(SessionUiHooks hooks);

    SessionUiHooks getUiHooks();

    /**
     * Attach or clear an optional view. Core never depends on a concrete GUI type.
     */
    void setView(SessionView view);

    /**
     * @return the attached view, or null when running headless
     */
    SessionView getView();

}
