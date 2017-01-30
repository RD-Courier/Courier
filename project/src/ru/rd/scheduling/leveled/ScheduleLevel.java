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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduleLevel implements StartStopListenerSet {
    private final String m_desc;
    private Logger m_log = null;
    private TimeTable m_timeTable;
    private boolean m_skipAmbient;
    private Timer m_timer;
    private Date m_curStart;
    private TimerTask m_curTask;
    private boolean m_started;
    private boolean m_active;
    private Collection<StartStopListener> m_listeners = new LinkedList<StartStopListener>();

    private class TimerTaskWrapper extends TimerTask {
        private Runnable m_work;

        public TimerTaskWrapper(final Runnable work) {
            m_work = work;
        }

        public void run() {
            m_work.run();
        }
    }
    
    private Runnable m_startRunnable = new Runnable() {
        public void run() {
            try {
                startListeners();
            } catch (Exception e) {
                if (m_log == null) {
                    e.printStackTrace(System.err);
                } else {
                    m_log.log(Level.WARNING, "Error starting schedule level", e);
                }
            }
        }
    };

    private Runnable m_stopRunnable = new Runnable() {
        public void run() {
            stopListeners(true);
        }
    };

    protected void info(final String method, final String msg, final Object[] pars) {
        if (m_log != null) {
            m_log.logp(
                Level.FINE, getClass().getName(), method,
                "Level ''" + m_desc + "'': " + msg, pars
            );
        }
    }

    public ScheduleLevel(
        final String desc, final Logger logger,
        final TimeTable timeTable, final boolean skipAmbient,
        final Timer timer
    ) {
        m_log = logger;
        m_desc = desc;
        m_timeTable = timeTable;
        m_skipAmbient = skipAmbient;
        m_timer = timer;
        m_started = false;
    }

    public String getDesc() {
        return m_desc;
    }

    public synchronized TimeTable getTimeTable() {
        return m_timeTable;
    }

    public synchronized void setTimetable(TimeTable timeTable) {
        if (timeTable == null) throw new NullPointerException();
        boolean wasStarted = m_started;
        if (m_timeTable != null) {
            if (m_timeTable.equals(timeTable)) return;
            if (m_started) stop();
        }
        m_timeTable = timeTable;
        if (wasStarted) start(new Date());
    }

    public synchronized void addListener(final StartStopListener listener) {
        if (listener == null) throw new RuntimeException("Listener cannot be null");
        m_listeners.add(listener);
        if (m_active) {
            listener.start(m_curStart);
        } else {
            listener.stop();
        }
    }

    public synchronized void removeListener(StartStopListener listener) {
        if (listener == null) throw new RuntimeException("Listener cannot be null");
        if (m_listeners.remove(listener)) {
            if (m_active) listener.stop();
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

    public boolean isEmpty() {
        return m_listeners.isEmpty();
    }

    private synchronized void startListeners() {
        if (m_active) return;
        m_active = true;
        final Date stopTime = m_timeTable.getNextStop(m_curStart);
        info(
            "startListeners",
            "start-listeners: curStart = {0,date} {0, time} | stopTime = {1,date} {1,time}",
            new Object[] {m_curStart, stopTime}
        );
        if (stopTime != null) {
            m_curTask = new TimerTaskWrapper(m_stopRunnable);
            m_timer.schedule(m_curTask, stopTime);
        }
        Collection<StartStopListener> listeners = new ArrayList<StartStopListener>(m_listeners);
        for (StartStopListener l: listeners) {
            l.start(m_curStart);
        }
    }

    private synchronized void stopListeners(boolean scheduleStart) {
        if (!m_active) return;
        info("stopListeners", "stop-listeners: next-start = {0,date} {0,time}", new Object[] {m_curStart});
        m_active = false;
        Collection<StartStopListener> listeners = new ArrayList<StartStopListener>(m_listeners);
        for (StartStopListener l: listeners) {
            l.stop();
        }
        if (scheduleStart) {
            m_curStart = m_timeTable.getNextStart(m_curStart);
            if (m_curStart != null) {
                m_curTask = new TimerTaskWrapper(m_startRunnable);
                m_timer.schedule(m_curTask, m_curStart);
            }
        }
    }

    public synchronized void start(final Date parentStart) {
        if (m_started) stop();
        //m_parentStart = (parentStart == null) ? new Date() : parentStart;
        final Date ambientStart = m_timeTable.getAmbientStart(new Date());
        if (m_skipAmbient && ambientStart != null && ambientStart.before(parentStart)) {
            m_curStart = m_timeTable.getNextStart(ambientStart);
        } else {
            m_curStart = ambientStart;
        }

        if (m_curStart == null) {
            m_curStart = new Date(Long.MAX_VALUE);
        }
        info(
            "start",
            "starting: parent-start = {0,date} {0,time} | ambient-start = {1,date} {1,time} | cur-start = {2,date} {2,time}",
            new Object[] {parentStart, ambientStart, m_curStart}
        );
        m_curTask = new TimerTaskWrapper(m_startRunnable);
        m_timer.schedule(m_curTask, m_curStart);
        m_started = true;
    }

    public synchronized void stop() {
        if (!m_started) return;

        info("stop", "stopping: cur-start = {0,date} {0,time}", new Object[] {m_curStart});
        if (m_curTask != null) {
            m_curTask.cancel();
            m_curTask = null;
        }
        stopListeners(false);
        m_started = false;
    }
}
