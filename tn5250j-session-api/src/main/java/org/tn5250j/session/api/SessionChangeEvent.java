package org.tn5250j.session.api;

public class SessionChangeEvent {

    private final Object source;
    private int state;
    private String message;

    public SessionChangeEvent(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
