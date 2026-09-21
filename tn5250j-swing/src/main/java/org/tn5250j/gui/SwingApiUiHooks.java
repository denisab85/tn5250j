package org.tn5250j.gui;

import org.tn5250j.session.api.SessionUiHooks;

import java.awt.Component;

/**
 * Bridges desktop Swing UI hooks to the session API contract.
 */
public final class SwingApiUiHooks implements SessionUiHooks {

    private final SwingSessionUiHooks delegate;

    public SwingApiUiHooks(Component parent) {
        this.delegate = new SwingSessionUiHooks(parent);
    }

    @Override
    public void signalBell() {
        delegate.signalBell();
    }

    @Override
    public String promptSystemRequest() {
        return delegate.promptSystemRequest();
    }

    @Override
    public boolean acceptUntrustedCertificate(String info) {
        return delegate.acceptUntrustedCertificate(info);
    }

    @Override
    public boolean rememberAcceptedCertificate() {
        return delegate.rememberAcceptedCertificate();
    }

    @Override
    public boolean confirmSaveSettings(String message) {
        return delegate.confirmSaveSettings(message);
    }

    public SwingSessionUiHooks getDelegate() {
        return delegate;
    }
}
