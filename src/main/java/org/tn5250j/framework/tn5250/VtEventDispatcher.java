package org.tn5250j.framework.tn5250;

/**
 * Runs screen and OIA updates that originate on the virtual terminal thread.
 * Swing sessions dispatch onto the event dispatch thread. Headless sessions
 * run the update inline.
 */
public interface VtEventDispatcher {

    void dispatch(Runnable action);

}
