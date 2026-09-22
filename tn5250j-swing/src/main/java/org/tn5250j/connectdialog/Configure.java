/*
 * Title: tn5250J
 * Copyright:   Copyright (c) 2001
 * Company:
 *
 * @author Kenneth J. Pouncey
 * @version 0.4
 * <p>
 * Description:
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.tn5250j.connectdialog;

import java.net.URI;
import org.tn5250j.cli.DesktopOptions;
import org.tn5250j.cli.SessionOptions;
import org.tn5250j.cli.StoredArguments;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.tn5250j.TN5250jConstants;
import org.tn5250j.encoding.CharMappings;
import org.tn5250j.tools.AlignLayout;
import org.tn5250j.tools.LangTool;

class Configure {

    private static Properties props = null;

    // property input structures
    private static JTextField systemName = null;
    private static JTextField systemId = null;
    private static JTextField port = null;
    private static JTextField deviceName = null;
    private static JTextField fpn = null;
    private static JComboBox cpb = null;
    private static JCheckBox jtb = null;
    private static JCheckBox ec = null;
    private static JCheckBox tc = null;
    private static JCheckBox sdn = null;
    private static JRadioButton sdNormal = null;
    private static JCheckBox useProxy = null;
    private static JTextField proxyHost = null;
    private static JTextField proxyPort = null;
    private static JCheckBox noEmbed = null;
    private static JCheckBox deamon = null;
    private static JCheckBox newJVM = null;
    private static JComboBox sslType = null;
    private static JCheckBox heartBeat = null;
    private static URI remoteEndpoint = null;
    private static String remoteAuthToken = null;

    private static JTabbedPane confTabs;
    private static JDialog dialog = null;
    private static Object[] options;

    public static String doEntry(Frame parent, String propKey, Properties props2) {

        props = props2;

        confTabs = new JTabbedPane();

        ec = new JCheckBox(LangTool.getString("conf.labelEnhanced"));
        tc = new JCheckBox(LangTool.getString("conf.labelUseSystemName"));
        sdn = new JCheckBox(LangTool.getString("conf.labelUseHostName"));
        useProxy = new JCheckBox(LangTool.getString("conf.labelUseProxy"));
        sdNormal = new JRadioButton(LangTool.getString("conf.label24"));
        JRadioButton sdBig = new JRadioButton(LangTool.getString("conf.label27"));
        noEmbed = new JCheckBox(LangTool.getString("conf.labelEmbed"));
        deamon = new JCheckBox(LangTool.getString("conf.labelDeamon"));
        newJVM = new JCheckBox(LangTool.getString("conf.labelNewJVM"));
        heartBeat = new JCheckBox(LangTool.getString("conf.labelHeartBeat"));

        jtb = new JCheckBox("AS/400 Toolbox");
        jtb.addItemListener(e -> doCPStateChanged(e));

        cpb = new JComboBox();

        String[] availCP = getAvailableCodePages();

        cpb.addItem(LangTool.getString("conf.labelDefault"));

        for (String s : availCP) {
            cpb.addItem(s);
        }

        sslType = new JComboBox();

        for (int x = 0; x < TN5250jConstants.SSL_TYPES.length; x++) {
            sslType.addItem(TN5250jConstants.SSL_TYPES[x]);
        }

        if (propKey == null) {
            remoteEndpoint = null;
            remoteAuthToken = null;
            systemName = new JTextField(20);
            systemId = new JTextField(20);
            port = new JTextField("23", 5);
            deviceName = new JTextField(20);
            fpn = new JTextField(20);
            proxyHost = new JTextField(20);
            proxyPort = new JTextField("1080", 5);

            ec.setSelected(true);
            tc.setSelected(true);
            jtb.setSelected(false);
            sdNormal.setSelected(true);
            deamon.setSelected(true);

            newJVM.setEnabled(false);
            noEmbed.setEnabled(false);
            deamon.setEnabled(false);

            systemName.setDocument(new SomethingEnteredDocument());
        } else {

            DesktopOptions parsed = DesktopOptions.parse(StoredArguments.split((String) props.get(propKey)));
            SessionOptions session = parsed.session;
            remoteEndpoint = parsed.remote;
            remoteAuthToken = parsed.remoteToken;
            systemName = new JTextField(propKey, 20);
            systemName.setEditable(false);
            systemName.setEnabled(false);

            systemId = new JTextField(session.host, 20);

            if (session.port != null) {
                port = new JTextField(session.port.toString(), 5);
            } else {
                port = new JTextField("23", 5);
            }

            if (session.sslType != null)
                sslType.setSelectedItem(session.sslType);

            if (session.proxyHost != null)
                proxyHost = new JTextField(session.proxyHost, 20);
            else
                proxyHost = new JTextField(20);

            if (session.config != null)
                fpn = new JTextField(session.config, 20);
            else
                fpn = new JTextField(20);
            if (session.codePage != null) {
                String codepage = session.codePage;
                String[] acps = CharMappings.getAvailableCodePages();
                jtb.setSelected(true);
                for (String acp : acps) {
                    if (acp.equals(codepage)) jtb.setSelected(false);
                }
                cpb.setSelectedItem(codepage);

            }

            if (session.enhanced)
                ec.setSelected(true);
            else
                ec.setSelected(false);
            if (session.nameFromSystem)
                tc.setSelected(true);
            else
                tc.setSelected(false);

            if (session.wide)
                sdBig.setSelected(true);
            else
                sdNormal.setSelected(true);

            if (session.deviceName != null)
                deviceName = new JTextField(session.deviceName, 20);
            else
                deviceName = new JTextField(20);

            if (session.deviceNameFromHostname) {
                sdn.setSelected(true);
                deviceName.setEnabled(false);
            } else {
                sdn.setSelected(false);
                deviceName.setEnabled(true);
            }

            if (session.proxyPort != null) {
                proxyPort = new JTextField(session.proxyPort.toString(), 5);
            } else {
                proxyPort = new JTextField("1080", 5);
            }

            if (session.proxy || session.proxyHost != null || session.proxyPort != null)
                useProxy.setSelected(true);
            else
                useProxy.setSelected(false);

            if (session.newWindow)
                noEmbed.setSelected(true);
            else
                noEmbed.setSelected(false);

            if (parsed.daemon)
                deamon.setSelected(true);
            else
                deamon.setSelected(false);

            if (parsed.newInstance)
                newJVM.setSelected(true);
            else
                newJVM.setSelected(false);

            if (session.heartbeat)
                heartBeat.setSelected(true);
            else
                heartBeat.setSelected(false);

            if (session.heartbeat)
                heartBeat.setSelected(true);
            else
                heartBeat.setSelected(false);

        }

        //Create main attributes panel
        JPanel mp = new JPanel();
        BoxLayout mpLayout = new BoxLayout(mp, BoxLayout.Y_AXIS);

        mp.setLayout(mpLayout);

        //System Name panel
        JPanel snp = new JPanel();
        AlignLayout snpLayout = new AlignLayout(2, 5, 5);
        snp.setLayout(snpLayout);
        snp.setBorder(BorderFactory.createEtchedBorder());

        addLabelComponent(LangTool.getString("conf.labelSystemName"),
                systemName,
                snp);

        addLabelComponent(" ",
                noEmbed,
                snp);

        addLabelComponent(" ",
                deamon,
                snp);

        addLabelComponent(" ",
                newJVM,
                snp);

        //System Id panel
        JPanel sip = new JPanel();

        AlignLayout al = new AlignLayout(2, 5, 5);

        sip.setLayout(al);
        sip.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("conf.labelSystemIdTitle")));


        addLabelComponent(LangTool.getString("conf.labelSystemId"),
                systemId,
                sip);

        addLabelComponent(LangTool.getString("conf.labelPort"),
                port,
                sip);

        addLabelComponent(LangTool.getString("conf.labelDeviceName"),
                deviceName,
                sip);

        addLabelComponent("",
                sdn,
                sip);

        sdn.addItemListener(e -> doItemStateChanged(e));

        addLabelComponent(LangTool.getString("conf.labelSSLType"),
                sslType,
                sip);

        addLabelComponent("",
                heartBeat,
                sip);

        // options panel
        JPanel op = new JPanel();
        BoxLayout opLayout = new BoxLayout(op, BoxLayout.Y_AXIS);
        op.setLayout(opLayout);
        op.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("conf.labelOptionsTitle")));

        // file name panel
        JPanel fp = new JPanel();
        BoxLayout fpLayout = new BoxLayout(fp, BoxLayout.Y_AXIS);
        fp.setLayout(fpLayout);
        fp.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("conf.labelConfFile")));

        fp.add(fpn);

        // screen dimensions panel
        JPanel sdp = new JPanel();
        BoxLayout sdpLayout = new BoxLayout(sdp, BoxLayout.X_AXIS);
        sdp.setLayout(sdpLayout);
        sdp.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("conf.labelDimensions")));

        // Group the radio buttons.
        ButtonGroup sdGroup = new ButtonGroup();
        sdGroup.add(sdNormal);
        sdGroup.add(sdBig);

        sdp.add(sdNormal);
        sdp.add(sdBig);

        // code page panel
        JPanel cp = new JPanel();
        BoxLayout cpLayout = new BoxLayout(cp, BoxLayout.X_AXIS);
        cp.setLayout(cpLayout);
        cp.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("conf.labelCodePage")));
        cp.add(cpb);
        cp.add(jtb);

        // emulation mode panel
        JPanel ep = new JPanel();
        BoxLayout epLayout = new BoxLayout(ep, BoxLayout.X_AXIS);
        ep.setLayout(epLayout);
        ep.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("conf.labelEmulateMode")));

        ep.add(ec);

        // title to be use panel
        JPanel tp = new JPanel();
        BoxLayout tpLayout = new BoxLayout(tp, BoxLayout.X_AXIS);
        tp.setLayout(tpLayout);
        tp.setBorder(BorderFactory.createTitledBorder(
                ""));

        addLabelComponent("",
                tc,
                tp);

        // add all options to Options panel
        op.add(fp);
        op.add(sdp);
        op.add(cp);
        op.add(ep);
        op.add(tp);

        //System Id panel
        JPanel sprox = new JPanel();

        AlignLayout spal = new AlignLayout(2, 5, 5);

        sprox.setLayout(spal);
        sprox.setBorder(BorderFactory.createEtchedBorder());


        addLabelComponent("",
                useProxy,
                sprox);

        addLabelComponent(LangTool.getString("conf.labelProxyHost"),
                proxyHost,
                sprox);

        addLabelComponent(LangTool.getString("conf.labelProxyPort"),
                proxyPort,
                sprox);

        confTabs.addTab(LangTool.getString("conf.tabGeneral"), snp);
        confTabs.addTab(LangTool.getString("conf.tabTCP"), sip);
        confTabs.addTab(LangTool.getString("conf.tabOptions"), op);
        confTabs.addTab(LangTool.getString("conf.tabProxy"), sprox);

        if (systemName.getText().trim().length() <= 0) {
            confTabs.setEnabledAt(1, false);
            confTabs.setEnabledAt(2, false);
            confTabs.setEnabledAt(3, false);
        }


        systemName.setAlignmentX(Component.CENTER_ALIGNMENT);
        systemId.setAlignmentX(Component.CENTER_ALIGNMENT);
        fpn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cpb.setAlignmentX(Component.CENTER_ALIGNMENT);


        Object[] message = new Object[1];
        message[0] = confTabs;

        options = new JButton[2];
        String title;

        final String propKey2 = propKey;

        if (propKey2 == null) {
            Action add = new AbstractAction(LangTool.getString("conf.optAdd")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    doConfigureAction(propKey2);
                }
            };
            options[0] = new JButton(add);
            ((JButton) options[0]).setEnabled(false);
            title = LangTool.getString("conf.addEntryATitle");
        } else {
            Action edit = new AbstractAction(LangTool.getString("conf.optEdit")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    doConfigureAction(propKey2);
                }
            };
            options[0] = new JButton(edit);
            title = LangTool.getString("conf.addEntryETitle");
        }

        Action cancel = new AbstractAction(LangTool.getString("conf.optCancel")) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        };
        options[1] = new JButton(cancel);

        JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null,
                options, options[0]);

        pane.setInitialValue(options[0]);
        pane.setComponentOrientation(parent.getComponentOrientation());
        dialog = pane.createDialog(parent, title); //, JRootPane.PLAIN_DIALOG);

        dialog.setVisible(true);

        return systemName.getText();

    }


    /**
     * Return the list of available code pages depending on which character
     * mapping flag is set.
     *
     * @return list of available code pages
     */
    private static String[] getAvailableCodePages() {
        return CharMappings.getAvailableCodePages();
    }

    /**
     * React to the configuration action button to perform to Add or Edit the
     * entry
     *
     * @param propKey - key to act upon
     */
    private static void doConfigureAction(String propKey) {

        if (propKey == null) {
            props.put(systemName.getText(), toArgString());
        } else {
            props.setProperty(systemName.getText(), toArgString());
        }
        dialog.dispose();

    }

    /**
     * React on the state change for radio buttons
     *
     * @param e Item event to react to
     */
    private static void doItemStateChanged(ItemEvent e) {

        deviceName.setEnabled(true);

        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (sdn.isSelected()) {
                deviceName.setEnabled(false);
            }
        }
    }

    /**
     * Available Code Page selection state change
     *
     * @param e Item event to react to changes
     */
    private static void doCPStateChanged(ItemEvent e) {

        String[] availCP = getAvailableCodePages();
        cpb.removeAllItems();
        cpb.addItem(LangTool.getString("conf.labelDefault"));

        for (String s : availCP) {
            cpb.addItem(s);
        }
    }

    private static void addLabelComponent(String text, Component comp, Container container) {

        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalTextPosition(JLabel.LEFT);
        container.add(label);
        container.add(comp);

    }

    private static void doSomethingEntered() {

        confTabs.setEnabledAt(1, true);
        confTabs.setEnabledAt(2, true);
        confTabs.setEnabledAt(3, true);
        ((JButton) options[0]).setEnabled(true);
        newJVM.setEnabled(true);
        noEmbed.setEnabled(true);
        deamon.setEnabled(true);
    }

    private static void doNothingEntered() {

        confTabs.setEnabledAt(1, false);
        confTabs.setEnabledAt(2, false);
        confTabs.setEnabledAt(3, false);
        ((JButton) options[0]).setEnabled(false);
        newJVM.setEnabled(false);
        noEmbed.setEnabled(false);
        deamon.setEnabled(false);

    }

    private static String toArgString() {

        StringBuilder sb = new StringBuilder();
        sb.append(StoredArguments.quote(systemId.getText()));

        // port
        if (port.getText() != null)
            if (!port.getText().trim().isEmpty())
                sb.append(" --host-port " + port.getText().trim());

        if (fpn.getText() != null)
            if (!fpn.getText().isEmpty())
                sb.append(" --config " + StoredArguments.quote(fpn.getText()));
        if (!LangTool.getString("conf.labelDefault").equals(
                cpb.getSelectedItem()))
            sb.append(" --code-page " + StoredArguments.quote((String) cpb.getSelectedItem()));

        if (!TN5250jConstants.SSL_TYPE_NONE.equals(sslType.getSelectedItem()))
            sb.append(" --ssl-type " + (String) sslType.getSelectedItem());

        if (ec.isSelected())
            sb.append(" --enhanced");

        if (tc.isSelected())
            sb.append(" --name-from-system");

        if (!sdNormal.isSelected())
            sb.append(" --wide");

        if (deviceName.getText() != null && !sdn.isSelected())
            if (!deviceName.getText().trim().isEmpty())
                if (deviceName.getText().trim().length() > 10)
                    sb.append(" --device-name " + deviceName.getText().trim().substring(0, 10).toUpperCase());
                else
                    sb.append(" --device-name " + deviceName.getText().trim().toUpperCase());

        if (sdn.isSelected())
            sb.append(" --device-name-from-hostname");

        if (useProxy.isSelected())
            sb.append(" --proxy");

        if (proxyHost.getText() != null)
            if (!proxyHost.getText().isEmpty())
                sb.append(" --proxy-host " + StoredArguments.quote(proxyHost.getText()));

        if (proxyPort.getText() != null)
            if (!proxyPort.getText().isEmpty())
                sb.append(" --proxy-port " + proxyPort.getText());

        if (noEmbed.isSelected())
            sb.append(" --new-window ");

        if (deamon.isSelected())
            sb.append(" --daemon ");

        if (newJVM.isSelected())
            sb.append(" --new-instance ");

        if (heartBeat.isSelected())
            sb.append(" --heartbeat ");

        if (remoteEndpoint != null)
            sb.append(" --remote ").append(remoteEndpoint);
        if (remoteAuthToken != null)
            sb.append(" --remote-token ").append(StoredArguments.quote(remoteAuthToken));

        return sb.toString();
    }

    private static class SomethingEnteredDocument extends PlainDocument {

        private static final long serialVersionUID = 1L;

        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {

            super.insertString(offs, str, a);
            if (!getText(0, getLength()).isEmpty())
                doSomethingEntered();
        }

        public void remove(int offs, int len) throws BadLocationException {
            super.remove(offs, len);
            if (getText(0, getLength()).isEmpty())
                doNothingEntered();
        }
    }
}
