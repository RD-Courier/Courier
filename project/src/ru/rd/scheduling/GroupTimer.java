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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public final class GroupTimer {
    private Object m_id;
    private final String m_desc;
    private Map m_tasks = new HashMap();
    private GroupTask m_activeTask;
    private Timer m_timer;
    private int m_nextTaskId = 0;

    public GroupTimer(final String desc) {
        m_desc = desc;
    }

    /**
     * No need to synchronize
     * because is intended to execute inside synchronized methods
     */
    private Timer getTimer() {
        if(m_timer == null) m_timer = new Timer();
        return m_timer;
    }

    /**
     * No need to synchronize
     * because is intended to execute inside synchronized methods
     */
    private Object getNewTaskId() {
        return new Integer(m_nextTaskId++);
    }

    /**
     * No need to synchronize
     * because is intended to execute inside synchronized methods
     */
    void setTaskId(final GroupTask t) {
        t.setId(getNewTaskId());
    }

    /**
     * No need to synchronize
     * because is intended to execute inside synchronized methods
     */
    void registerTask(final GroupTask t) {
        t.register(getTimer());
    }

    synchronized void removeTaskFromMap(final Object id) {
        m_tasks.remove(id);
        if((m_tasks.size() == 0) && (m_timer != null)) {
            m_timer.cancel();
            m_timer = null;
        }
    }

    synchronized void onBeginTask(final GroupTask t) {
        m_activeTask = t;
    }

    synchronized void onEndTask(final GroupTask t) {
        m_activeTask = null;
    }

    public Object getId() {
        return m_id;
    }

    void setId(final Object id) {
        m_id = id;
    }

    public String getDesc() {
        return m_desc;
    }

    synchronized public GroupTask getTask(final String id) {
        return (GroupTask)m_tasks.get(id);
    }

    synchronized public Set getTaskIds() {
        return m_tasks.keySet();
    }

    synchronized public int getSize() {
        return m_tasks.size();
    }

    synchronized public GroupTask getActiveTask() {
        return m_activeTask;
    }

    synchronized public Object addTask(final Work w) {
        if(w != null) {
            final GroupTask t = new GroupTask(w);
            t.setGroup(this);
            m_tasks.put(t.getId(), t);
            return t.getId();
        } else {
            return null;
        }
    }

    synchronized public void removeTask(final Object id) {
        if (m_tasks.containsKey(id)) {
            final GroupTask t = (GroupTask)m_tasks.get(id);
            t.clearGroup();
        }
    }

    synchronized public void cancel() {
        final GroupTask[] tt = new GroupTask[m_tasks.size()];
        m_tasks.values().toArray(tt);
        for (int i = 0; i < tt.length; i++) tt[i].clearGroup();
    }

    synchronized public String toString() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        try {
            final Iterator it = m_tasks.values().iterator();
            while(it.hasNext()) {
                final GroupTask t = (GroupTask)it.next();
                pw.println(t);
            }

            if(getActiveTask() != null) {
                pw.println("activeTask = " + getActiveTask().getId());
            }
        } finally {
            pw.close();
        }
        sw.flush();
        return sw.toString();
    }
}
