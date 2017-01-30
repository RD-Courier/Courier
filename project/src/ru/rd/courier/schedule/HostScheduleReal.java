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

import ru.rd.scheduling.leveled.*;
import ru.rd.utils.SysUtils;

import java.util.logging.Logger;
import java.util.Date;

/**
 * User: AStepochkin
 * Date: 19.03.2009
 * Time: 12:59:18
 */
public class HostScheduleReal implements HostSchedule {
    private final Logger m_logger;
    private final FileHostSchedules m_parent;
    private final StartStopUpdateProxy m_proxy;
    private final StartStopListenerSet m_enable;
    private StartStopPlugin m_engine;


    public HostScheduleReal(String name, Logger logger, FileHostSchedules parent) {
        m_logger = logger;
        m_proxy = new StartStopUpdateProxy(name + " schedule proxy", logger);
        m_enable = new StartStopContainer(name + " enable schedule", logger);
        m_parent = parent;
        installEngine(null, true);
    }

    public Logger getLogger() {
        return m_logger;
    }

    public void setDisabled() {
        setEngine(null, false);
    }

    private void setEngine(StartStopPlugin engine, boolean enabled) {
        beginUpdate();
        try {
            clearEngine();
            installEngine(engine, enabled);
        } finally {
            endUpdate();
        }
    }

    public void setEngine(StartStopPlugin engine) {
        setEngine(engine, true);
    }

    private void clearEngine() {
        if (m_engine == null) {
            m_parent.getParent().removeListener(m_proxy);
        } else {
            m_parent.getParent().removeListener(m_engine.getTopSchedule());
            SysUtils.dispose(m_engine, getLogger());
            m_engine = null;
            m_proxy.setParent(null);
        }
    }

    private void installEngine(StartStopPlugin engine, boolean enabled) {
        m_engine = engine;
        if (m_engine == null) {
            m_parent.getParent().addListener(m_proxy);
        } else {
            m_parent.getParent().addListener(m_engine.getTopSchedule());
            m_proxy.setParent(m_engine.getBottomSchedule());
        }
        if (enabled) {
            m_enable.start(new Date());
        } else {
            m_enable.stop();
        }
    }

    public void simpleRelay() {
        setEngine(null);
    }

    public void dispose() {
        clearEngine();
        SysUtils.dispose(m_engine, getLogger());
    }

    public StartStopListenerSet getMainSchedule() {
        return m_proxy;
    }

    public StartStopListenerSet getEnableSchedule() {
        return m_enable;
    }

    private void beginUpdate() {
        m_proxy.beginUpdate();
    }

    private void endUpdate() {
        m_proxy.endUpdate();
    }

    public boolean isEmpty() {
        return getMainSchedule().isEmpty() && getEnableSchedule().isEmpty();
    }
}
