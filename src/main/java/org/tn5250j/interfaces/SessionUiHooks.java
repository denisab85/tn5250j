package org.tn5250j.interfaces;

/**
 * UI side effects for a 5250 session. The session and screen model call this
 * instead of Swing. A desktop client installs a Swing implementation; a
 * headless or web client installs its own or uses {@link HeadlessSessionUiHooks}.
 */
public interface SessionUiHooks {

    void signalBell();

    /**
     * @return null if cancelled or nothing to do, otherwise the user's SysReq input
     */
    String promptSystemRequest();

    /**
     * @param info human-readable certificate details
     * @return true to trust the certificate
     */
    boolean acceptUntrustedCertificate(String info);

    /**
     * Called only after {@link #acceptUntrustedCertificate(String)} returns true.
     * The default does not persist the certificate. The Swing implementation
     * prompts with the historical "Remember this certificate?" dialog.
     *
     * @return true to store the accepted certificate in the user keystore
     */
    default boolean rememberAcceptedCertificate() {
        return false;
    }

}
