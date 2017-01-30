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
package ru.rd.scheduling.leveled;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

/**
 * User: AStepochkin
 * Date: 13.03.2009
 * Time: 10:02:46
 */
public abstract class StartStopContainerBase implements StartStopSet {
    protected final String m_desc;
    protected Logger m_log = null;
    protected Collection<StartStopListener> m_listeners = new LinkedList<StartStopListener>();
    private boolean m_active = false;
    private Date m_parentStart = null;

    public StartStopContainerBase(String desc, Logger logger) {
        if (logger == null) {
            m_log = Logger.getLogger(getClass().getPackage().getName());
        } else {
            m_log = logger;
        }
        m_desc = desc;
    }

    protected void info(final String method, final String msg, final Object[] pars) {
        if (m_log != null) {
            String desc = (m_desc == null || m_desc.length() == 0) ? "" : " ''" + m_desc + "''";
            m_log.log(Level.FINE, "StartStopContainer" + desc + ": " + msg, pars);
        }
    }

    protected void info(final String method, final String msg) {
        if (m_log != null) {
            String desc = (m_desc == null || m_desc.length() == 0) ? "" : " '" + m_desc + "'";
            m_log.log(Level.FINE, "StartStopContainer" + desc + ": " + msg);
        }
    }

    public String getDesc() {
        return m_desc;
    }

    synchronized public void addListener(final StartStopListener l) {
        if (l == null) throw new RuntimeException("Listener cannot be null");
        m_listeners.add(l);
        if (m_active) l.start(m_parentStart);
        else l.stop();
    }

    synchronized public void removeListener(final StartStopListener l) {
        if (l == null) throw new RuntimeException("Listener cannot be null");
        if (m_listeners.remove(l)) {
            if (m_active) l.stop();
        }
    }

    synchronized public void removeListeners() {
        Iterator<StartStopListener> it = m_listeners.iterator();
        while (it.hasNext()) {
            StartStopListener l = it.next();
            if (m_active) l.stop();
            it.remove();
        }
    }

    synchronized public boolean isEmpty() {
        return m_listeners.isEmpty();
    }

    synchronized public boolean isActive() {
        return m_active;
    }

    synchronized public void startListeners(final Date parentStart) {
        if (m_active) return;
        info(
            "start",
            "start: parent-start = {0,date} {0, time}",
            new Object[] {parentStart}
        );
        m_active = true;
        m_parentStart = parentStart;
        Collection<StartStopListener> listeners = new ArrayList<StartStopListener>(m_listeners);
        for (StartStopListener l: listeners) {
            l.start(parentStart);
        }
    }

    synchronized public void stopListeners() {
        if (!m_active) return;
        info("stop", "stop");
        m_active = false;
        Collection<StartStopListener> listeners = new ArrayList<StartStopListener>(m_listeners);
        for (StartStopListener l: listeners) {
            l.stop();
        }
    }
}
