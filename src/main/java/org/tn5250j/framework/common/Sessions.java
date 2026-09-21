/*
 * @(#)Sessions.java
 * Copyright:    Copyright (c) 2001
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 *
 */
package org.tn5250j.framework.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.tn5250j.Session5250;
import org.tn5250j.interfaces.SessionsInterface;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;


/**
 * Contains a collection of Session objects. This list is a static snapshot
 * of the list of Session objects available at the time of the snapshot.
 */
public class Sessions implements SessionsInterface {

    private static final long HEART_BEAT_INTERVAL_MS = 15000L;

    private List<Session5250> sessions = null;
    private int count = 0;
    private Timer heartBeater;

    private final TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());

    public Sessions() {

        sessions = new ArrayList<>();
    }

    private void sendHeartBeats() {
        List<Session5250> snapshot;
        synchronized (sessions) {
            snapshot = new ArrayList<>(sessions);
        }

        for (Session5250 ses : snapshot) {
            try {
                if (ses.isConnected() && ses.isSendKeepAlive()) {
                    ses.getVT().sendHeartBeat();
                    if (log.isDebugEnabled()) {
                        log.debug(" sent heartbeat to " + ses.getSessionName());
                    }
                }
            } catch (Exception ex) {
                log.warn(ex.getMessage());
            }
        }

    }

    /** Caller must hold {@code sessions}. */
    private void startHeartBeater() {
        if (heartBeater != null) {
            return;
        }
        heartBeater = new Timer("tn5250j-keepalive", true);
        heartBeater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendHeartBeats();
            }
        }, HEART_BEAT_INTERVAL_MS, HEART_BEAT_INTERVAL_MS);
    }

    /** Caller must hold {@code sessions}. */
    private void cancelHeartBeaterIfIdle() {
        for (Session5250 session : sessions) {
            if (session.isSendKeepAlive()) {
                return;
            }
        }
        if (heartBeater != null) {
            heartBeater.cancel();
            heartBeater = null;
        }
    }

    protected void addSession(Session5250 newSession) {
        log.debug("adding Session: " + newSession.getSessionName());
        synchronized (sessions) {
            sessions.add(newSession);
            ++count;
            if (newSession.isSendKeepAlive()) {
                startHeartBeater();
            }
        }
    }

    protected void removeSession(Session5250 session) {
        if (session != null) {
            log.debug("Removing session: " + session.getSessionName());
            if (session.isConnected())
                session.disconnect();
            synchronized (sessions) {
                sessions.remove(session);
                --count;
                cancelHeartBeaterIfIdle();
            }
        }
    }

    protected void removeSession(String sessionName) {
        log.debug("Remove session by name: " + sessionName);
        removeSession(item(sessionName));

    }

    protected void removeSession(int index) {
        log.debug("Remove session by index: " + index);
//      removeSession((SessionGUI)(((Session5250)item(index)).getGUI()));
        removeSession(item(index));
    }

    public int getCount() {
        synchronized (sessions) {
            return count;
        }
    }

    public Session5250 item(int index) {
        synchronized (sessions) {
            return sessions.get(index);
        }
    }

    public Session5250 item(String sessionName) {
        synchronized (sessions) {
            Session5250 s = null;
            int x = 0;

            while (x < sessions.size()) {

                s = sessions.get(x);

                if (s.getSessionName().equals(sessionName))
                    return s;

                x++;
            }

            return null;
        }
    }

    public Session5250 item(Session5250 sessionObject) {
        synchronized (sessions) {
            Session5250 s = null;
            int x = 0;

            while (x < sessions.size()) {

                s = sessions.get(x);

                if (s.equals(sessionObject))
                    return s;

                x++;
            }

            return null;
        }
    }

    public ArrayList<Session5250> getSessionsList() {
        synchronized (sessions) {
            ArrayList<Session5250> newS = new ArrayList<>(sessions.size());
            for (Session5250 session : sessions) newS.add(session);
            return newS;
        }
    }

    public void refresh() {


    }


}
