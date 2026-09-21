package org.tn5250j.session.client.local;

import org.tn5250j.Session5250;
import org.tn5250j.event.SessionChangeEvent;
import org.tn5250j.event.SessionListener;
import org.tn5250j.session.api.ScreenModel;
import org.tn5250j.session.api.SessionClient;
import org.tn5250j.session.api.SessionUiHooks;
import org.tn5250j.session.api.TerminalOps;
import org.tn5250j.session.api.UnsupportedCapabilityException;

import java.util.ArrayList;
import java.util.List;

public final class LocalSessionClient implements SessionClient, SessionListener {

    private final Session5250 session;
    private final LocalScreenModel screenModel;
    private final LocalTerminalOps terminalOps;
    private SessionUiHooks uiHooks = org.tn5250j.session.api.HeadlessSessionUiHooks.INSTANCE;
    private final List<org.tn5250j.session.api.SessionListener> listeners = new ArrayList<>();

    public LocalSessionClient(Session5250 session) {
        this.session = session;
        this.screenModel = new LocalScreenModel(session.getScreen());
        this.terminalOps = new LocalTerminalOps(session);
        session.addSessionListener(this);
    }

    public Session5250 getSession5250() {
        return session;
    }

    @Override
    public void connect() {
        session.setUiHooks(new UiHooksBridge(uiHooks));
        session.connect();
    }

    @Override
    public void disconnect() {
        session.disconnect();
    }

    @Override
    public boolean isConnected() {
        return session.isConnected();
    }

    @Override
    public String getSessionName() {
        return session.getSessionName();
    }

    @Override
    public String getHostName() {
        return session.getHostName();
    }

    @Override
    public String getAllocatedDeviceName() {
        return session.getAllocatedDeviceName();
    }

    @Override
    public ScreenModel getScreen() {
        return screenModel;
    }

    @Override
    public void addSessionListener(org.tn5250j.session.api.SessionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeSessionListener(org.tn5250j.session.api.SessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void sendKeys(String keys) {
        screenModel.sendKeys(keys);
    }

    @Override
    public boolean moveCursor(int pos) {
        return screenModel.moveCursor(pos);
    }

    @Override
    public void sendAid(int aidKey) {
        screenModel.sendAid(aidKey);
    }

    @Override
    public void setUiHooks(SessionUiHooks hooks) {
        uiHooks = hooks == null ? org.tn5250j.session.api.HeadlessSessionUiHooks.INSTANCE : hooks;
        session.setUiHooks(new UiHooksBridge(uiHooks));
    }

    @Override
    public SessionUiHooks getUiHooks() {
        return uiHooks;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T extension(Class<T> capability) {
        if (TerminalOps.class.equals(capability)) {
            return (T) terminalOps;
        }
        throw new UnsupportedCapabilityException(capability);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> type) {
        if (Session5250.class.equals(type)) {
            return (T) session;
        }
        if (type.isInstance(screenModel.getScreen5250())) {
            return (T) screenModel.getScreen5250();
        }
        return null;
    }

    @Override
    public void onSessionChanged(SessionChangeEvent changeEvent) {
        org.tn5250j.session.api.SessionChangeEvent event =
                new org.tn5250j.session.api.SessionChangeEvent(this);
        event.setState(changeEvent.getState());
        event.setMessage(changeEvent.getMessage());
        for (org.tn5250j.session.api.SessionListener listener : new ArrayList<>(listeners)) {
            listener.onSessionChanged(event);
        }
    }
}
