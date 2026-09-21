package org.tn5250j.session.api;

public interface OiaModel {

    boolean isInsertMode();

    int getLevel();

    int getInputInhibited();

    String getInhibitedText();

    boolean isKeysBuffered();

    boolean isScriptActive();

    boolean isMessageWait();

    void setScriptActive(boolean running);

    void addOIAListener(OiaModelListener listener);

    void removeOIAListener(OiaModelListener listener);
}
