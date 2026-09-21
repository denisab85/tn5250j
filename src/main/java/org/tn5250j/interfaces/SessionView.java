package org.tn5250j.interfaces;

/**
 * Optional view attached to a session. Core session lifecycle talks to this
 * type instead of a concrete Swing panel so a session can run headless.
 */
public interface SessionView {

    /**
     * Release view resources when the session is closed.
     */
    void closeDown();

}
