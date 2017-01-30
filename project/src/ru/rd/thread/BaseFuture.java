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

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;

/**
 * User: AStepochkin
 * Date: 14.10.2008
 * Time: 15:47:48
 */
public class BaseFuture<V> implements Future<V> {
    private final Object lock;
    private final CourierLogger m_logger;
    private FutureListener m_firstListener;
    private List<FutureListener> m_otherListeners;
    private V m_result;
    private boolean m_ready;
    private boolean m_cancelled;
    private int m_waiters;

    public BaseFuture(CourierLogger logger) {
        lock = new Object();
        m_logger = logger;
    }

    public void join() {
        awaitUninterruptibly();
    }

    public boolean join(long timeoutMillis) {
        return awaitUninterruptibly(timeoutMillis);
    }

    private Future<V> awaitUninterruptibly() {
        synchronized (lock) {
            while (!m_ready) {
                m_waiters++;
                //noinspection EmptyCatchBlock
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                } finally {
                    m_waiters--;
                }
            }
        }

        return this;
    }

    private boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
        long startTime = timeoutMillis <= 0 ? 0 : System.currentTimeMillis();
        long waitTime = timeoutMillis;

        synchronized (lock) {
            if (m_ready) {
                return m_ready;
            } else if (waitTime <= 0) {
                return m_ready;
            }

            m_waiters++;
            try {
                for (;;) {
                    try {
                        lock.wait(waitTime);
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        }
                    }

                    if (m_ready) {
                        return true;
                    } else {
                        waitTime = timeoutMillis
                                - (System.currentTimeMillis() - startTime);
                        if (waitTime <= 0) {
                            return m_ready;
                        }
                    }
                }
            } finally {
                m_waiters--;
            }
        }
    }

    protected void setValue(V newValue) {
        synchronized (lock) {
            // Allow only once.
            if (m_ready || m_cancelled) {
                return;
            }

            m_result = newValue;
            m_ready = true;
            if (m_waiters > 0) {
                lock.notifyAll();
            }
        }

        notifyListeners();
    }

    protected V getValue() {
        synchronized (lock) {
            return m_result;
        }
    }

    public void addListener(FutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (m_ready) {
                notifyNow = true;
            } else {
                if (m_firstListener == null) {
                    m_firstListener = listener;
                } else {
                    if (m_otherListeners == null) {
                        m_otherListeners = new ArrayList<FutureListener>(1);
                    }
                    m_otherListeners.add(listener);
                }
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
    }

    public void removeListener(FutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (lock) {
            if (!m_ready) {
                if (listener == m_firstListener) {
                    if (m_otherListeners != null && !m_otherListeners.isEmpty()) {
                        m_firstListener = m_otherListeners.remove(0);
                    } else {
                        m_firstListener = null;
                    }
                } else if (m_otherListeners != null) {
                    m_otherListeners.remove(listener);
                }
            }
        }
    }

    private void notifyListeners() {
        // There won't be any visibility problem or concurrent modification
        // because 'm_ready' flag will be checked against both addListener and
        // removeListener calls.
        if (m_firstListener != null) {
            notifyListener(m_firstListener);
            m_firstListener = null;

            if (m_otherListeners != null) {
                for (FutureListener l : m_otherListeners) {
                    notifyListener(l);
                }
                m_otherListeners = null;
            }
        }
    }

    private void notifyListener(FutureListener l) {
        try {
            l.operationComplete(this);
        } catch (Throwable t) {
            m_logger.error(t);
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (lock) {
            if (m_ready || m_cancelled) return false;

            m_cancelled = true;
            m_ready = true;
            if (mayInterruptIfRunning) interruptIfRunning();
            if (m_waiters > 0) {
                lock.notifyAll();
            }
        }

        notifyListeners();
        return true;
    }

    protected void interruptIfRunning() {}

    public boolean isCancelled() {
        synchronized (lock) {
            return m_cancelled;
        }
    }

    public boolean isDone() {
        synchronized (lock) {
            return m_ready;
        }
    }

    public V get() throws InterruptedException, ExecutionException {
        join();
        return m_result;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        join(unit.toMillis(timeout));
        return m_result;
    }
}
