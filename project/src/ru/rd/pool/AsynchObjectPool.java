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
package ru.rd.pool;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.thread.WorkThread;
import ru.rd.utils.ExceptionCatchRunnable;

/**
 * User: AStepochkin
 * Date: 29.06.2005
 * Time: 10:04:45
 */
public class AsynchObjectPool extends ObjectPool {
    private final ObjectPoolIntf m_threadProvider;
    private long m_allocateObjectTimeout;
    private long m_checkTimeout;
    private WorkThread m_cachedThread;

    public AsynchObjectPool(
        String desc, CourierLogger logger,
        ObjectPoolIntf threadPool, PoolObjectFactory objFactory
    ) {
        super(desc, logger, objFactory);
        m_threadProvider = threadPool;
    }

    public AsynchObjectPool(
        String desc, CourierLogger logger,
        ObjectPool threadPool, PoolObjectFactory objFactory,
        int initialCapacity, int incCapacity, int maxCapacity, long getObjectTimeout,
        long shrinkInterval, int shrinkCapacity, long shrinkObjPeriod,
        long checkInterval, long checkTimeout,
        long expirePeriod
    ) {
        super(
            desc, logger, objFactory,
            initialCapacity, incCapacity, maxCapacity,
            shrinkInterval, shrinkCapacity, shrinkObjPeriod,
            checkInterval, expirePeriod
        );
        m_threadProvider = threadPool;
        m_allocateObjectTimeout = getObjectTimeout;
        m_checkTimeout = checkTimeout;
    }

    public AsynchObjectPool(
        String desc, CourierLogger logger,
        ObjectPool threadPool, PoolObjectFactory objFactory,
        int initialCapacity, int incCapacity, int maxCapacity, long getObjectTimeout,
        long shrinkInterval, int shrinkCapacity, long shrinkObjPeriod,
        long checkInterval, long checkTimeout
    ) {
        this(
            desc, logger, threadPool, objFactory,
            initialCapacity, incCapacity, maxCapacity, getObjectTimeout,
            shrinkInterval, shrinkCapacity, shrinkObjPeriod,
            checkInterval, checkTimeout,
            -1
        );
    }

    public final void setTimeouts(long allocateObjectTimeout, long checkTimeout) {
        m_allocateObjectTimeout = allocateObjectTimeout;
        m_checkTimeout = checkTimeout;
    }

    private abstract class ValueInnerWork<T> extends ExceptionCatchRunnable {
        private T m_value;

        public final T getValue() {
            return m_value;
        }

        protected void setValue(T value) {
            m_value = value;
        }
    }

    private void asynchOperation(ExceptionCatchRunnable work, long timeout) {
        WorkThread thread;
        boolean isLocalThread;
        if (m_cachedThread == null) {
            thread = (WorkThread) m_threadProvider.getObject();
            isLocalThread = true;
        } else {
            thread = m_cachedThread;
            isLocalThread = false;
        }
        try {
            boolean expired = !thread.launchWorkAndWait(work, timeout);
            if (work.getException() != null) {
                throw new RuntimeException(
                    "Asynch operation error. Exception is attached as cause",
                    work.getException());
            }
            if (expired) {
                String errMes = (
                    "Timeout (" + timeout + ") expired on thread " +
                    thread.toString()
                );
                thread.stopRequest();
                if (!isLocalThread) returnCachedThread();
                throw new RuntimeException(errMes);
            }
        } finally{
            if (isLocalThread) {
                try { m_threadProvider.releaseObject(thread); }
                catch (Exception e) { m_logger.error(e); }
            }
        }
    }

    protected void beforeActivities() {
        m_cachedThread = (WorkThread) m_threadProvider.getObject();
    }

    protected void afterActivities() {
        returnCachedThread();
    }

    private void returnCachedThread() {
        if (m_cachedThread == null) return;
        try { m_threadProvider.releaseObject(m_cachedThread); }
        catch (Exception e) { m_logger.error(e); }
        m_cachedThread = null;
    }

    protected Object allocateObject() {
        ValueInnerWork<Object> vw = new ValueInnerWork<Object>() {
            protected void safeRun() throws Exception {
                setValue(m_objFactory.getObject(AsynchObjectPool.this));
            }

            public String toString() {
                return "allocate object using factory: " + m_objFactory.toString();
            }
        };
        asynchOperation(vw, m_allocateObjectTimeout);
        return vw.getValue();
    }

    protected void deallocateObject(final Object o) {
        asynchOperation(
            new ExceptionCatchRunnable() {
                protected void safeRun() throws Exception {
                    m_objFactory.returnObject(o);
                }

                public String toString() {
                    return "deallocate object " + o.toString();
                }
            }, m_allocateObjectTimeout
        );
    }

    protected boolean objectWrapperValid(final Object o) {
        ValueInnerWork<Boolean> vw = new ValueInnerWork<Boolean>() {
            protected void safeRun() throws Exception {
                setValue(m_objFactory.checkObject(o));
            }

            public String toString() {
                return "check object: " + o.toString();
            }
        };
        asynchOperation(vw, m_checkTimeout);
        return vw.getValue();
    }
}
