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
package ru.rd.scheduling.leveled.launchers;

import ru.rd.scheduling.leveled.StartStopListener;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class PeriodicLauncher implements StartStopListener {
    private Logger m_logger;
    private long m_interval;
    private Timer m_timer;
    private Runnable m_work = null;
    private TimerTask m_curTask = null;

    public PeriodicLauncher(Logger logger, long interval, Runnable work, Timer timer) {
        m_logger = logger;
        m_interval = interval;
        m_work = work;
        m_timer = (timer == null) ? new Timer(): timer;
    }

    public PeriodicLauncher(Logger logger, final long interval, final Runnable work) {
        this(logger, interval, work, null);
    }

    synchronized public void start(final Date parentStart) {
        m_curTask = new TimerTask() {
            public void run() {
                /*
                if (m_logger != null) {
                    m_logger.log(
                        Level.FINE,
                        "PeriodicLauncher: interval {0}: launching work: {1}",
                        new Object[] {m_interval, m_work}
                    );
                }
                */
                m_work.run();
            }
        };
        m_timer.schedule(m_curTask, 0, m_interval);
    }

    synchronized public void stop() {
        if (m_curTask != null) m_curTask.cancel();
    }

    synchronized public void setInterval(final long interval) {
        m_interval = interval;
        if (m_curTask == null) {
            stop();
            start(null);
        }
    }

    synchronized public long getInterval() {
        return m_interval;
    }
}
