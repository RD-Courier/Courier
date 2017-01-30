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
package ru.rd.scheduling;

import java.util.Timer;
import java.util.TimerTask;

public final class GroupTask extends TimerTask {
    private Object m_id = null;
    private GroupTimer m_group = null;
    private Work m_work = null;
    private boolean m_isRunning = false;
    private boolean m_delayedClear = false;

    public GroupTask(final Work w) {
        m_work = w;
    }

    public Object getId() {
        return m_id;
    }

    void setId(final Object id) {
        m_id = id;
    }

    private void absClearGroup() {
        if (m_group != null) {
            final GroupTimer tg = m_group;
            m_group = null;
            tg.removeTaskFromMap(getId());
        }
        setId(null);
    }

    public boolean clearGroup() {
        boolean ret = false;
        if (m_group != null) {
            ret = super.cancel();
            if (m_isRunning) {
                m_delayedClear = true;
            } else {
                absClearGroup();
            }
        }
        return ret;
    }

    public void setGroup(final GroupTimer g) {
        clearGroup();

        if (g != null) {
            m_group = g;
            g.setTaskId(this);
            g.registerTask(this);
        }
    }

    public boolean cancel() {
        return clearGroup();
    }

    public GroupTimer getGroup() {
        return m_group;
    }

    public Work getWork() {
        return m_work;
    }

    public void run() {
        m_isRunning = true;
        m_group.onBeginTask(this);
        m_work.run(this);
        m_group.onEndTask(this);
        m_isRunning = false;
        if (m_delayedClear) {
            absClearGroup();
            m_delayedClear = false;
        }
    }

    public String toString() {
        return "task-id: " + getId() + " work: " + m_work.getDesc();
    }

    protected void register(final Timer timer) {
        m_work.register(timer, this);
    }
}
