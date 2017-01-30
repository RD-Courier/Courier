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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.CalendarHelper;
import ru.rd.scheduling.leveled.TimeTables.*;
import ru.rd.scheduling.leveled.launchers.OneExactTimeLauncher;
import ru.rd.scheduling.leveled.launchers.OneTimeImmediateLauncher;
import ru.rd.scheduling.leveled.launchers.PeriodicLauncher;

import java.util.logging.Logger;
import java.util.*;

/**
 * User: AStepochkin
 * Date: 01.08.2005
 * Time: 13:00:02
 */
public abstract class ScheduleInitializer {
    private final String m_innerScheduleTagName;
    protected abstract Logger getLogger();
    protected abstract Timer getTimer();
    protected void onLevel(StartStopSet level, int subScheduleCount) {}
    protected abstract Runnable createWork(Node conf, StartStopSet parent);
    protected StartStopListener createInnerSchedule(Node conf) {
        return null;
    }

    public ScheduleInitializer(String innerScheduleTagName) {
        m_innerScheduleTagName = innerScheduleTagName;
    }

    public ScheduleInitializer() {
        this("inner-schedule");
    }

    public StartStopListenerSet init(Node e, String name) throws Exception {
        StartStopContainer ret = new StartStopContainer(
            name + " schedule root", getLogger());
        initScheduleLevel(e, ret);
        return ret;
    }

    public void initScheduleLevel(Node e, StartStopSet parent) {
        int innerScheduleCount = 0;
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element se = (Element)n;
                String nodeName = se.getNodeName();
                GenericTimeTable timeTable;
                StartStopListener launcher = null;
                if (nodeName.equals("time-table")) {
                    timeTable = initTimeTable(se);
                    StartStopListenerSet sss = new ScheduleLevel(
                        nodeName, getLogger(), timeTable, false, getTimer()
                    );
                    initScheduleLevel(se, sss);
                    launcher = sss;
                    innerScheduleCount++;
                } else {
                    if (nodeName.equals(m_innerScheduleTagName)) {
                        launcher = createInnerSchedule(se);
                    } else {
                        Runnable work = createWork(se, parent);
                        if (nodeName.equals("launch-once-exact")) {
                            launcher = new OneExactTimeLauncher(
                                getLogger(), initCalendarSetter(se), work, getTimer()
                            );
                            adjustOnceLauncher(work, parent);
                        } else if (nodeName.equals("launch-once")) {
                            launcher = new OneTimeImmediateLauncher(getLogger(), work);
                            adjustOnceLauncher(work, parent);
                        } else if (nodeName.equals("launch-periodically")) {
                            launcher = initPeriodicLauncher(se, work, parent);
                        }
                    }
                }
                if (launcher != null) parent.addListener(launcher);
            }
        }
        onLevel(parent, innerScheduleCount);
    }

    private GenericTimeTable initTimeTable(Node conf) {
        GenericTimeTable timeTable;
        String period = DomHelper.getNodeAttr(conf, "period");
        if (period.equals("month")) {
            timeTable = new MonthTimeTable(getLogger());
        } else if (period.equals("week")) {
            timeTable = new WeekTimeTable(getLogger());
        } else if (period.equals("day")) {
            timeTable = new DayTimeTable(getLogger());
        } else if (period.equals("hour")) {
            timeTable = new HourTimeTable(getLogger());
        } else if (period.equals("minute")) {
            timeTable = new MinutesTimeTable(getLogger());
        } else if (period.equals("second")) {
            timeTable = new SecondsTimeTable(getLogger());
        } else {
            throw new RuntimeException("Unknown time table period '" + period + "'");
        }
        Element e;
        e = DomHelper.getChild(conf, "start", false);
        if (e != null) {
            timeTable.setStart(initCalendarSetter(e));
        }
        e = DomHelper.getChild(conf, "stop", false);
        if (e != null) {
            timeTable.setStop(initCalendarSetter(e));
        }
        return timeTable;
    }

    private static Map<String, Integer> s_FieldNameToId = new HashMap<String, Integer>();
    static {
        s_FieldNameToId.put("month", Calendar.MONTH);
        s_FieldNameToId.put("month-day", Calendar.DAY_OF_MONTH);
        s_FieldNameToId.put("week-day", Calendar.DAY_OF_WEEK);
        s_FieldNameToId.put("hour", Calendar.HOUR_OF_DAY);
        s_FieldNameToId.put("minute", Calendar.MINUTE);
        s_FieldNameToId.put("second", Calendar.SECOND);
        s_FieldNameToId.put("millisecond", Calendar.MILLISECOND);
    }

    private CalendarSetter initCalendarSetter(Node e) {
        CalendarSetter ret = new CalendarSetter();

        for (int i = 0; i < e.getAttributes().getLength(); i++) {
            Node attr = e.getAttributes().item(i);
            if (s_FieldNameToId.containsKey(attr.getNodeName())) {
                ret.addFieldValue(
                    s_FieldNameToId.get(attr.getNodeName()),
                    Integer.parseInt(attr.getNodeValue())
                );
            } else if (attr.getNodeName().equals("week-day-name")) {
                ret.addFieldValue(
                    Calendar.DAY_OF_WEEK,
                    CalendarHelper.getWeekByName(attr.getNodeValue())
                );
            } else {
                throw new RuntimeException(
                    "Unknown date-time unit '" + attr.getNodeName() + "'"
                );
            }
        }

        return ret;
    }

    private static Map<String, Integer> s_intervalFieldToId =
        new HashMap<String, Integer>();
    static {
        s_intervalFieldToId.put("months", Calendar.MONTH);
        s_intervalFieldToId.put("days", Calendar.DAY_OF_MONTH);
        s_intervalFieldToId.put("hours", Calendar.HOUR_OF_DAY);
        s_intervalFieldToId.put("minutes", Calendar.MINUTE);
        s_intervalFieldToId.put("seconds", Calendar.SECOND);
        s_intervalFieldToId.put("milliseconds", Calendar.MILLISECOND);
    }

    public static CalendarInterval initCalendarInterval(Node e) {
        CalendarInterval ret = new CalendarInterval();

        for (int i = 0; i < e.getAttributes().getLength(); i++) {
            Node attr = e.getAttributes().item(i);
            if (s_intervalFieldToId.containsKey(attr.getNodeName())) {
                ret.addFieldInterval(
                    s_intervalFieldToId.get(attr.getNodeName()),
                    Integer.parseInt(attr.getNodeValue())
                );
            } else {
                throw new RuntimeException(
                    "Unknown date-time interval unit '" + attr.getNodeName() + "'"
                );
            }
        }

        return ret;
    }

    protected void adjustOnceLauncher(Runnable work, StartStopSet parent) {}
    protected void adjustPeriodicLauncher(Runnable work, PeriodicLauncher launcher, StartStopSet parent) {}

    private StartStopListener initPeriodicLauncher(Node e, Runnable work, StartStopSet parent) {
        PeriodicLauncher launcher = new PeriodicLauncher(
            getLogger(), extractTimeFromConf(e), work, getTimer()
        );
        adjustPeriodicLauncher(work, launcher, parent);
        return launcher;
    }

    public static long extractTimeFromConf(Node conf) {
        CalendarInterval ci = initCalendarInterval(conf);
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(0);
        ci.shiftCalendar(c);
        return c.getTimeInMillis();
    }
}
