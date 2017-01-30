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

import java.util.Date;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 16.03.2009
 * Time: 9:38:34
 */
public class StartStopUpdateProxy extends StartStopContainerBase implements StartStopListenerSet {
    private StartStopSet m_parent;
    private int m_updateCount = 0;
    private boolean m_savedIsActive = false;
    private boolean m_isActive = false;
    private Date m_startDate = null;
    private Date m_savedStartDate = null;

    public StartStopUpdateProxy(String desc, Logger logger) {
        super(desc, logger);
    }

    public synchronized void beginUpdate() {
        m_updateCount++;
        if (m_updateCount == 1) {
            m_savedIsActive = m_isActive;
            m_savedStartDate = m_startDate;
        }
    }

    public synchronized void endUpdate() {
        if (m_updateCount <= 0) return;
        m_updateCount--;
        if (m_updateCount == 0 && m_savedIsActive != m_isActive) {
            if (m_isActive) {
                startListeners(m_savedStartDate);
            } else {
                stopListeners();
            }
        }
    }

    public synchronized StartStopSet getParent() {
        return m_parent;
    }

    public synchronized void setParent(StartStopSet parent) {
        if (m_parent == parent) return;
        beginUpdate();
        try {
            if (m_parent == null) {
                if (m_isActive) stop();
            } else {
                m_parent.removeListener(this);
            }
            m_parent = parent;
            if (m_parent != null) {
                m_parent.addListener(this);
            }
        } finally {
            endUpdate();
        }
    }

    public synchronized void start(Date parentStart) {
        m_isActive = true;
        m_startDate = parentStart;
        if (m_updateCount <= 0) {
            startListeners(parentStart);
        }
    }

    public synchronized void stop() {
        m_isActive = false;
        if (m_updateCount <= 0) {
            stopListeners();
        }
    }
}
