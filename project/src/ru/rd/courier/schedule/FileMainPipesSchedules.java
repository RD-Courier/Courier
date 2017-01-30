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

import ru.rd.courier.utils.FileStringDetector;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.scheduling.leveled.StartStopContainer;
import ru.rd.scheduling.leveled.StartStopSet;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 02.04.2009
 * Time: 16:41:56
 */
public class FileMainPipesSchedules extends FileStringDetector implements MainPipesSchedules {
    private final String m_courierCode;
    private final Map<String, StartStopContainer> m_schedules = new HashMap<String, StartStopContainer>();
    private Date m_startDate;

    public FileMainPipesSchedules(Logger logger, File file, Timer timer, String courierCode) {
        super(logger, file, timer);
        m_courierCode = courierCode;
        m_startDate = null;
    }

    private synchronized void setPriorityPipes(Set<String> pipes) {
        if (m_startDate == null) return;

        for (Map.Entry<String, StartStopContainer> e: m_schedules.entrySet()) {
            StartStopContainer sch = getSch(e.getKey());
            if (pipes.remove(e.getKey())) {
                if (!sch.isActive()) sch.start(m_startDate);
            } else {
                if (sch.isActive()) sch.stop();
            }
        }

        for (String pipe: pipes) {
            StartStopContainer sch = getSch(pipe);
            sch.start(m_startDate);
        }
    }

    private synchronized StartStopContainer getSch(String pipe) {
        StartStopContainer ret = m_schedules.get(pipe);
        if (ret == null) {
            ret = new StartStopContainer(pipe + " priority schedule", getLogger());
            m_schedules.put(pipe, ret);
        }
        return ret;
    }

    public StartStopSet getSchedule(String pipe) {
        return getSch(pipe);
    }

    public synchronized void start() {
        m_startDate = new Date();
        super.start();
    }

    public synchronized void stop() {
        m_startDate = null;
        super.stop();
    }

    protected void parse(StringSimpleParser p) {
        while (true) {
            p.skipBlanks();
            if (p.beyondEnd()) break;
            String courierCode = p.shiftWordOrBracketedString('\'');
            p.skipBlanks();
            p.ensureChar('(');
            if (m_courierCode.equalsIgnoreCase(courierCode)) {
                setPriorityPipes(new HashSet<String>(p.parseDelimitedList('\'', ',', ')')));
                break;
            } else {
                p.skipDelimitedList('\'', ',', ')');
            }
        }
    }
}
