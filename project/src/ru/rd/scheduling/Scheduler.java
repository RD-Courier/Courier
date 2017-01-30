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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Scheduler {
    private Map m_taskGroups = new HashMap();
    private int m_nextId = 0;

    private Object getGroupId() {
        return new Integer(m_nextId++);
    }

    public Object addTaskGroup(final String desc) {
        final GroupTimer tg = new GroupTimer(desc);
        final Object id = getGroupId();
        tg.setId(id);
        m_taskGroups.put(id, tg);
        return id;
    }

    public void removeTaskGroup(final Object groupId) {
        if(m_taskGroups.containsKey(groupId)) {
            final GroupTimer tg = getTaskGroup(groupId);
            tg.cancel();
            m_taskGroups.remove(groupId);
        }
    }

    public GroupTimer getTaskGroup(final Object groupId) {
        return (GroupTimer)m_taskGroups.get(groupId);
    }

    public void addWork(final Object groupId, final Work w) {
        if(m_taskGroups.containsKey(groupId)) {
            getTaskGroup(groupId).addTask(w);
        }
    }

    public String toString() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        try {
            final Iterator it = m_taskGroups.values().iterator();
            while(it.hasNext()) {
                final GroupTimer tg = (GroupTimer)it.next();
                pw.println("group id = " + tg.getId());
                pw.println(tg);
            }
        } finally {
            pw.close();
        }
        sw.flush();
        return sw.toString();
    }
}
