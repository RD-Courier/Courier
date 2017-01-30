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
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.scheduling.leveled.*;
import ru.rd.scheduling.leveled.TimeTables.DayInfo;
import ru.rd.scheduling.leveled.TimeTables.DayListTimeTable;
import ru.rd.scheduling.leveled.TimeTables.NeverTimeTable;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.Date;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 20.03.2009
 * Time: 14:28:59
 */
public class WorkDaysSchedule extends FileLinesDetector {
    private final StartStopUpdateProxy m_proxy;
    private final ScheduleLevel m_schedule;

    public WorkDaysSchedule(Logger logger, File file, Timer timer) {
        super(logger, file, timer);
        m_proxy = new StartStopUpdateProxy("WorkDaysScheduleProxy", logger);
        m_schedule = new ScheduleLevel("WorkDaysSchedule", logger, new NeverTimeTable(), false, timer);
        m_proxy.setParent(m_schedule);
    }

    public synchronized StartStopListenerSet getSchedule() {
        return m_proxy;
    }

    public synchronized void start() {
        getDetector().start();
        m_schedule.start(new Date());
    }

    public synchronized void stop() {
        getDetector().stop();
        m_schedule.stop();
    }

    private class Parser extends LineParserImpl {
        private final DayListTimeTable tt;

        public Parser(StringSimpleParser p) {
            super(p);
            tt = new DayListTimeTable();
        }

        public void parse() throws Exception {
            List<DayInfo> days = parseDays();
            for (DayInfo di: days) {
                tt.addDay(di);
            }
            m_proxy.beginUpdate();
            try {
                m_schedule.setTimetable(tt);
            } finally {
                m_proxy.endUpdate();
            }
        }

        private List<DayInfo> parseDays() {
            List<DayInfo> ret = new LinkedList<DayInfo>();
            p.skipBlanks();
            if (p.beyondEnd()) return ret;
            while (true) {
                DayInfo di = new DayInfo();
                di.year = p.shiftInt();
                p.ensureChar('-');
                di.month = p.shiftInt() - 1;
                p.ensureChar('-');
                di.day = p.shiftInt();
                ret.add(di);
                p.skipBlanks();
                if (p.beyondEnd()) break;
                p.ensureChar(',');
                p.skipBlanks();
            }
            return ret;
        }
    }

    protected final LineParser createParser(StringSimpleParser p) {
        return new Parser(p);
    }
}
