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

import ru.rd.scheduling.leveled.TimeTable;

import java.util.*;

/**
 * User: AStepochkin
 * Date: 18.03.2009
 * Time: 14:57:52
 */
public class DayListTimeTable implements TimeTable {
    private List<Date> m_days = new ArrayList<Date>();
    protected final Calendar m_calendar;

    public DayListTimeTable() {
        m_calendar = new GregorianCalendar();
    }

    private void setActualMin(int fieldId) {
        if (fieldId == Calendar.DAY_OF_WEEK) {
            m_calendar.set(fieldId, m_calendar.getFirstDayOfWeek());
        } else {
            int am = m_calendar.getActualMinimum(fieldId);
            m_calendar.set(fieldId, am);
        }
    }

    private void clearFields() {
        setActualMin(Calendar.HOUR_OF_DAY);
        setActualMin(Calendar.MINUTE);
        setActualMin(Calendar.SECOND);
        setActualMin(Calendar.MILLISECOND);
    }

    public synchronized void addDay(int year, int month, int day) {
        Calendar c = new GregorianCalendar(year, month, day);
        Date d = c.getTime();
        int idx = Collections.binarySearch(m_days, d);
        if (idx < 0) {
            m_days.add(-idx - 1, d);
        }
    }

    public synchronized void addDay(DayInfo di) {
        addDay(di.year, di.month, di.day);
    }

    public synchronized Date getAmbientStart(Date dt) {
        m_calendar.setTime(dt);
        clearFields();
        Date baseDate = m_calendar.getTime();
        int idx = Collections.binarySearch(m_days, baseDate);
        if (idx >= 0) return baseDate;
        idx = -idx - 1;
        if (idx < m_days.size()) return m_days.get(idx);
        return null;
    }

    public synchronized Date getNextStart(Date start) {
        int idx = Collections.binarySearch(m_days, start);
        if (idx >= 0) {
            idx++;
            if (idx >= m_days.size()) return null;
            return m_days.get(idx);
        }
        idx = -idx - 1;
        if (idx >= m_days.size()) return null;
        return m_days.get(idx);
    }

    public synchronized Date getNextStop(Date start) {
        m_calendar.setTime(start);
        m_calendar.add(Calendar.DAY_OF_MONTH, 1);
        return m_calendar.getTime();
    }
}
