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
package ru.rd.scheduling.leveled.test;

import ru.rd.scheduling.leveled.*;
import ru.rd.scheduling.leveled.TimeTables.GenericTimeTable;
import ru.rd.scheduling.leveled.TimeTables.HourTimeTable;
import ru.rd.scheduling.leveled.TimeTables.MinutesTimeTable;
import ru.rd.scheduling.leveled.TimeTables.WeekTimeTable;
import ru.rd.scheduling.leveled.launchers.HourPeriodicLauncher;
import ru.rd.scheduling.leveled.launchers.PeriodicLauncher;
import ru.rd.courier.utils.DomHelper;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class Launcher {
    private static void out(String mes) {
        System.out.println((new Date()) + " - " + mes);
    }

    private static class MessagesStartStopListener implements StartStopListener {
        private final String m_desc;

        public MessagesStartStopListener(final String desc) {
            m_desc = desc;
        }

        private void out(String mes) {
            System.out.println((new Date()) + " - " + m_desc + " - " + mes);
        }

        public void start(final Date parentStart) { out("start"); }
        public void stop() { out("stop"); }
    }

    private static class PulseStartStopListener implements StartStopListener {
        private final String m_desc;
        private long m_interval;
        private Timer m_timer;
        private TimerTask m_curTask = null;

        public PulseStartStopListener(final String desc, final long interval) {
            m_desc = desc;
            m_interval = interval;
            m_timer = new Timer();
        }

        private void out(String mes) {
            System.out.println((new Date()) + " - " + m_desc + " - " + mes);
        }

        public void start(final Date parentStart) {
            out("start");
            m_curTask = new TimerTask() {
                public void run() { out("impulse"); }
            };
            m_timer.schedule(m_curTask, 0, m_interval);
        }

        public void stop() {
            if (m_curTask != null) m_curTask.cancel();
            out("stop");
        }
    }

    public static void main(final String[] args) throws Exception, ParserConfigurationException, SAXException, NoSuchMethodException {
        Document doc = DomHelper.parseString(
            "<root><a1 a1-attr1=\"attr1.1-val\"> <![CDATA[a1-begin]]> <a2><a4></a4> a2 </a2><a3 a3-attr1=\"attr1.3-val\"> a3 </a3> a1-end </a1></root>"
        );
        out(DomHelper.nodeToEscString(doc.getDocumentElement()));
        System.exit(0);

        Calendar c = new GregorianCalendar();
        c.setTime(new Date());
        out((new SimpleDateFormat("dd.MM.yyyy E HH:mm:ss SSS")).format(c.getTime()));
        c.set(Calendar.DAY_OF_MONTH, 20);
        out((new SimpleDateFormat("dd.MM.yyyy E HH:mm:ss SSS")).format(c.getTime()));
        out("DAY_OF_WEEK = " + c.get(Calendar.DAY_OF_WEEK));
        out("FirstDayOfWeek = " + c.getFirstDayOfWeek());
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        out("DAY_OF_WEEK = " + c.get(Calendar.DAY_OF_WEEK));
        out((new SimpleDateFormat("dd.MM.yyyy E HH:mm:ss SSS")).format(c.getTime()));

        final Timer timer = new Timer();
        final Logger log = Logger.getAnonymousLogger();
        log.setLevel(Level.FINEST);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        log.addHandler(consoleHandler);
        StartStopListenerSet root;
        /*
        root = new ScheduleLevel(
            "half-minute", log,
            new CyclicTimeTable(log, 30*1000, 4, 1, 2), false,
            timer
        );
        final ScheduleLevel ch1_1 = new ScheduleLevel(
            "10-sec", log,
            new CyclicTimeTable(log, 5*1000, 5, 1, 2), false,
            timer
        );
        final StartStopListener ch1_m = new MessagesStartStopListener("ch1_m");
        final StartStopListener ch2_p = new PulseStartStopListener("ch2_p", 1000);
        final StartStopListener ch2_m = new MessagesStartStopListener("ch2_m");

        root.addListener(ch1_1);
        root.addListener(ch1_m);
        ch1_1.addListener(ch2_p);
        ch1_1.addListener(ch2_m);
        */

        root = new StartStopContainer("root", log);
        CalendarSetter start, stop;
        GenericTimeTable tt;

        start = new CalendarSetter();
        start.addFieldValue(Calendar.DAY_OF_WEEK, 1);
        stop = new CalendarSetter();
        stop.addFieldValue(Calendar.DAY_OF_WEEK, 2);
        tt = new WeekTimeTable(log);
        tt.setStart(start);
        tt.setStop(stop);
        ScheduleLevel weekLevel = new ScheduleLevel(
            "week-days", log,
            tt,
            false,
            timer
        );
        root.addListener(weekLevel);
        weekLevel.addListener(new MessagesStartStopListener("week"));

        start = new CalendarSetter();
        start.addFieldValue(Calendar.HOUR_OF_DAY, 16);
        stop = new CalendarSetter();
        stop.addFieldValue(Calendar.HOUR_OF_DAY, 20);
        tt = new HourTimeTable(log);
        tt.setStart(start);
        tt.setStop(stop);
        ScheduleLevel hourLevel = new ScheduleLevel(
            "hours", log,
            tt,
            false,
            timer
        );
        hourLevel.addListener(new MessagesStartStopListener("hour"));
        weekLevel.addListener(hourLevel);

        final int min = 28;

        CalendarInterval interval = new CalendarInterval();
        interval.addFieldInterval(Calendar.MINUTE, min);
        interval.addFieldInterval(Calendar.SECOND, 16);
        hourLevel.addListener(new HourPeriodicLauncher(
            log, "minute launcher", 1,
            new Runnable() {
                public void run() {
                    System.out.println((new Date()) + " - minute launcher run");
                }
            },
            interval, timer
        ));

        start = new CalendarSetter();
        start.addFieldValue(Calendar.MINUTE, min);
        stop = new CalendarSetter();
        stop.addFieldValue(Calendar.MINUTE, min);
        tt = new MinutesTimeTable(log);
        tt.setStart(start);
        tt.setStop(stop);
        ScheduleLevel minuteLevel1 = new ScheduleLevel(
            "minute1", log,
            tt,
            false,
            timer
        );
        minuteLevel1.addListener(new MessagesStartStopListener("minute1"));
        hourLevel.addListener(minuteLevel1);
        minuteLevel1.addListener(
            new PeriodicLauncher(log, 500, new Runnable() {
                public void run() {
                    out("launch minute1");
                }
            }, timer)
        );

        /*
        ScheduleLevel minuteLevel2 = new ScheduleLevel(
            "minute2", log,
            new MinutesTimeTable(log, 25, 26),
            false,
            timer
        );
        minuteLevel2.addListener(new MessagesStartStopListener("minute2"));
        hourLevel.addListener(minuteLevel2);
        minuteLevel2.addListener(
            new PeriodicLauncher("", 500, new Runnable() {
                public void run() {
                    out("launch minute2");
                }
            }, timer)
        );
        */

        root.start(new Date());
        try {
            Thread.sleep(3 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        root.stop();
        System.exit(0);
    }
}
