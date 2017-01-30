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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CyclicTimeTable implements TimeTable {
    private Logger m_log;
    private long m_unit;
    private long m_cycleInterval;
    private long m_start;
    private long m_stop;

    protected void info(final String meth, final String msg, final Object[] pars) {
        if (m_log != null) {
            m_log.logp(Level.FINE, getClass().getName(), meth, msg, pars);
        }
    }

    public CyclicTimeTable(
        final Logger logger, long unit, long cycleInterval, long start, long stop
    ) {
        m_log = logger;
        m_unit = unit;
        m_cycleInterval = cycleInterval;
        m_start = start;
        m_stop = stop;
    }

    public Date getAmbientStart(final Date dt) {
        final long millisec = dt.getTime();
        final long cycleMillisec = m_unit * m_cycleInterval;
        final long cycleStart = millisec - (millisec % cycleMillisec);
        info("stop", "initial = {0,time} cycleStart = {1,time}", new Object[] {dt, new Date(cycleStart)});
        return new Date(cycleStart + (m_unit * m_start));
    }

    public Date getNextStart(final Date start) {
        return new Date(start.getTime() + (m_unit * m_cycleInterval));
    }

    public Date getNextStop(final Date start) {
        return new Date(start.getTime() + (m_unit * (m_stop - m_start + 1)));
    }
}
