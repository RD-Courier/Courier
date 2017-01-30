/*
 * Copyright 2005-2017 Courier AUTHORS: please see AUTHORS file.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY AUTHORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package ru.rd.courier.manager;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.manager.message.ManagerInfoMessage;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.courier.manager.message.CourierInfoMessage;
import ru.rd.net.AbstractASynchSynchClient;
import ru.rd.net.message.CheckMessage;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TimerTask;

/**
 * User: STEPOCHKIN
 * Date: 29.09.2008
 * Time: 23:33:13
 */
public class ManagedCourier {
    private final Manager m_manager;
    public final int id;
    private boolean m_closed;
    public CourierInfo info;
    private final IoSession m_mainSession;
    private final AbstractASynchSynchClient m_client;
    private final Collection<IoSession> m_statSessions = new LinkedList<IoSession>();
    private String m_host;
    private Collection<ManagedCourierListener> m_listeners;
    private TimerTask m_checkTask;
    private final long m_checkInterval;

    public ManagedCourier(
        Manager manager, int id,
        IoSession mainSession, long checkInterval
    ) throws Exception {
        m_closed = false;
        m_manager = manager;
        this.id = id;
        info = null;
        m_mainSession = mainSession;
        m_checkInterval = checkInterval;
        m_client = new AbstractASynchSynchClient(getLogger(), mainSession);
    }

    public void handShake(CourierInfoMessage info) throws Exception {
        synchronized(this) {
            if (m_closed) return;
        }
        this.info = info;
        WriteFuture wf = m_mainSession.write(new ManagerInfoMessage(id, m_manager.getStatPort()));
        if (!wf.join(getTimeout())) {
            getLogger().warning("Receive manager info timeout " + getTimeout());
            dispose();
            return;
        }
        m_manager.courierConnected(this);
        m_checkTask = new TimerTask() {
            public void run() { checkConnection(); }
        };
        m_manager.getTimer().schedule(m_checkTask, m_checkInterval, m_checkInterval);
    }

    void messageReceived(IoSession session, Object message) throws Exception {
        m_client.resultReceived(message);
    }

    private long m_checkId = 0;

    private void checkConnection() {
        if (m_closed || info == null) return;
        boolean cv;
        try {
            CheckMessage m = new CheckMessage(m_checkId++);
            CheckMessage bm = (CheckMessage)m_client.write(m, getTimeout());
            cv = m.equals(bm);
        } catch (Exception e) {
            warn("Connection check error", e) ;
            cv = false;
        }
        if (!cv) {
            debug("Connection check failed");
            dispose();
        }
    }

    private void debug(String message) {
        getLogger().debug("ManagedCourier " + toString() + ": " + message);
    }

    private void warn(String message, Throwable e) {
        getLogger().warning("ManagedCourier " + toString() + ": " + message, e);
    }

    private CourierLogger getLogger() {
        return m_manager.getLogger();
    }

    private long getTimeout() {
        return m_manager.getTimeout();
    }

    public IoSession getSession() {
        return m_mainSession;
    }

    public String getHost() {
        if (m_host == null) {
            InetSocketAddress a = (InetSocketAddress)getSession().getRemoteAddress();
            m_host = a.getHostName();
        }
        return m_host;
    }

    public String getConfig() {
        return info.getCode();
    }

    public String toString() {
        return "ID=" + id + " (" + info + ")";
    }

    public void addStatSession(IoSession session) {
        m_statSessions.add(session);
    }

    public void removeStatSession(IoSession session) {
        m_statSessions.remove(session);
        session.close();
    }

    private void closeSession(IoSession session) {
        try {
            CloseFuture cf = session.close();
            if (!cf.join(1000)) {
                getLogger().warning("Session " + session + " close timeout");
            }
        } catch (Exception e) { getLogger().warning(e); }
    }

    public void processResult(ProcessResult result){
        if (m_listeners != null) {
            for (ManagedCourierListener l: m_listeners) {
                l.processResult(result);
            }
        }
    }

    public void dispose() {
        synchronized(this) {
            if (m_closed) return;
            m_closed = true;
        }
        if (m_checkTask != null) m_checkTask.cancel();
        try {
            m_manager.courierDisconnected(this);
        } catch(Exception e) { getLogger().warning("Courier " + toString() + " error", e); }
        if (m_listeners != null) {
            for (ManagedCourierListener l: m_listeners) {
                try {
                    l.courierClosing();
                } catch(Exception e) { getLogger().warning(e); }
            }
        }
        closeSession(m_mainSession);
        for (IoSession session: m_statSessions) {
            closeSession(session);
        }
    }

    public synchronized boolean isActive() {
        return !m_closed;
    }

    public void addListener(ManagedCourierListener l) {
        if (m_listeners == null) m_listeners = new LinkedList<ManagedCourierListener>();
        m_listeners.add(l);
    }

    public void removeListener(ManagedCourierListener l) {
        if (m_listeners == null) return;
        m_listeners.remove(l);
    }
}
