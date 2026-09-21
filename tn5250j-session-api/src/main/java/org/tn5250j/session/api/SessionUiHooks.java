package org.tn5250j.session.api;

public interface SessionUiHooks {

    void signalBell();

    String promptSystemRequest();

    boolean acceptUntrustedCertificate(String info);

    default boolean rememberAcceptedCertificate() {
        return false;
    }

    default boolean confirmSaveSettings(String message) {
        return false;
    }
}
