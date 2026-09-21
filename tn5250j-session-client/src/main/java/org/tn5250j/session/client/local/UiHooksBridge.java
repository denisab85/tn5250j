package org.tn5250j.session.client.local;

import org.tn5250j.session.api.SessionUiHooks;

final class UiHooksBridge implements org.tn5250j.interfaces.SessionUiHooks {

    private final SessionUiHooks delegate;

    UiHooksBridge(SessionUiHooks delegate) {
        this.delegate = delegate == null
                ? org.tn5250j.session.api.HeadlessSessionUiHooks.INSTANCE : delegate;
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
}
