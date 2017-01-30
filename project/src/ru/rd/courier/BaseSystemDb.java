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
package ru.rd.courier;

import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public abstract class BaseSystemDb implements SystemDb {
    protected Application m_appl;
    protected CourierLogger m_logger;
    protected boolean m_started = false;
    protected int m_syncInterval;
    protected Timer m_timer;
    protected boolean m_cancelTimer;
    protected TimerTask m_syncTask;
    protected final DateFormat m_dateFormat;


    public BaseSystemDb(
        final Application appl, final CourierLogger msgh,
        final Node n, final Timer timer
    ) throws CourierException {
        m_dateFormat = appl.getDatabaseDateFormat();
        try {
            m_appl = appl;
            m_logger = msgh;
            if (timer == null) {
                m_timer = new Timer("SystemDbTimer");
                m_cancelTimer = true;
            } else {
                m_timer = timer;
                m_cancelTimer = false;
            }
            setSyncInterval(Integer.parseInt(DomHelper.getNodeAttr(n, "sync-interval")));
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    private synchronized void setSyncInterval(final int syncInterval) throws CourierException {
        m_syncInterval = syncInterval;
        if (m_syncTask != null) { stop(); start(); }
    }

    protected abstract TimerTask getTask();

    public synchronized void start() throws CourierException {
        if (m_started) {
            throw new CourierException("System DB already started");
        }

        if (m_timer == null) {
            throw new CourierException("Cannot start while timer is null");
        }
        m_syncTask = getTask();
        m_timer.schedule(m_syncTask, 0, m_syncInterval * 1000);
    }

    public synchronized void stop() throws CourierException {
        if (m_cancelTimer) {
            m_timer.cancel();
        } else if (m_syncTask != null) {
            m_syncTask.cancel();
        }
        m_syncTask = null;
        m_timer = null;
    }
}
