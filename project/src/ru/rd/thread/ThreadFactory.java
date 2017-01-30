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
package ru.rd.thread;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.utils.NativeWrapper;

/**
 * User: AStepochkin
 * Date: 05.01.2005
 * Time: 12:15:37
 */
public class ThreadFactory implements PoolObjectFactory {
    private final CourierLogger m_logger;
    private final String m_desc;
    private final ThreadGroup m_group;
    private boolean m_initCOM;
    private int m_curId = 1;

    public ThreadFactory(CourierLogger logger, ThreadGroup group, String desc, boolean initCOM) {
        m_logger = logger;
        m_desc = desc;
        m_group = group;
        m_initCOM = initCOM;
    }

    public ThreadFactory(CourierLogger logger, String desc, boolean initCOM) {
        this(logger, null, desc, initCOM);
    }

    public ThreadFactory(CourierLogger logger, String desc) {
        this(logger, desc, false);
    }

    public Object getObject(ObjectPoolIntf pool) {
        final WorkThread wt = new WorkThread(m_logger, m_group, m_desc + "-" + m_curId, false);
        if (m_initCOM) wt.launchWorkAndWait(new Runnable(){
            public void run() {
                NativeWrapper.coInitialize();
            }
        });
        m_curId++;
        return wt;
    }

    public void returnObject(final Object o) {
        WorkThread wt = (WorkThread)o;
        if (m_initCOM && !wt.isBusy()) {
            wt.launchWorkAndWait(
                new Runnable(){
                    public void run() {
                        NativeWrapper.coUninitialize();
                    }
                    public String toString() {
                        return "Thread coUninitializer";
                    }
                }, 15*1000
            );
        }
        wt.closeOrDump(5);
    }

    public boolean checkObject(final Object o) {
        WorkThread wt = (WorkThread)o;
        return wt.isActive() && !wt.isInterrupted() && wt.isAlive() && !wt.isBusy();
    }

    public static void launchWork(final ObjectPoolIntf threadPool, Runnable work) {
        final WorkThread wt = (WorkThread)threadPool.getObject();
        wt.launchWork(work, new FreeListener() {
            public void free(Runnable w) {
                threadPool.releaseObject(wt);
            }
        });
    }
}
