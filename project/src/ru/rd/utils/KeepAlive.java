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
package ru.rd.utils;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.PoolExecutor;

import java.util.Timer;
import java.util.TimerTask;

/**
 * User: AStepochkin
 * Date: 04.02.2008
 * Time: 12:29:59
 */
public abstract class KeepAlive extends StatedObjectExtendable {
    protected final PoolExecutor m_threads;
    protected final Timer m_timer;
    private TimerTask m_timerTask = null;
    protected long m_timeout;
    protected long m_checkInterval;
    private boolean m_shouldBeStarted = false;
    private boolean m_invalidated = false;
    private boolean m_firstTry = true;

    public KeepAlive(
        CourierLogger logger, ObjectPoolIntf threadPool, Timer timer,
        long checkInterval, long timeout
    ) {
        this(logger, threadPool, timer);
        setTimeout(timeout);
        setCheckInterval(checkInterval);
    }

    public KeepAlive(CourierLogger logger, ObjectPoolIntf threadPool, Timer timer) {
        super(logger);
        m_threads = new PoolExecutor(threadPool);
        m_timer = timer;
        m_checkInterval = 0;
        m_timeout = 0;
        m_state = c_stateStopped;
    }

    public void start() {
        synchronized(lock) {
            if (m_shouldBeStarted) return;
            m_shouldBeStarted = true;
            m_firstTry = true;
        }
        launchCheck();
    }

    public boolean stop(long timeout) {
        synchronized(lock) {
            if (!m_shouldBeStarted) return true;
            m_shouldBeStarted = false;
        }
        checkStop(true);
        return waitState(c_stateStopped, timeout);
    }

    public void stop() {
        stop(0);
    }

    public final long getTimeout() {
        return m_timeout;
    }

    public final void setTimeout(long value) {
        m_timeout = value;
    }

    public final void setCheckInterval(long value) {
        m_checkInterval = value;
    }

    protected long getCheckInterval() {
        return m_checkInterval;
    }

    public boolean shouldBeStarted() {
        synchronized(lock) {
            return m_shouldBeStarted;
        }
    }

    public void setShouldBeStarted(boolean shouldBeStarted) {
        synchronized(lock) {
            if (m_shouldBeStarted == shouldBeStarted) return;
            m_shouldBeStarted = shouldBeStarted;
            checkTimerTask();
        }
    }

    public boolean isAlive() {
        synchronized(lock) {
            return m_state == c_stateStarted && !m_invalidated;
        }
    }

    public PoolExecutor getExecutor() {
        return m_threads;
    }

    protected void invalidate() {
        debug("Invalidate");
        synchronized(lock) {
            if (m_state != c_stateStarted && m_state != c_stateStarting) {
                m_logger.debug("Invalid state '" + m_state + "' for invalidation");
            }
            m_invalidated = true;
            m_firstTry = true;
        }
        launchCheck();
    }

    private static final State[] c_waitStopStates = new State[] { c_stateStarted, c_stateStopped };

    private void checkStop(boolean synch) {
        try {
            boolean shouldBeStopped;
            synchronized(lock) {
                //debug("CheckStop: invalidated = " + m_invalidated);

                shouldBeStopped = (m_invalidated || !m_shouldBeStarted);
                if (m_invalidated) m_invalidated = false;
                if (!shouldBeStopped || m_state == c_stateStopping || m_state == c_stateStopped) return;
                if (m_state == c_stateStarting) {
                    if (!waitStates(c_waitStopStates, m_timeout)) {
                        m_logger.warning("Timeout " + m_timeout + " expired waiting for 'started'");
                    }
                }
                shouldBeStopped = (m_state == c_stateStarted);
                if (shouldBeStopped) {
                    setState(c_stateStopping, false);
                }
            }

            if (shouldBeStopped) cleanupLaunch(synch);
        } finally {
            checkTimerTask();
        }
    }

    private void cleanupLaunch(boolean synch) {
        try {
            Runnable cw = getCleanupWork();
            if (cw == null) return;
            if (synch) {
                m_threads.synchExec(cw, m_timeout, m_logger);
            } else {
                m_threads.exec(cw, m_timeout, m_logger);
            }
        } catch (Exception e) {
            m_logger.warning(e);
        } finally {
            setState(c_stateStopped);
            customStopped();
        }
    }

    private void checkTimerTask() {
        synchronized(lock) {
            if (m_shouldBeStarted) {
                if (m_state != c_stateStarted && m_timerTask == null) {
                    m_timerTask = new TimerTask() {
                        public void run() {
                            try {
                                launchCheck();
                            } catch (Throwable e) {
                                m_logger.error(e);
                            }
                        }
                    };
                    m_timer.schedule(m_timerTask, m_checkInterval, m_checkInterval);
                }
            } else {
                if (m_state == c_stateStopped && m_timerTask != null) {
                    m_timerTask.cancel();
                    m_timerTask = null;
                }
            }
        }
    }

    private void checkStart() {
        boolean shouldBeStarted;
        synchronized(lock) {
            shouldBeStarted = m_shouldBeStarted && m_state == c_stateStopped;
            if (shouldBeStarted) {
                setState(c_stateStarting, false);
            }
        }

        if (shouldBeStarted) {
            init(m_timeout);
        }
        checkTimerTask();
    }

    private boolean init(long timeout) {
        final boolean firstTry = m_firstTry;
        m_firstTry = false;
        return m_threads.synchExec(
            new Runnable() {
                public void run() {
                    try {
                        init();
                        setState(c_stateStarted);
                        customStarted();
                    } catch (Exception e) {
                        if (firstTry) m_logger.warning("Error starting " + getLogDesc(), e);
                        else m_logger.debug("Error starting " + getLogDesc() + ": " + e.getMessage());
                        cleanupLaunch(false);
                    }
                }

                public String toString() {
                    return "Starting: " + getDesc();
                }
            },
            timeout, m_logger, false
        );
    }

    private void launchCheck() {
        safeAsyncRun(new Invokee() {
            public void invoke() {
                if (getState() == c_stateStarted) {
                    try { checkCustom(); } catch (Exception e) { m_logger.error(e); }
                }
                checkStop(false);
                checkStart();
            }
        }, 0);
    }

    protected final void safeAsyncRun(final Invokee activity, long timeout) {
        m_threads.exec(new SafeLoggingRunnable(m_logger) {
            public void safeRun() throws Exception {
                activity.invoke();
            }
        }, timeout, m_logger);
    }

    protected abstract void init() throws Exception;
    protected abstract Runnable getCleanupWork();
    protected void checkCustom() throws Exception {}
    protected void customStarted() {}
    protected void customStopped() {}
}
