package org.tn5250j.session.client.local;

import org.tn5250j.event.ScreenOIAListener;
import org.tn5250j.framework.tn5250.ScreenOIA;
import org.tn5250j.session.api.OiaModel;
import org.tn5250j.session.api.OiaModelListener;

import java.util.ArrayList;
import java.util.List;

public final class LocalOiaModel implements OiaModel, ScreenOIAListener {

    private final ScreenOIA oia;
    private final List<OiaModelListener> listeners = new ArrayList<>();

    public LocalOiaModel(ScreenOIA oia) {
        this.oia = oia;
        oia.addOIAListener(this);
    }

    @Override
    public boolean isInsertMode() {
        return oia.isInsertMode();
    }

    @Override
    public int getLevel() {
        return oia.getLevel();
    }

    @Override
    public int getInputInhibited() {
        return oia.getInputInhibited();
    }

    @Override
    public String getInhibitedText() {
        return oia.getInhibitedText();
    }

    @Override
    public boolean isKeysBuffered() {
        return oia.isKeysBuffered();
    }

    @Override
    public boolean isScriptActive() {
        return oia.isScriptActive();
    }

    @Override
    public void setScriptActive(boolean running) {
        oia.setScriptActive(running);
    }

    @Override
    public boolean isMessageWait() {
        return oia.isMessageWait();
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
    public void onOIAChanged(ScreenOIA changedOIA, int change) {
        for (OiaModelListener listener : new ArrayList<>(listeners)) {
            listener.onOIAChanged(this, change);
        }
    }
}
