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

import java.util.logging.Logger;
import java.util.*;

/**
 * User: AStepochkin
 * Date: 17.03.2009
 * Time: 16:11:27
 */
public abstract class StartStopCollector extends StartStopContainerBase {
    protected Map<StartStopSet, StartStopHolder> m_parents = new HashMap<StartStopSet, StartStopHolder>();
    protected int m_activeCount = 0;
    //private boolean m_started = true;

    public StartStopCollector(String desc, Logger logger) {
        super(desc, logger);
    }

    protected abstract boolean needToStart();

    private synchronized void incCheckStarted() {
        m_activeCount++;
        if (needToStart()) {
            startListeners(new Date());
        }
    }

    private synchronized void checkStopped() {
        if (!needToStart()) {
            stopListeners();
        }
    }

    private synchronized void decCheckStopped() {
        m_activeCount--;
        checkStopped();
    }

    public synchronized void subscribe(StartStopSet parent) {
        if (m_parents.containsKey(parent)) return;
        StartStopHolder ssh = new StartStopHolder(parent);
        m_parents.put(parent, ssh);
        ssh.subscribe();
    }

    public synchronized void unsubscribe(StartStopSet parent) {
        StartStopHolder ssh = m_parents.remove(parent);
        if (ssh != null) {
            ssh.unsubscribe();
        }
    }

    public synchronized void unsubscribeAll() {
        Iterator<Map.Entry<StartStopSet, StartStopHolder>> it = m_parents.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<StartStopSet, StartStopHolder> e = it.next();
            e.getValue().unsubscribe();
            it.remove();
        }
    }

    /*
    public synchronized void clearParents(boolean stopParents) {
        Iterator<Map.Entry<StartStopSet, StartStopHolder>> it = m_parents.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<StartStopSet, StartStopHolder> e = it.next();
            if (m_started) {
                e.getValue().unsubscribe();
            }
            if (stopParents) {
                StartStopSet p = e.getKey();
                if (p instanceof StartStopCollector) {
                    ((StartStopCollector)p).clearParents(true);
                }
                p.stop();
            }
            it.remove();
        }
    }
    */

    public Set<StartStopSet> getParents() {
        return Collections.unmodifiableSet(m_parents.keySet());
    }

    /*
    public synchronized void start() {
        if (m_started) return;
        for (StartStopHolder ssh: m_parents.values()) {
            ssh.subscribe();
        }
        m_started = true;
    }

    public synchronized void stop() {
        if (!m_started) return;
        for (StartStopHolder ssh: m_parents.values()) {
            ssh.unsubscribe();
        }
        m_started = false;
    }
    */

    private class StartStopHolder implements StartStopListener {
        private StartStopSet m_parent;
        private boolean m_started;
        private boolean m_inited;

        public StartStopHolder(StartStopSet parent) {
            m_started = false;
            m_parent = parent;
            m_inited = false;
        }

        public synchronized void start(Date parentStart) {
            if (!m_started) {
                m_started = true;
                incCheckStarted();
            }
            m_inited = true;
        }

        public synchronized void stop() {
            if (!m_inited) {
                m_started = false;
                checkStopped();
            } else if (m_started) {
                m_started = false;
                decCheckStopped();
            }
            m_inited = true;
        }

        public synchronized void subscribe() {
            m_started = false;
            m_parent.addListener(this);
        }

        public synchronized void unsubscribe() {
            m_parent.removeListener(this);
        }
    }
}
