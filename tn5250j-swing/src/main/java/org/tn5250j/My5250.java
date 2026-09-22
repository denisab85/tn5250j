/**
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
package org.tn5250j;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.tn5250j.connectdialog.ConnectDialog;
import org.tn5250j.cli.DesktopOptions;
import org.tn5250j.cli.StoredArguments;
import picocli.CommandLine;
import org.tn5250j.event.BootEvent;
import org.tn5250j.event.BootListener;
import org.tn5250j.event.EmulatorActionEvent;
import org.tn5250j.event.EmulatorActionListener;
import org.tn5250j.event.SessionChangeEvent;
import org.tn5250j.event.SessionListener;
import org.tn5250j.framework.Tn5250jController;
import org.tn5250j.framework.common.SessionManager;
import org.tn5250j.framework.common.Sessions;
import org.tn5250j.gui.TN5250jSplashScreen;
import org.tn5250j.interfaces.ConfigureFactory;
import org.tn5250j.interfaces.GUIViewInterface;
import org.tn5250j.session.api.ConnectionProfile;
import org.tn5250j.session.api.SessionClient;
import org.tn5250j.session.client.SessionClientFactory;
import org.tn5250j.session.server.SessionServerMain;
import org.tn5250j.tools.LangTool;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

public class My5250 implements BootListener, SessionListener, EmulatorActionListener {

    private GUIViewInterface frame1;
    private String[] sessionArgs = null;
    private DesktopOptions launchOptions = DesktopOptions.parse();
    private static Properties sessions = new Properties();
    private static BootStrapper strapper = null;
    private final SessionManager manager;
    private static List<GUIViewInterface> frames;
    private final TN5250jSplashScreen splash;
    private int step;
    private StringBuilder viewNamesForNextStartBuilder = null;

    private final TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());

    My5250() {

        splash = new TN5250jSplashScreen("tn5250jSplash.jpg");
        splash.setSteps(5);
        splash.setVisible(true);

        loadLookAndFeel();


        loadSessions();
        splash.updateProgress(++step);

        initJarPaths();

        initScripting();

        // sets the starting frame type.  At this time there are tabs which is
        //    default and Multiple Document Interface.
//		startFrameType();

        frames = new ArrayList<>();

        newView();

        setDefaultLocale();
        manager = SessionManager.instance();
        splash.updateProgress(++step);
        Tn5250jController.getCurrent();
    }


    /**
     * we only want to try and load the Nimbus look and feel if it is not
     * for the MAC operating system.
     */
    private void loadLookAndFeel() {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // we don't care. Cause this should always work.
            }
        }
    }

    /**
     * Check if there are any other instances of tn5250j running
     */
    static private boolean checkBootStrapper(String[] args) {

        try {
            Socket boot = new Socket("localhost", 3036);

            PrintWriter out = new PrintWriter(boot.getOutputStream(), true);

            // parse args into a string to send to the other instance of
            //    tn5250j
            String opts = args.length == 0 ? "null" : StoredArguments.join(args);
            out.println(opts);
            out.flush();
            out.close();
            boot.close();
            return true;

        } catch (UnknownHostException e) {
            // TODO: Should be logged @ DEBUG level
            //         System.err.println("localhost not known.");
        } catch (IOException e) {
            // TODO: Should be logged @ DEBUG level
            //         System.err.println("No other instances of tn5250j running.");
        }

        return false;
    }

    public void bootOptionsReceived(BootEvent bootEvent) {
        ConfigureFactory.getInstance().reloadSettings();
        loadSessions();
        try {
            String value = bootEvent.getNewSessionOptions();
            DesktopOptions options = DesktopOptions.parse("null".equals(value) ? new String[0] : StoredArguments.split(value));
            if (options.server || options.help) return;
            SwingUtilities.invokeLater(() -> {
                launchOptions = options;
                if (options.session.host != null) newSession(options.session.host, new String[0]);
                else if (!options.sessions.isEmpty()) startSessionsFromList(this, filterExistingViewNames(options.sessions));
                else openConnectSessionDialogAndStartSelectedSession();
            });
        } catch (IllegalArgumentException | CommandLine.ParameterException ex) {
            log.warn("Invalid forwarded desktop options: " + ex.getMessage());
        }
    }

    static public void main(String[] args) {
        DesktopOptions options = new DesktopOptions();
        CommandLine command = options.commandLine();
        command.setExecutionStrategy(result -> {
            options.validate();
            if (CommandLine.printHelpIfRequested(result)) return 0;
            if (options.server) {
                try {
                    SessionServerMain.run(options.listener);
                } catch (Exception ex) {
                    throw new CommandLine.ExecutionException(command, "Failed to start session server: " + ex.getMessage(), ex);
                }
            } else {
                launch(options, args);
            }
            return 0;
        });
        int exit = command.execute(args);
        if (exit != 0) System.exit(exit);
    }

    private static void launch(DesktopOptions options, String[] args) {
        if (!options.newInstance) {

            if (!checkBootStrapper(args)) {

                // if we did not find a running instance and the -d options is
                //    specified start up the bootstrap daemon to allow checking
                //    for running instances
                if (options.daemon) {
                    strapper = new BootStrapper();

                    strapper.start();
                }
            } else {

                return;
            }
        }

        My5250 m = new My5250();
        m.launchOptions = options;

        if (strapper != null)
            strapper.addBootListener(m);

        if (options.width != null || options.height != null) {
            m.frame1.setSize(options.width == null ? m.frame1.getWidth() : options.width,
                    options.height == null ? m.frame1.getHeight() : options.height);
            m.frame1.centerFrame();
        }
        if (options.locale != null) Locale.setDefault(parseLocal(options.locale));
        LangTool.init();

        if (options.session.host != null) {
            m.newSession(options.session.host, new String[0]);
            return;
        }

        List<String> lastViewNames = new ArrayList<>();
        lastViewNames.addAll(loadLastSessionViewNames());
        lastViewNames.addAll(options.sessions);
        lastViewNames = filterExistingViewNames(lastViewNames);

        if (!lastViewNames.isEmpty()) {
            insertDefaultSessionIfConfigured(lastViewNames);
            startSessionsFromList(m, lastViewNames);
            if (sessions.containsKey("emul.showConnectDialog")) {
                m.openConnectSessionDialogAndStartSelectedSession();
            }
        } else {
            m.startNewSession();
        }

    }

    private static void startSessionsFromList(My5250 m, List<String> lastViewNames) {
        for (String viewName : lastViewNames) {
            if (!m.frame1.isVisible()) {
                m.splash.updateProgress(++m.step);
                m.splash.setVisible(false);
                m.frame1.setVisible(true);
                m.frame1.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            m.sessionArgs = StoredArguments.split(sessions.getProperty(viewName));
            m.newSession(viewName, m.sessionArgs);
        }
    }

    private static void insertDefaultSessionIfConfigured(List<String> lastViewNames) {
        if (getDefaultSession() != null && !lastViewNames.contains(getDefaultSession())) {
            lastViewNames.add(0, getDefaultSession());
        }
    }

    static List<String> loadLastSessionViewNamesFrom(String[] commandLineArgs) {
        return DesktopOptions.parse(commandLineArgs).sessions;
    }

    static List<String> loadLastSessionViewNames() {
        if (!sessions.containsKey("emul.startLastView")) {
            return new ArrayList<>();
        }
        String emulview = sessions.getProperty("emul.view", "").trim();
        if (emulview.isEmpty()) {
            return new ArrayList<>();
        }
        return DesktopOptions.parse(StoredArguments.split(emulview)).sessions;
    }

    static List<String> filterExistingViewNames(List<String> lastViewNames) {
        List<String> result = new ArrayList<>();
        for (String viewName : lastViewNames) {
            if (sessions.containsKey(viewName)) {
                result.add(viewName);
            }
        }
        return result;
    }

    private static boolean containsNotOnlyNullValues(String[] stringArray) {
        if (stringArray != null) {
            for (String s : stringArray) {
                if (s != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setDefaultLocale() {

        if (sessions.containsKey("emul.locale")) {
            Locale.setDefault(parseLocal(sessions.getProperty("emul.locale")));
        }

    }

    private static String getDefaultSession() {
        String defaultSession = sessions.getProperty("emul.default");
        if (defaultSession != null && !defaultSession.trim().isEmpty()) {
            return defaultSession;
        }
        return null;
    }

    private void startNewSession() {

        String sel = "";

        if (containsNotOnlyNullValues(sessionArgs) && !sessionArgs[0].startsWith("-")) {
            sel = sessionArgs[0];
        } else {
            sel = getDefaultSession();
        }

        Sessions sess = manager.getSessions();

        if (sel != null && sess.getCount() == 0 && sessions.containsKey(sel)) {
            sessionArgs = StoredArguments.split(sessions.getProperty(sel));
        }

        if (sessionArgs == null || sess.getCount() > 0 || sessions.containsKey("emul.showConnectDialog")) {
            openConnectSessionDialogAndStartSelectedSession();
        } else {
            newSession(sel, sessionArgs);
        }
    }


    private void openConnectSessionDialogAndStartSelectedSession() {
        String sel = openConnectSessionDialog();
        Sessions sess = manager.getSessions();
        if (sel != null) {
            String selArgs = sessions.getProperty(sel);
            sessionArgs = StoredArguments.split(selArgs);

            newSession(sel, sessionArgs);
        } else {
            if (sess.getCount() == 0)
                System.exit(0);
        }
    }

    private void startDuplicateSession(SessionPanel ses) {

        loadSessions();
        if (ses == null) {
            Sessions sess = manager.getSessions();
            for (int x = 0; x < sess.getCount(); x++) {

                if ((SessionPanel.of(sess.item(x)) != null)
                        && SessionPanel.of(sess.item(x)).isVisible()) {

                    ses = SessionPanel.of(sess.item(x));
                    break;
                }
            }
        }

        String selArgs = sessions.getProperty(ses.getSessionName());
        sessionArgs = StoredArguments.split(selArgs);

        newSession(ses.getSessionName(), sessionArgs);
    }

    private String openConnectSessionDialog() {

        splash.setVisible(false);
        ConnectDialog sc = new ConnectDialog(frame1, LangTool.getString("ss.title"), sessions);

        // load the new session information from the session property file
        loadSessions();
        return sc.getConnectKey();
    }

    private synchronized void newSession(String sel, String[] args) {
        DesktopOptions saved = DesktopOptions.parse(args);
        Properties sesProps = launchOptions.propertiesFor(saved);
        String propFileName = launchOptions.session.config != null ? launchOptions.session.config : saved.session.config;
        String session = sesProps.getProperty(TN5250jConstants.SESSION_HOST);
        boolean newWindow = launchOptions.session.newWindow || saved.session.newWindow;
        boolean nameFromSystem = launchOptions.session.nameFromSystem || saved.session.nameFromSystem;

        int sessionCount = manager.getSessions().getCount();

        SessionPanel s;
        ConnectionProfile profile = launchOptions.profileFor(sel, saved);
        if (profile != null) {
            SessionClient client = SessionClientFactory.create(profile);
            s = new SessionPanel(client);
        } else {
            Session5250 s2 = manager.openSession(sesProps, propFileName, sel);
            s = new SessionPanel(s2);
        }


        if (!frame1.isVisible()) {
            splash.updateProgress(++step);

            // Here we check if this is the first session created in the system.
            //  We have to create a frame on initialization for use in other scenarios
            //  so if this is the first session being added in the system then we
            //  use the frame that is created and skip the part of creating a new
            //  view which would increment the count and leave us with an unused
            //  frame.
            if (newWindow && sessionCount > 0) {
                newView();
            }
            splash.setVisible(false);
            frame1.setVisible(true);
            frame1.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else {
            if (newWindow) {
                splash.updateProgress(++step);
                newView();
                splash.setVisible(false);
                frame1.setVisible(true);
                frame1.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            }
        }

        if (nameFromSystem)
            frame1.addSessionView(sel, s);
        else
            frame1.addSessionView(session, s);

        s.connect();

        s.addEmulatorActionListener(this);
    }

    private void newView() {

        // we will now to default the frame size to take over the whole screen
        //    this is per unanimous vote of the user base
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int width = screenSize.width;
        int height = screenSize.height;

        if (sessions.containsKey("emul.width"))
            width = Integer.parseInt(sessions.getProperty("emul.width"));
        if (sessions.containsKey("emul.height"))
            height = Integer.parseInt(sessions.getProperty("emul.height"));

        frame1 = new Gui5250Frame(this);

        frame1.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        if (sessions.containsKey("emul.frame" + frame1.getFrameSequence())) {

            String location = sessions.getProperty("emul.frame" + frame1.getFrameSequence());
            //         System.out.println(location + " seq > " + frame1.getFrameSequence() );
            restoreFrame(frame1, location);
        } else {
            frame1.setSize(width, height);
            frame1.centerFrame();
        }

        frames.add(frame1);

    }

    private void restoreFrame(GUIViewInterface frame, String location) {

        StringTokenizer tokenizer = new StringTokenizer(location, ",");
        int x = Integer.parseInt(tokenizer.nextToken());
        int y = Integer.parseInt(tokenizer.nextToken());
        int width = Integer.parseInt(tokenizer.nextToken());
        int height = Integer.parseInt(tokenizer.nextToken());

        frame.setLocation(x, y);
        frame.setSize(width, height);
    }

    /**
     * @param view
     */
    protected void closingDown(GUIViewInterface view) {

        Sessions sess = manager.getSessions();

        if (log.isDebugEnabled()) {
            log.debug("number of active sessions we have " + sess.getCount());
        }

        if (viewNamesForNextStartBuilder == null) {
            // preserve sessions for next boot
            viewNamesForNextStartBuilder = new StringBuilder();
        }
        while (view.getSessionViewCount() > 0) {
            SessionPanel sesspanel = view.getSessionAt(0);
            viewNamesForNextStartBuilder.append("--session ")
                    .append(StoredArguments.quote(sesspanel.getSessionName()))
                    .append(" ");
            closeSessionInternal(sesspanel);
        }

        sessions.setProperty("emul.frame" + view.getFrameSequence(),
                view.getX() + "," +
                        view.getY() + "," +
                        view.getWidth() + "," +
                        view.getHeight());

        frames.remove(view);
        view.dispose();

        if (log.isDebugEnabled()) {
            log.debug("number of active sessions we have after shutting down " + sess.getCount());
        }

        log.info("view settings " + viewNamesForNextStartBuilder);
        if (sess.getCount() == 0) {

            sessions.setProperty("emul.width", Integer.toString(view.getWidth()));
            sessions.setProperty("emul.height", Integer.toString(view.getHeight()));

            sessions.setProperty("emul.view", viewNamesForNextStartBuilder.toString());

            // save off the session settings before closing down
            ConfigureFactory.getInstance().saveSettings(ConfigureFactory.SESSIONS,
                    ConfigureFactory.SESSIONS,
                    "------ Defaults --------");
            if (strapper != null) {
                strapper.interrupt();
            }
            System.exit(0);
        }


    }

    /**
     * Really closes the tab/session
     * @param sesspanel
     */
    protected void closeSessionInternal(SessionPanel sesspanel) {
        GUIViewInterface f = getParentView(sesspanel);
        if (f == null) {
            return;
        }
        Sessions sessions = manager.getSessions();
        if ((sessions.item(sesspanel.getSession())) != null) {
            f.removeSessionView(sesspanel);
            manager.closeSession(sesspanel.getSession());
        }
        if (manager.getSessions().getCount() < 1) {
            closingDown(f);
        }
    }

    private static Locale parseLocal(String localString) {
        if (localString.contains("-")) return Locale.forLanguageTag(localString);
        String[] parts = localString.split("_", 3);
        return new Locale(parts[0], parts.length > 1 ? parts[1] : "", parts.length > 2 ? parts[2] : "");
    }

    private static void loadSessions() {

        sessions = (ConfigureFactory.getInstance()).getProperties(
                ConfigureFactory.SESSIONS);
    }

    public void onSessionChanged(SessionChangeEvent changeEvent) {

        Session5250 ses5250 = (Session5250) changeEvent.getSource();
        SessionPanel ses = SessionPanel.of(ses5250);
        if (ses == null) {
            manager.closeSession(ses5250);
            return;
        }

        switch (changeEvent.getState()) {
            case TN5250jConstants.STATE_REMOVE:
                closeSessionInternal(ses);
                break;
        }
    }

    public void onEmulatorAction(EmulatorActionEvent actionEvent) {

        SessionPanel ses = (SessionPanel) actionEvent.getSource();

        switch (actionEvent.getAction()) {
            case EmulatorActionEvent.CLOSE_SESSION:
                closeSessionInternal(ses);
                break;
            case EmulatorActionEvent.CLOSE_EMULATOR:
                throw new UnsupportedOperationException("Not yet implemented!");
            case EmulatorActionEvent.START_NEW_SESSION:
                startNewSession();
                break;
            case EmulatorActionEvent.START_DUPLICATE:
                startDuplicateSession(ses);
                break;
        }
    }

    private GUIViewInterface getParentView(SessionPanel session) {

        GUIViewInterface f = null;

        for (GUIViewInterface frame : frames) {
            f = frame;
            if (f.containsSession(session))
                return f;
        }

        return null;

    }

    /**
     * Initializes the scripting environment if the jython interpreter exists
     * in the classpath
     */
    private void initScripting() {

        try {
            Class.forName("org.tn5250j.scripting.JPythonInterpreterDriver");
        } catch (NoClassDefFoundError | Exception ncdfe) {
            log.warn("Information Message: Can not find scripting support"
                    + " files, scripting will not be available: "
                    + "Failed to load interpreter drivers " + ncdfe);
        }

        splash.updateProgress(++step);

    }

    /**
     * Sets the jar path for the available jars.
     * Sets the python.path system variable to make the jython jar available
     * to scripting process.
     *
     * This needs to be rewritten to loop through and obtain all jars in the
     * user directory.  Maybe also additional paths to search.
     */
    private void initJarPaths() {

        String jarClassPaths = System.getProperty("python.path")
                + File.pathSeparator + "jython.jar"
                + File.pathSeparator + "jythonlib.jar"
                + File.pathSeparator + "jt400.jar"
                + File.pathSeparator + "itext.jar";

        if (sessions.containsKey("emul.scriptClassPath")) {
            jarClassPaths += File.pathSeparator + sessions.getProperty("emul.scriptClassPath");
        }

        System.setProperty("python.path", jarClassPaths);

        splash.updateProgress(++step);

    }

    static Properties getSessions() {
        return sessions;
    }

}
