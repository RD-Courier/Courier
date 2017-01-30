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
package ru.rd.scheduling.leveled.launchers;

import ru.rd.scheduling.leveled.CalendarInterval;
import ru.rd.scheduling.leveled.StartStopListener;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CalendarPeriodicLauncher implements StartStopListener {
    private Logger m_logger;
    private final String m_desc;
    private int m_interval;
    private int m_calendarField;
    private CalendarInterval m_offset;
    private Timer m_timer;
    private Runnable m_work = null;
    private TimerTask m_curTask = null;
    private Calendar m_curLaunchDate = new GregorianCalendar();
    private boolean m_active;
    protected int[] m_clearFields;

    public CalendarPeriodicLauncher(
        Logger logger, String desc, int calendarField, int interval,
        Runnable work, CalendarInterval offset, Timer timer
    ) {
        m_logger = logger;
        m_desc = desc;
        m_calendarField = calendarField;
        m_interval = interval;
        if (offset != null) {
            m_offset = offset;
        } else {
            m_offset = new CalendarInterval();
        }
        m_work = work;
        m_timer = (timer == null) ? new Timer(): timer;
    }

    public CalendarPeriodicLauncher(
        Logger logger, String desc, int calendarField, int interval,
        Runnable work, CalendarInterval offset
    ) {
        this(logger, desc, calendarField, interval, work, offset, null);
    }

    public CalendarPeriodicLauncher(
        Logger logger, String desc, int calendarField, int interval, Runnable work
    ) {
        this(logger, desc, calendarField, interval, work, null, null);
    }

    protected void setActualMin(int fieldId) {
        m_curLaunchDate.set(fieldId, m_curLaunchDate.getActualMinimum(fieldId));
    }

    protected final void setToBase() {
        for (int i = m_clearFields.length - 1; i >= 0; i--) {
            setActualMin(m_clearFields[i]);
        }
    }

    private void nextLaunchDate() {
        m_curLaunchDate.add(m_calendarField, m_interval);
    }

    private class LaunchTimerTask extends TimerTask {
        public LaunchTimerTask() {}
        public void run() {
            synchronized(CalendarPeriodicLauncher.this) {
                if (!m_active) return;
                m_work.run();
                nextLaunchDate();
                m_curTask = new LaunchTimerTask();

                m_logger.log(
                    Level.FINE, "PeriodicLauncher ''{0}'' launch: next-launch-time = {1,date} {1,time}",
                    new Object[] {m_desc, m_curLaunchDate.getTime()}
                );

                m_timer.schedule(m_curTask, m_curLaunchDate.getTime());
            }
        }
    }

    synchronized public void start(final Date parentStart) {
        m_active = true;
        m_curLaunchDate.setTime(new Date());
        setToBase();
        if (m_offset != null) m_offset.shiftCalendar(m_curLaunchDate);
        m_curTask = new LaunchTimerTask();

        m_logger.logp(
            Level.FINE, getClass().getName(), "start",
            "PeriodicLauncher ''{0}'' start: next-launch-time = {1,date} {1,time}",
            new Object[] {m_desc, m_curLaunchDate.getTime()}
        );

        m_timer.schedule(m_curTask, m_curLaunchDate.getTime());
    }

    synchronized public void stop() {
        m_active = false;
        if (m_curTask != null) m_curTask.cancel();
    }

    synchronized public void addOffsetPart(int fieldId, int interval) {
        boolean allowedField = false;
        for (int i = 0; i < m_clearFields.length; i++) {
            if (m_clearFields[i] == fieldId) {
                allowedField = true;
                break;
            }
        }
        if (!allowedField) {
            m_logger.log(
                Level.WARNING, "Launcher '" + m_desc + "': suspicious calendar field {0}",
                new Integer(fieldId)
            );
        }
        m_offset.addFieldInterval(fieldId, interval);
    }
}
