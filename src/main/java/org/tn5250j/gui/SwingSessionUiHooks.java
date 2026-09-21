package org.tn5250j.gui;

import org.tn5250j.interfaces.SessionUiHooks;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Toolkit;

/**
 * Desktop UI side effects: toolkit beep, the system-request dialog, and the
 * historical certificate trust prompts.
 */
public final class SwingSessionUiHooks implements SessionUiHooks {

    private final Component parent;

    public SwingSessionUiHooks(Component parent) {
        this.parent = parent;
    }

    @Override
    public void signalBell() {
        Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public String promptSystemRequest() {
        return new SystemRequestDialog(parent).show();
    }

    @Override
    public boolean acceptUntrustedCertificate(String info) {
        int accept = JOptionPane.showConfirmDialog(null, info,
                "Unknown Certificate - Do you accept it?",
                JOptionPane.YES_NO_OPTION);
        return accept == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean rememberAcceptedCertificate() {
        int save = JOptionPane.showConfirmDialog(null,
                "Remember this certificate?", "Save Certificate",
                JOptionPane.YES_NO_OPTION);
        return save == JOptionPane.YES_OPTION;
    }

}
