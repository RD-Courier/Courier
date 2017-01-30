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
package ru.rd.pool2;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.thread.WorkThread;
import ru.rd.utils.SafeLoggingRunnable;

/**
 * User: Astepochkin
 * Date: 30.09.2008
 * Time: 15:12:19
 */
public class AsynchExecutePolicy implements ExecutePolicy {
    private final CourierLogger m_logger;
    private final ObjectPoolIntf m_threadPool;
    private long m_conTimeout;
    private long m_checkTimeout;

    public AsynchExecutePolicy(CourierLogger logger, ObjectPoolIntf threadPool) {
        m_logger = logger;
        m_threadPool = threadPool;
        m_conTimeout = 0;
        m_checkTimeout = 0;
    }

    public void setAllocateTimeout(long value) {
        m_conTimeout = value;
    }

    public void setCheckTimeout(long value) {
        m_checkTimeout = value;
    }

    public Object createThreadContext() {
        return m_threadPool.getObject();
    }

    public void closeThreadContext(Object threadContext) {
        m_threadPool.releaseObject(threadContext);
    }

    private void execWork(Object bulkContext, Runnable work, long timeout) {
        WorkThread wt;
        if (bulkContext == null) {
            wt = (WorkThread)m_threadPool.getObject();
        } else {
            wt = (WorkThread)bulkContext;
        }
        try {
            wt.launchWorkAndWait(work, timeout);
        } finally {
            if (bulkContext == null) m_threadPool.releaseObject(wt);
        }
    }

    public Object allocateObject(
        final ObjectPoolIntf pool, final Object bulkContext, final PoolObjectFactory factory
    ) throws Exception {

        class ConnectWork extends SafeLoggingRunnable {
            private Object m_object = null;

            public ConnectWork() {
                super(m_logger);
            }

            protected void safeRun() throws Throwable {
                m_object = factory.getObject(pool);
            }

            public Object getObject() {
                return m_object;
            }
        }

        ConnectWork w = new ConnectWork();
        execWork(bulkContext, w, m_conTimeout);
        return w.getObject();
    }

    public void deallocateObject(Object bulkContext, final Object o, final PoolObjectFactory factory) {
        execWork(bulkContext, new SafeLoggingRunnable(m_logger) {
            protected void safeRun() throws Throwable {
                factory.returnObject(o);
            }
        }, m_conTimeout);
    }

    public boolean objectWrapperValid(Object bulkContext, final Object o, final PoolObjectFactory factory) {
        class CheckWork extends SafeLoggingRunnable {
            private boolean m_valid = false;

            public CheckWork() {
                super(m_logger);
            }

            protected void safeRun() throws Throwable {
                m_valid = factory.checkObject(o);
            }

            public boolean isValid() {
                return m_valid;
            }
        }

        CheckWork w = new CheckWork();
        execWork(bulkContext, w, m_checkTimeout);
        return w.isValid();
    }
}
