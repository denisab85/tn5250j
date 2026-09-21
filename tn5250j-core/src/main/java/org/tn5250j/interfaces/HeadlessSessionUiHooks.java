package org.tn5250j.interfaces;

/**
 * No-op UI hooks for a session with no view attached.
 * The bell is ignored, system request is cancelled, and untrusted
 * certificates are rejected so a headless process never blocks on a dialog.
 */
public final class HeadlessSessionUiHooks implements SessionUiHooks {

    public static final HeadlessSessionUiHooks INSTANCE = new HeadlessSessionUiHooks();

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
