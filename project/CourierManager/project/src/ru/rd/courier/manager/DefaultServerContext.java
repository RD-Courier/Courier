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
package ru.rd.courier.manager;

import ru.rd.net.ServerContext;
import ru.rd.utils.Disposable;
import ru.rd.utils.SysUtils;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.ConsoleCourierLogger;

import java.util.Timer;

/**
 * User: AStepochkin
 * Date: 16.10.2008
 * Time: 13:24:08
 */
class DefaultServerContext implements ServerContext, Disposable {
    private Timer m_timer;
    private LogProvider m_loggers;
    private ObjectPoolIntf m_threads;
    private boolean m_isDebugMode;

    public DefaultServerContext() {}

    public DefaultServerContext(
        Timer timer, LogProvider loggers,
        ObjectPoolIntf threads, boolean isDebugMode
    ) {
        this();
        m_timer = timer;
        m_loggers = loggers;
        m_threads = threads;
        m_isDebugMode = isDebugMode;
    }

    public Timer getTimer() {
        return m_timer;
    }

    public void setTimer(Timer timer) {
        m_timer = timer;
    }

    public CourierLogger getLogger(String name) {
        return m_loggers.getLogger(name);
    }

    public void setLogProvider(LogProvider loggers) {
        m_loggers = loggers;
    }

    public ObjectPoolIntf getThreadPool() {
        return m_threads;
    }

    public void setThreadPool(ObjectPoolIntf threads) {
        m_threads = threads;
    }

    public boolean isDebugMode() {
        return m_isDebugMode;
    }

    public void setDebugMode(boolean value) {
        m_isDebugMode = value;
    }

    public void dispose() {
        CourierLogger logger = getLogger(null);
        try { getTimer().cancel(); } catch(Throwable e) { logger.warning(e); }
        try { m_threads.close(); } catch(Throwable e) { logger.warning(e); }
        SysUtils.dispose(m_loggers, new ConsoleCourierLogger(""));
    }
}
