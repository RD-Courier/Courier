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
import ru.rd.courier.utils.CloseEventListener;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolException;
import ru.rd.pool.ReleaseHelper;

import java.util.*;

public class AsyncBuffer<E> {
    private boolean m_closed = false;
    private int m_activeThreadsCnt = 0;
    private int m_maxThreadCnt;
    private CourierLogger m_logger;
    private ObjectPoolIntf m_threadPool;
    private LinkedList<E> m_data = new LinkedList<E>();
    private Timer m_timer;
    private boolean m_borrowedTimer;
    private Receiver<E> m_receiver;
    private long m_period;
    private int m_partSize;
    private CloseEventListener m_closeListener;
    private TimerTask m_launcher;

    public interface Receiver<E> {
        void handleData(List<E> dataPart);
        void close();
    }

    private class Worker implements Runnable, FreeListener {
        private final ReleaseHelper m_rHelper;
        private List<E> m_part;

        public Worker(List<E> part) {
            incActiveHandlersCnt(1);
            m_part = part;
            m_rHelper = new ReleaseHelper(m_threadPool);
            ((WorkThread)m_rHelper.getObject()).launchWork(this, this);
        }

        public void run() {
            try {
                while (m_part != null) {
                    m_receiver.handleData(m_part);
                    m_part = getDataPart();
                }
            } catch (Exception e) {
                m_logger.error("Uncaught exception in worker thread", e);
            } finally {
                incActiveHandlersCnt(-1);
            }
        }

        public void free(Runnable w) {
            m_rHelper.releaseObject();
        }
    }

    public AsyncBuffer(
        CourierLogger logger,
        ObjectPoolIntf threadPool, int maxThreadCnt,
        Timer timer, long period,
        Receiver<E> receiver, int partSize,
        CloseEventListener closeListener
    ) {
        if (logger == null)
            throw new IllegalArgumentException("Logger cannot be null");
        m_logger = logger;
        if (maxThreadCnt < 1)
            throw new IllegalArgumentException("Max threads count must be > 0");
        m_maxThreadCnt = maxThreadCnt;
        if (threadPool == null)
            throw new IllegalArgumentException("ThreadPool cannot be null");
        m_threadPool = threadPool;
        m_receiver = receiver;
        m_period = period;
        m_partSize = partSize;
        m_closeListener = closeListener;
        m_timer = timer;
        m_borrowedTimer = timer != null;
    }

    private void initTimerTask() {
        if (m_timer == null) m_timer = new Timer();
        m_launcher = new TimerTask() {
            public void run() {
                try {
                    launchThread();
                } catch(Throwable e) {
                    m_logger.error(e);
                }
            }
        };
        m_timer.schedule(m_launcher, 0, m_period);
    }

    private void ensureProgress() throws PoolException {
        if (m_activeThreadsCnt < 1) {
            if (m_period > 0) {
                if (m_launcher == null) initTimerTask();
            } else {
                launchThread();
            }
        }
    }

    private void cancelTimerTask() {
        if (m_launcher != null) {
            m_launcher.cancel();
            m_launcher = null;
        }
    }

    private synchronized void incActiveHandlersCnt(int shift) {
        m_activeThreadsCnt += shift;
        if (m_closed && (m_activeThreadsCnt < 1)) {
            notifyAll();
        }
    }

    public synchronized void add(E obj) throws PoolException {
        if (m_closed) throw new IllegalStateException("Buffer closed");
        m_data.addLast(obj);
        ensureProgress();
        objectAdding(obj);
    }

    protected void objectAdding(E obj) throws PoolException {}
    protected void objectRemoving(E obj) {}

    public synchronized void add(E[] objs) throws PoolException {
        for (E obj : objs) {
            add(obj);
        }
    }

    private synchronized List<E> getDataPart() {
        if (m_data.size() == 0) {
            cancelTimerTask();
            return null;
        }
        List<E> res = new LinkedList<E>();
        int i = 0;
        for (Iterator<E> it = m_data.iterator(); (i < m_partSize) && it.hasNext(); i++) {
            E obj = it.next();
            res.add(obj);
            objectRemoving(obj);
            it.remove();
        }

        String mes = (
            "thread: " + Thread.currentThread().getName() +
            " getDataPart: returned part size = " + res.size() +
            (res.size() > 0 ? " first = " + res.get(0) : "")
        );
        m_logger.debug(mes);

        return res;
    }

    public synchronized void launchThread() throws PoolException {
        if (m_activeThreadsCnt < m_maxThreadCnt) {
            List<E> l = getDataPart();
            if (l != null) new Worker(l);
        }
    }

    public synchronized void flush() throws PoolException {
        if (m_data.size() > 0 || m_activeThreadsCnt > 0) {
            if (m_data.size() > 0) ensureProgress();
            try {
                wait();
            } catch (InterruptedException e) {
                throw new PoolException(e);
            }
        }
    }

    public synchronized void close() throws PoolException {
        m_closed = true;
        try { flush(); } catch (Exception e) { m_logger.warning(e); }
        if (m_launcher != null) m_logger.warning("Timer task != null on close");
        if (!m_borrowedTimer) {
            try { m_timer.cancel(); } catch (Exception e) { m_logger.warning(e); }
        }
        m_timer = null;
        if (m_receiver != null) {
            try { m_receiver.close(); } catch (Exception e) { m_logger.warning(e); }
        }
        if (m_closeListener != null) {
            try { m_closeListener.closed(); } catch (Exception e) { m_logger.warning(e); }
        }
    }

    public synchronized int getMaxThreadCount() {
        return m_maxThreadCnt;
    }

    public synchronized int getPartSize() {
        return m_partSize;
    }

    public synchronized long getPeriod() {
        return m_period;
    }
}
