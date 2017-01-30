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
package ru.rd.scheduling.leveled.TimeTables;

import ru.rd.scheduling.leveled.CalendarSetter;
import ru.rd.scheduling.leveled.TimeTable;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class GenericTimeTable implements TimeTable {
    private final Logger m_log;
    protected final Calendar m_calendar;
    private CalendarSetter m_start;
    private CalendarSetter m_stop;
    private int m_periodFieldId;

    protected void info(String mes, String msg, Object[] pars) {
        if (m_log != null) {
            m_log.logp(Level.INFO, getClass().getName(), mes, msg, pars);
        }
    }

    public GenericTimeTable(Logger logger, int periodFieldId) {
        m_calendar = new GregorianCalendar();
        m_log = logger;
        m_periodFieldId = periodFieldId;
    }

    public CalendarSetter getStart() {
        return m_start;
    }

    public void setStart(CalendarSetter start) {
        m_start = start;
    }

    public CalendarSetter getStop() {
        return m_stop;
    }

    public void setStop(CalendarSetter stop) {
        m_stop = stop;
    }

    public void setStartField(int fieldId, int value) {
        m_start.addFieldValue(fieldId, value);
    }

    public void setStopField(int fieldId, int value) {
        m_stop.addFieldValue(fieldId, value);
    }

    private void setStopTime() {
        if (m_stop == null) {
            if (m_start == null) {
                m_calendar.setTimeInMillis(Long.MAX_VALUE);
            } else {
                m_calendar.add(m_periodFieldId, 1);
            }
        } else {
            m_stop.setCalendar(m_calendar);
        }
    }

    private void setStartTime() {
        if (m_start != null) {
            m_start.setCalendar(m_calendar);
        }
    }

    protected void setActualMin(int fieldId) {
        if (fieldId == Calendar.DAY_OF_WEEK) {
            m_calendar.set(fieldId, m_calendar.getFirstDayOfWeek());
        } else {
            int am = m_calendar.getActualMinimum(fieldId);
            m_calendar.set(fieldId, am);
        }
    }

    protected abstract void clearFields();

    public Date getAmbientStart(final Date dt) {
        m_calendar.setTime(dt);
        clearFields();
        Date baseDate = m_calendar.getTime();
        if (m_stop != null) {
            setStopTime();
            if (dt.after(m_calendar.getTime())) {
                m_calendar.setTime(baseDate);
                m_calendar.add(m_periodFieldId, 1);
                baseDate = m_calendar.getTime();
            }
        }
        m_calendar.setTime(baseDate);
        setStartTime();
        return m_calendar.getTime();
    }

    public Date getNextStart(final Date start) {
        m_calendar.setTime(start);
        m_calendar.add(m_periodFieldId, 1);

        if (m_log != null) {
            m_log.logp(
                Level.FINE, getClass().getName(), "Time-table getNextStart",
                "Time-table getNextStart {0}" +
                "\ncur-start = {1,date} {1,time} | next-start = {2,date} {2,time}",
                new Object[] {this, start, m_calendar.getTime()}
            );
        }

        return m_calendar.getTime();
    }

    public Date getNextStop(final Date start) {
        if (m_stop == null) {
            if (m_log != null) {
                m_log.log(
                    Level.FINE,
                    "Time-table getNextStop: {0}" +
                    "\ncur-start = {1,date} {1,time} | next-stop = never",
                    new Object[] {this, start}
                );
            }
            return null;
        }
        m_calendar.setTime(start);
        clearFields();
        setStopTime();

        if (m_log != null) {
            m_log.log(
                Level.FINE,
                "Time-table getNextStop: {0}" +
                "\ncur-start = {1,date} {1,time} | next-stop = {2,date} {2,time}",
                new Object[] {this, start, m_calendar.getTime()}
            );
        }

        return m_calendar.getTime();
    }

    public boolean isBetween(GenericTimeTable tt) {
        if (getStop().isGreater(tt.getStop())) return false;
        CalendarSetter start2 = tt.getStart();
        return getStart().isGreater(start2) || getStart().equals(start2);
    }

    public String toString() {
        return (
            "period-field = " + m_periodFieldId +
            "\nstart: " + m_start + " stop: " + m_stop
        );
    }
}
