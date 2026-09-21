package org.tn5250j.framework.tn5250;

/**
 * Runs VT-originated UI updates on the calling thread.
 */
public final class InlineVtEventDispatcher implements VtEventDispatcher {

    public static final InlineVtEventDispatcher INSTANCE = new InlineVtEventDispatcher();

    @Override
    public void dispatch(Runnable action) {
        action.run();
    }

}
