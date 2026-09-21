package org.tn5250j.session.wire;

public final class OiaStateDto {

    private boolean insertMode;
    private int level;
    private int inputInhibited;
    private String inhibitedText;
    private boolean keysBuffered;
    private boolean scriptActive;
    private boolean messageWait;

    public OiaStateDto() {
    }

    public OiaStateDto(boolean insertMode, int level, int inputInhibited, String inhibitedText) {
        this.insertMode = insertMode;
        this.level = level;
        this.inputInhibited = inputInhibited;
        this.inhibitedText = inhibitedText;
    }

    public boolean isInsertMode() {
        return insertMode;
    }

    public void setInsertMode(boolean insertMode) {
        this.insertMode = insertMode;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getInputInhibited() {
        return inputInhibited;
    }

    public void setInputInhibited(int inputInhibited) {
        this.inputInhibited = inputInhibited;
    }

    public String getInhibitedText() {
        return inhibitedText;
    }

    public void setInhibitedText(String inhibitedText) {
        this.inhibitedText = inhibitedText;
    }

    public boolean isKeysBuffered() {
        return keysBuffered;
    }

    public void setKeysBuffered(boolean keysBuffered) {
        this.keysBuffered = keysBuffered;
    }

    public boolean isScriptActive() {
        return scriptActive;
    }

    public void setScriptActive(boolean scriptActive) {
        this.scriptActive = scriptActive;
    }

    public boolean isMessageWait() {
        return messageWait;
    }

    public void setMessageWait(boolean messageWait) {
        this.messageWait = messageWait;
    }
}
