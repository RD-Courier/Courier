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

import ru.rd.courier.utils.FileLinesDetector;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.scheduling.leveled.*;
import ru.rd.scheduling.leveled.TimeTables.DayTimeTable;

import java.util.*;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 19.03.2009
 * Time: 12:55:48
 */
public class HostSchedulesLineParser extends FileLinesDetector.LineParserImpl {
    private final FileHostSchedules m_schedules;
    private final Set<String> m_updatedSet = new HashSet<String>();
    private final int m_changeCount;

    public HostSchedulesLineParser(FileHostSchedules schedules, StringSimpleParser p, int changeCount) {
        super(p);
        m_schedules = schedules;
        m_changeCount = changeCount;
    }

    protected Logger getLogger() {
        return m_schedules.getLogger();
    }

    public final Timer getTimer() {
        return m_schedules.getTimer();
    }

    public final void parse() throws Exception {
        if (p.thisChar('#')) return;
        boolean disabled = false;
        if (p.thisCharThenMove('*')) {
            disabled = true;
        }
        String key = p.shiftWord().toUpperCase();
        String schkey = key + "#" + m_changeCount;
        HostScheduleReal hs = m_schedules.getHostScheduleReal(key);
        if (disabled) {
            hs.setDisabled();
        } else {
            HostScheduleEngine hse = new HostScheduleEngine(schkey, hs.getLogger(), m_schedules);
            hse.setAllHours();
            parseSchedule(schkey, hse);
            p.skipBlanks();
            Properties props = new Properties();
            if (!p.beyondEnd()) {
                p.getProperties(props, '\'', null);
            }
            boolean withWeekend = StringHelper.boolParam(props, "weekend", false);
            if (withWeekend) {
                hse.setAllDays();
            } else {
                hse.setWEDays(schkey);
            }
            hs.setEngine(hse);
        }
        m_updatedSet.add(key);
    }

    protected final StartStopMerger parseSchedule(String key, HostScheduleEngine hs) {
        StartStopMerger ret = new StartStopMerger(key + " schedule", getLogger());
        if (p.thisCharThenMove(';') || p.beyondEnd()) return ret;
        int index = 0;
        while (true) {
            p.skipBlanks();
            ScheduleLevel sl = parseDayTimeTable(key + " schedule-" + index);
            ret.subscribe(sl);
            hs.addHours(sl);
            p.skipBlanks();
            if (p.thisCharThenMove(';') || p.beyondEnd()) break;
            p.ensureChar(',');
            index++;
        }
        return ret;
    }

    protected ScheduleLevel parseDayTimeTable(String desc) {
        DayTimeTable tt = new DayTimeTable(getLogger());
        ScheduleLevel sl = new ScheduleLevel(desc, getLogger(), tt, false, getTimer());
        if (!p.thisChar('-')) {
            tt.setStart(parseSetter());
            p.skipBlanks();
        }
        if (p.thisChar(',') || p.thisChar(';') || p.beyondEnd()) return sl;
        p.ensureChar('-');
        if (p.thisChar(',') || p.thisChar(';') || p.beyondEnd()) return sl;
        p.skipBlanks();
        tt.setStop(parseSetter());
        return sl;
    }

    private CalendarSetter parseSetter() {
        CalendarSetter setter = new CalendarSetter();
        setter.addFieldValue(Calendar.HOUR_OF_DAY, p.shiftInt());
        p.ensureChar(':');
        setter.addFieldValue(Calendar.MINUTE, p.shiftInt());
        return setter;
    }

    public void startParse() {
        m_schedules.cleanup();
    }

    public void finishParse() {
        m_schedules.forceStart(m_updatedSet);
    }
}
