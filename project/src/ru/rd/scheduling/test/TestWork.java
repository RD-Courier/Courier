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
package ru.rd.scheduling.test;

import ru.rd.scheduling.GroupTask;
import ru.rd.scheduling.Work;

import java.util.Timer;

public class TestWork implements Work{
    private String m_desc;
    private long m_delay;
    private long m_period;
    private int m_launchQnt = 1;
    private int m_launchLimit = 0;
    private Launcher m_launcher = null;

    public TestWork(
        final String desc, final long delay, final long period, final int limit, final Launcher l
    ) {
        m_desc = desc;
        m_delay = delay;
        m_period = period;
        m_launchLimit = limit;
        m_launcher = l;
    }

    public String getDesc() {
        return m_desc;
    }

    public void register(final Timer timer, final GroupTask tt) {
        timer.scheduleAtFixedRate(tt, m_delay, m_period);
    }

    public void run(final GroupTask tt) {
        System.out.println(getDesc() + " : run " + m_launchQnt);
        m_launchQnt++;
        if ((tt != null) && (m_launchQnt > m_launchLimit)) {
            tt.cancel();
            System.out.println(
                tt.getGroup().getDesc() + " size = " + tt.getGroup().getSize()
            );
            if (tt.getGroup().getSize() < 2) {
                tt.getGroup().cancel();
                if (m_launcher != null) {
                    synchronized(m_launcher) {
                        m_launcher.notify();
                    }
                }
            }
        }
    }
}
