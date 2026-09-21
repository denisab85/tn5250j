package org.tn5250j.session.api;

/**
 * Advanced terminal operations migrated incrementally from direct {@code tnvt} access.
 */
public interface TerminalOps {

    void systemRequest(char code);

    void systemRequest(String code);

    void toggleDebug();

    boolean isSupported();
}
