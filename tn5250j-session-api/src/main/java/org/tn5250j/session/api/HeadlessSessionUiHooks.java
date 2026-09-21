package org.tn5250j.session.api;

public final class HeadlessSessionUiHooks implements SessionUiHooks {

    public static final HeadlessSessionUiHooks INSTANCE = new HeadlessSessionUiHooks();

    private HeadlessSessionUiHooks() {
    }

    @Override
    public void signalBell() {
    }

    @Override
    public String promptSystemRequest() {
        return null;
    }

    @Override
    public boolean acceptUntrustedCertificate(String info) {
        return false;
    }
}
