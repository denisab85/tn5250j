package org.tn5250j.session.client.remote;

import org.tn5250j.session.api.OiaModel;
import org.tn5250j.session.api.OiaModelListener;
import org.tn5250j.session.wire.OiaStateDto;

import java.util.ArrayList;
import java.util.List;

final class RemoteOiaModel implements OiaModel {

    private boolean insertMode;
    private int level;
    private int inputInhibited;
    private String inhibitedText = "";
    private boolean keysBuffered;
    private boolean scriptActive;
    private boolean messageWait;
    private final List<OiaModelListener> listeners = new ArrayList<>();

    void apply(OiaStateDto dto) {
        if (dto == null) {
            return;
        }
        insertMode = dto.isInsertMode();
        level = dto.getLevel();
        inputInhibited = dto.getInputInhibited();
        inhibitedText = dto.getInhibitedText();
        keysBuffered = dto.isKeysBuffered();
        scriptActive = dto.isScriptActive();
        messageWait = dto.isMessageWait();
    }

    void fireChanged(int change) {
        for (OiaModelListener listener : new ArrayList<>(listeners)) {
            listener.onOIAChanged(this, change);
        }
    }

    @Override
    public boolean isInsertMode() {
        return insertMode;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getInputInhibited() {
        return inputInhibited;
    }

    @Override
    public String getInhibitedText() {
        return inhibitedText;
    }

    @Override
    public void addOIAListener(OiaModelListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeOIAListener(OiaModelListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isKeysBuffered() {
        return keysBuffered;
    }

    @Override
    public boolean isScriptActive() {
        return scriptActive;
    }

    @Override
    public void setScriptActive(boolean running) {
        scriptActive = running;
    }

    @Override
    public boolean isMessageWait() {
        return messageWait;
    }
}
