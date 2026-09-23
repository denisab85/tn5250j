package org.tn5250j.debug;

import org.tn5250j.My5250;
import org.tn5250j.SessionPanel;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Resolves open Swing sessions and performs debug operations on the EDT.
 */
public final class SwingDebugService {

    public List<SessionSummary> listSessions() throws Exception {
        return onEdt(() -> {
            List<SessionSummary> sessions = new ArrayList<>();
            int id = 0;
            for (SessionPanel panel : My5250.allSessionPanels()) {
                sessions.add(new SessionSummary(id++, panel.getSessionName(), panel.isConnected()));
            }
            return sessions;
        });
    }

    public SwingRenderedScreen.Snapshot captureScreen(int sessionId) throws Exception {
        return onEdt(() -> {
            SessionPanel panel = requireSession(sessionId);
            return SwingRenderedScreen.capture(panel.getScreen());
        });
    }

    public void sendKeys(int sessionId, String keys) throws Exception {
        onEdt(() -> {
            requireSession(sessionId).debugSendKeys(keys);
            return null;
        });
    }

    public void sendText(int sessionId, String text) throws Exception {
        onEdt(() -> {
            requireSession(sessionId).debugSendText(text);
            return null;
        });
    }

    public void sendMouse(int sessionId, MouseRequest request) throws Exception {
        onEdt(() -> {
            SessionPanel panel = requireSession(sessionId);
            Point viewPoint = resolveViewPoint(panel, request);
            dispatchMouse(panel, viewPoint.x, viewPoint.y, request.button, request.clicks);
            return null;
        });
    }

    private static Point resolveViewPoint(SessionPanel panel, MouseRequest request) {
        if (request.row > 0 && request.col > 0) {
            Point point = new Point();
            panel.debugViewPointForCell(request.row, request.col, point);
            return point;
        }
        if (request.x >= 0 && request.y >= 0) {
            return new Point(request.x, request.y);
        }
        throw new IllegalArgumentException("Provide row/col (1-based) or non-negative x/y view coordinates.");
    }

    private static void dispatchMouse(SessionPanel panel, int x, int y, int button, int clicks) {
        int mask = button == MouseEvent.BUTTON3 ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
        long when = System.currentTimeMillis();
        MouseEvent click = new MouseEvent(
                panel,
                MouseEvent.MOUSE_CLICKED,
                when,
                mask,
                x,
                y,
                clicks,
                button == MouseEvent.BUTTON3,
                button);
        for (MouseListener listener : panel.getMouseListeners()) {
            listener.mouseClicked(click);
        }
        panel.getFocusForMe();
    }

    private static SessionPanel requireSession(int sessionId) {
        List<SessionPanel> panels = My5250.allSessionPanels();
        if (sessionId < 0 || sessionId >= panels.size()) {
            throw new IllegalArgumentException("Unknown session id: " + sessionId);
        }
        return panels.get(sessionId);
    }

    private static <T> T onEdt(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }
        final List<T> result = new ArrayList<>(1);
        final List<Exception> error = new ArrayList<>(1);
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.add(task.call());
            } catch (Exception ex) {
                error.add(ex);
            }
        });
        if (!error.isEmpty()) {
            throw error.get(0);
        }
        return result.isEmpty() ? null : result.get(0);
    }

    public static final class SessionSummary {
        public final int id;
        public final String name;
        public final boolean connected;

        public SessionSummary(int id, String name, boolean connected) {
            this.id = id;
            this.name = name;
            this.connected = connected;
        }
    }

    public static final class MouseRequest {
        public final int row;
        public final int col;
        public final int x;
        public final int y;
        public final int button;
        public final int clicks;

        public MouseRequest(int row, int col, int x, int y, int button, int clicks) {
            this.row = row;
            this.col = col;
            this.x = x;
            this.y = y;
            this.button = button;
            this.clicks = clicks;
        }
    }
}
