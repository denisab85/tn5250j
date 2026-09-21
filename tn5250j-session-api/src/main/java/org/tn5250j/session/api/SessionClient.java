package org.tn5250j.session.api;

/**
 * UI-facing session contract for local or remote core attachment.
 */
public interface SessionClient {

    void connect();

    void disconnect();

    boolean isConnected();

    String getSessionName();

    String getHostName();

    String getAllocatedDeviceName();

    ScreenModel getScreen();

    void addSessionListener(SessionListener listener);

    void removeSessionListener(SessionListener listener);

    void sendKeys(String keys);

    boolean moveCursor(int pos);

    void sendAid(int aidKey);

    void setUiHooks(SessionUiHooks hooks);

    SessionUiHooks getUiHooks();

    /**
     * Optional capability extension (e.g. {@link TerminalOps}).
     */
    <T> T extension(Class<T> capability);

    /**
     * Returns an underlying implementation type when available (in-process only).
     */
    <T> T unwrap(Class<T> type);
}
