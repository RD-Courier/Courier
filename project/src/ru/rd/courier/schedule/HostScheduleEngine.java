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
package ru.rd.courier.schedule;

import ru.rd.scheduling.leveled.*;
import ru.rd.scheduling.leveled.TimeTables.WeekTimeTable;
import ru.rd.utils.Disposable;

import java.util.logging.Logger;
import java.util.Calendar;

/**
 * User: AStepochkin
 * Date: 01.04.2009
 * Time: 17:34:20
 */
public class HostScheduleEngine implements StartStopPlugin, Disposable {
    private final FileHostSchedules m_parent;
    private final StartStopListenerSet m_hostSchedule;
    private final StartStopMerger m_daySchedule;
    private final StartStopMerger m_hourSchedule;
    private boolean m_hasWeDays;
    private boolean m_hasPartHours;

    public HostScheduleEngine(String name, Logger logger, FileHostSchedules parent) {
        m_parent = parent;
        m_hostSchedule = new StartStopContainer(name + " host schedule", logger);
        m_daySchedule = new StartStopMerger(name + " day schedule", logger);
        m_hourSchedule = new StartStopMerger(name + " schedule", logger);
        installAllDays();
        installAllHours();
    }

    public synchronized void setAllDays() {
        if (!m_hasWeDays) return;
        clearWEDays();
        installAllDays();
    }

    public synchronized void setWEDays(String key) {
        if (m_hasWeDays) return;
        clearAllDays();
        installWEDays(key);
    }

    private void installAllDays() {
        m_daySchedule.subscribe(m_hostSchedule);
        m_hasWeDays = false;
    }

    private void installWEDays(String key) {
        addDays(createWeekendSchedule(key));
        setWorkDays();
        m_hasWeDays = true;
    }

    private void clearAllDays() {
        m_daySchedule.unsubscribe(m_hostSchedule);
    }

    private void clearWEDays() {
        m_hostSchedule.removeListeners();
        m_daySchedule.unsubscribeAll();
    }

    private void addDays(StartStopListenerSet days) {
        m_hostSchedule.addListener(days);
        m_daySchedule.subscribe(days);
    }

    private void setWorkDays() {
        WorkDaysSchedule wdays = m_parent.getWdSchedule();
        if (wdays == null) return;
        m_daySchedule.subscribe(wdays.getSchedule());
    }

    private ScheduleLevel createWeekendSchedule(String key) {
        WeekTimeTable wf = new WeekTimeTable(m_parent.getLogger());
        CalendarSetter stopSetter = new CalendarSetter();
        stopSetter.addFieldValue(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        wf.setStop(stopSetter);
        return new ScheduleLevel(key + " weekend filter", m_parent.getLogger(), wf, false, m_parent.getTimer());
    }

    private void clearPartHours() {
        m_daySchedule.removeListeners();
        m_hourSchedule.unsubscribeAll();
    }

    private void installPartHours() {
        m_hasPartHours = true;
    }

    private void clearAllHours() {
        m_hourSchedule.unsubscribe(m_daySchedule);
    }

    private void installAllHours() {
        m_hourSchedule.subscribe(m_daySchedule);
        m_hasPartHours = false;
    }

    public void setAllHours() {
        if (!m_hasPartHours) return;
        clearPartHours();
        installAllHours();
    }

    public void addHours(StartStopListenerSet hours) {
        if (!m_hasPartHours) {
            clearAllHours();
            installPartHours();
        }
        m_daySchedule.addListener(hours);
        m_hourSchedule.subscribe(hours);
    }

    public void dispose() {
        if (m_hasWeDays) {
            clearWEDays();
        } else {
            clearAllDays();
        }

        if (m_hasPartHours) {
            clearPartHours();
        } else {
            clearAllHours();
        }
    }

    public StartStopListener getTopSchedule() {
        return m_hostSchedule;
    }

    public StartStopSet getBottomSchedule() {
        return m_hourSchedule;
    }

    public void simpleRelay() {
        setAllDays();
        setAllHours();
    }
}
