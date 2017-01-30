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

import ru.rd.scheduling.leveled.StartStopListener;
import ru.rd.scheduling.leveled.StartStopSet;
import ru.rd.courier.schedule.RelaunchLimit;
import ru.rd.courier.LocalSystemDb;

import java.util.Date;

/**
 * User: AStepochkin
 * Date: 07.06.2008
 * Time: 10:35:21
 */
public class ScheduleLimit implements RelaunchLimit {
    private final StartStopSet m_scheduleParent;
    private StartStopListener m_ssListener;
    private boolean m_inWork = false;
    private boolean m_expired = false;

    public ScheduleLimit(StartStopSet scheduleParent) {
        m_scheduleParent = scheduleParent;
        m_ssListener = new StartStopListener() {
            public void start(Date parentStart) {
                m_inWork = true;
            }
            public void stop() {
                scheduleStopped();
            }
        };
        scheduleParent.addListener(m_ssListener);
    }

    private synchronized void scheduleStopped() {
        if (m_inWork) m_expired = true;
    }

    public boolean canBeRelaunched(LocalSystemDb.ProcessInfo pinfo) {
        return !m_expired;
    }

    public String getRejectReason(LocalSystemDb.ProcessInfo pinfo) {
        return (m_expired) ? "Schedule expired" : "";
    }

    public void finish() {
        synchronized(this) {
            m_inWork = false;
        }
        m_scheduleParent.removeListener(m_ssListener);
    }
}
