package org.tn5250j;

import org.tn5250j.framework.tn5250.VtEventDispatcher;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

/**
 * Runs VT-originated screen updates on the Swing event dispatch thread.
 * Matches the previous {@code SwingUtilities.invokeAndWait} behavior used
 * while a session has a GUI attached.
 */
public final class SwingEdtDispatcher implements VtEventDispatcher {

    public static final SwingEdtDispatcher INSTANCE = new SwingEdtDispatcher();

    @Override
    public void dispatch(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

}
