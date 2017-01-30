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
import ru.rd.scheduling.leveled.StartStopContainer;
import ru.rd.scheduling.leveled.StartStopListenerSet;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 13.03.2009
 * Time: 10:45:45
 */
public class FileHostSchedules extends FileLinesDetector implements HostSchedules {
    protected Map<String, HostScheduleReal> m_schedules = new HashMap<String, HostScheduleReal>();
    private final StartStopListenerSet m_parent;
    private final WorkDaysSchedule m_wdSchedule;

    public FileHostSchedules(Logger logger, File file, File wdFile, Timer timer) {
        super(logger, file, timer);
        if (wdFile == null) {
            m_wdSchedule = null;
        } else {
            m_wdSchedule = new WorkDaysSchedule(logger, wdFile, timer);
        }
        m_parent = new StartStopContainer("FileHostSchedules", logger);
    }

    public void start() {
        getDetector().start();
        if (m_wdSchedule != null) {
            m_wdSchedule.start();
        }
    }

    public void stop() {
        getDetector().stop();
        if (m_wdSchedule != null) {
            m_wdSchedule.stop();
        }
    }

    protected LineParser createParser(StringSimpleParser p) {
        return new HostSchedulesLineParser(this, p, m_changeCount);
    }

    public WorkDaysSchedule getWdSchedule() {
        return m_wdSchedule;
    }

    public StartStopListenerSet getParent() {
        return m_parent;
    }

    public HostScheduleReal getHostScheduleReal(String name) {
        HostScheduleReal hs = m_schedules.get(name);
        if (hs == null) {
            hs = new HostScheduleReal(name, getLogger(), this);
            m_schedules.put(name, hs);
        }
        return hs;
    }

    public HostSchedule getHostSchedule(String name) {
        return getHostScheduleReal(name);
    }

    public void cleanup() {
        Iterator<Map.Entry<String, HostScheduleReal>> it = m_schedules.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, HostScheduleReal> e = it.next();
            HostScheduleReal hs = e.getValue();
            if (hs.isEmpty()) {
                hs.dispose();
                it.remove();
            }
        }
    }

    public void forceStart(Set<String> excludeNames) {
        for (Map.Entry<String, HostScheduleReal> e : m_schedules.entrySet()) {
            try {
                if (!excludeNames.contains(e.getKey())) {
                    e.getValue().simpleRelay();
                }
            } catch (Exception err) {
                getLogger().log(Level.WARNING, err.getMessage(), err);
            }
        }
    }
}
