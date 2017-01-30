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
import ru.rd.utils.StatedObjectExtendable;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * User: STEPOCHKIN
 * Date: 13.09.2008
 * Time: 9:16:20
 */
public abstract class AsynchProcessing<Resource, ProcessTarget> extends StatedObjectExtendable {
    protected final CourierLogger m_logger;
    private String m_desc;
    private final Queue<ProcessTarget> m_targets = new LinkedList<ProcessTarget>();
    private final Executor m_exec;
    private final int m_maxExec;
    private int m_maxTargetCount;
    private long m_checkInterval = 5000;
    private long m_lastExecSleepInterval = 1000;
    private Timer m_timer;
    private boolean m_ownsTimer;

    private int m_chunkSize = 1;
    private int m_execCount = 0;
    private boolean m_isThreadLaunching = false;
    private TimerTask m_checkTask;
    private boolean m_execWaiting = false;
    private int m_lastSize = 0;

    private int m_bufferSize = 0;
    private long m_bufferInterval = 0;
    private TimerTask m_bufferTask = null;

    private static volatile int sDescCount = 1;
    private static String getDefaultDesc() {
        return "AsynchProcessing-" + sDescCount++;
    }

    public void setBufferProperties(long bufferInterval, int bufferSize) {
        m_bufferInterval = bufferInterval;
        m_bufferSize = bufferSize;
    }

    public AsynchProcessing(CourierLogger logger, Executor exec, int maxExec) {
        super(logger);
        m_logger = logger;
        m_exec = exec;
        m_maxExec = maxExec;
        m_state = c_stateStarted;
        m_desc = getDefaultDesc();
    }

    public CourierLogger getLogger() {
        return m_logger;
    }

    public String getDesc() {
        return m_desc;
    }

    public void setDesc(String desc) {
        if (desc == null) {
            m_desc = getDefaultDesc();
        } else {
            m_desc = desc;
        }
    }

    public final void setTimer(Timer timer) {
        Timer oldTimer;
        boolean oldOwnsTimer;
        synchronized(lock) {
            //debug("setTimer: " + timer);
            oldTimer = m_timer;
            oldOwnsTimer = m_ownsTimer;
            m_timer = timer;
            m_ownsTimer = false;
        }
        if (oldTimer != null && oldOwnsTimer) {
            try {
                //debug("Timer cancel");
                oldTimer.cancel();
            } catch (Exception e) {
                m_logger.warning("Error cancelling timer", e);
            }
        }
    }

    private static int s_timerCount = 1;
    private Timer getTimer() {
        synchronized(lock) {
            if (m_timer == null) {
                String timerName = getDesc() + "-Timer-" + s_timerCount++;
                //debug("Creating timer: " + timerName);
                m_timer = new Timer(timerName);
                m_ownsTimer = true;
            }
            return m_timer;
        }
    }

    public void setCheckInterval(long value) {
        m_checkInterval = value;
    }

    public void setLastExecSleepInterval(long value) {
        m_lastExecSleepInterval = value;
    }

    public void setChunkSize(int value) {
        if (value < 1) {
            throw new RuntimeException("Invalid chunk size " + value + " should be > 0");
        }
        m_chunkSize = value;
    }

    public void setMaxTargetCount(int value) {
        m_maxTargetCount = value;
    }

    private boolean hasThreads() {
        return m_execCount > 0 || m_isThreadLaunching;
    }

    private class BufferTask extends TimerTask {
        public void run() {
            boolean launch = false;
            synchronized(lock) {
                if (m_bufferTask != null) {
                    m_bufferTask.cancel();
                    m_bufferTask = null;
                }
                if (m_execWaiting) {
                    lock.notifyAll();
                } else {
                    m_isThreadLaunching = true;
                    launch = true;
                }
            }
            if (launch) tryLaunchThread();
        }
    }

    public final boolean addTarget(ProcessTarget target, boolean processAtOnce) {
        boolean res;
        boolean launch;
        synchronized(lock) {
            ensureState(c_stateStarted);
            res = m_targets.offer(target);
            if (m_maxTargetCount > 0) {
                while (m_targets.size() > m_maxTargetCount) {
                    m_targets.poll();
                }
            }
            if (!res || m_resUnavailable || !processAtOnce) return res;

            launch = false;
            if (m_bufferInterval > 0) {
                if (m_targets.size() < m_bufferSize) {
                    if (m_bufferTask == null) {
                        m_bufferTask = new BufferTask();
                        m_timer.schedule(m_bufferTask, m_bufferInterval);
                    }
                } else {
                    if (m_bufferTask != null) {
                        m_bufferTask.cancel();
                        m_bufferTask = null;
                    }
                    launch = true;
                }
            } else {
                launch = !hasThreads();
            }

            if (launch) {
                if (m_execWaiting) {
                    lock.notifyAll();
                    launch = false;
                } else if (hasThreads()) {
                    launch = false;
                }
            }

            if (launch) {
                m_isThreadLaunching = true;
            }
        }
        if (launch) tryLaunchThread();
        return res;
    }

    public final boolean addTarget(ProcessTarget target) {
        return addTarget(target, true);
    }

    private boolean m_resUnavailable;

    public final void resourceAvailable() {
        boolean launch;
        synchronized(lock) {
            m_resUnavailable = false;
            launch = m_state == c_stateStarted && m_targets.size() > 0 && !hasThreads();
            if (launch) m_isThreadLaunching = true;
        }
        if (launch) tryLaunchThread();
    }

    public final void resourceUnavailable() {
        synchronized(lock) {
            m_resUnavailable = true;
        }
    }

    private void tryLaunchThread() {
        synchronized(lock) {
            debug("tryLaunchThread: ExecCount = " + m_execCount + " ResourceAvailable = " + (!m_resUnavailable));
            if (m_bufferTask != null) {
                m_bufferTask.cancel();
                m_bufferTask = null;
            }
            if (m_resUnavailable || (m_maxExec > 0 && m_execCount >= m_maxExec)) return;
        }
        m_exec.execute(new ThreadActivity());
    }

    private void checkPerformance() {
        boolean launch = false;
        synchronized(lock) {
            debug("checkPerformance: state = " + m_state + " LastSize = " + m_lastSize+ " ResourceAvailable = " + (!m_resUnavailable));
            if (m_state == c_stateStarted) {
                launch = m_targets.size() > m_lastSize;
                m_lastSize = m_targets.size();
                if (launch && m_execWaiting) {
                    lock.notifyAll();
                    launch = false;
                }
            } else {
                if (m_checkTask != null) {
                    m_checkTask.cancel();
                    m_checkTask = null;
                }
            }
        }
        if (launch) tryLaunchThread();
    }

    protected void debug(String message) {
        super.debug(
            message + " | ExecCount = " + m_execCount + " TargetCount = " + m_targets.size()
            + " ThreadName = " + Thread.currentThread().getName());
        //+ "\n" + ErrorHelper.stackTraceToString(Thread.currentThread().getStackTrace())
    }

    private final List<ThreadActivity> m_activities = new LinkedList<ThreadActivity>();

    private class ThreadActivity implements Runnable {
        private Resource m_resource;
        private boolean m_activityClosed = false;

        public void run() {
            try {
                safeRun();
            } catch (Exception e) {
                m_logger.error(e);
            }
        }

        private boolean isActivityClosed() {
            return m_state != c_stateStarted || m_activityClosed;
        }

        public void stopActivity() {
            synchronized(lock) {
                m_activityClosed = true;
            }
        }

        public void cancelActivity() {
            Resource resource = null;
            synchronized(lock) {
                if (m_activityClosed) return;
                m_activityClosed = true;
                if (m_execWaiting) {
                    lock.notifyAll();
                } else {
                    resource = m_resource;
                    m_resource = null;
                }
            }
            try {
                if (resource != null) releaseCancelledResource(resource);
            } catch (Exception e) {
                try {
                    m_logger.warning("Error cancelling resource " + m_resource, e);
                } catch (Exception e1) { m_logger.warning(e1.initCause(e)); }
            }
        }

        private void threadExit_nl() {
            m_activities.remove(this);
            m_execCount--;
            if (m_execCount <= 0) {
                if (m_checkTask != null && m_targets.size() == 0) {
                    m_checkTask.cancel();
                    m_checkTask = null;
                }
                if (m_state == c_stateStopping) {
                    setState(c_stateStopped);
                }
            }
        }

        private void safeRun() throws Exception {
            synchronized(lock) {
                debug("threadActivity");
                if (!isActivityClosed() && m_checkInterval > 0 && m_checkTask == null) {
                    m_checkTask = new TimerTask() {
                        public void run() { checkPerformance(); }
                    };
                    getTimer().schedule(m_checkTask, m_checkInterval, m_checkInterval);
                }
                m_isThreadLaunching = false;
                if (m_execCount == 0) m_lastSize = m_targets.size();
                m_execCount++;
                m_activities.add(this);
            }
            try {
                Resource resource = findResource();
                synchronized(lock) {
                    m_resource = resource;
                }
                if (resource != null) {
                    try {
                        for (;;) {
                            boolean resourceValid = processResource();
                            synchronized(lock) {
                                if (!isActivityClosed() && resourceValid && m_execCount == 1 && m_lastExecSleepInterval > 0) {
                                    m_execWaiting = true;
                                    //debug("threadActivity wait: lastExecSleepInterval = " + m_lastExecSleepInterval);
                                    lock.wait(m_lastExecSleepInterval);
                                    //debug("threadActivity wakeup");
                                    m_execWaiting = false;
                                }
                                if (isActivityClosed() || !resourceValid) break;
                                if (m_bufferInterval <= 0) {
                                    if (m_targets.size() == 0) break;
                                } else {
                                    if (m_targets.size() < m_bufferSize) break;
                                }
                            }
                        }
                    } finally {
                        synchronized(lock) {
                            resource = m_resource;
                            m_resource = null;
                        }
                        try { if (resource != null) releaseResource(resource); }
                        catch (Throwable e) { m_logger.warning(e); }
                    }
                }
            } finally {
                synchronized(lock) {
                    threadExit_nl();
                }
            }
            debug("threadActivity end" );
        }

        private boolean processResource() {
            boolean rv = true;
            for (;;) {
                ProcessTarget target;
                List<ProcessTarget> chunk = null;
                Resource resource;
                synchronized(lock) {
                    if (isActivityClosed()) break;
                    resource = m_resource;
                    if (resource == null) break;
                    target = m_targets.poll();
                    if (target == null) break;
                    for (int i = 1; i < m_chunkSize; i++) {
                        ProcessTarget t = m_targets.poll();
                        if (t == null) break;
                        if (chunk == null) chunk = new LinkedList<ProcessTarget>();
                        chunk.add(t);
                    }
                }
                try {
                    if (chunk == null) {
                        //debug("Target begin: " + target);
                        process(resource, target);
                        //debug("Target end: " + target);
                    } else {
                        chunk.add(0, target);
                        //debug("Target chunk begin: " + chunk);
                        process(resource, chunk);
                        //debug("Target chunk end: " + chunk);
                    }
                } catch (Throwable e) {
                    m_logger.warning(e);
                    try {
                        if (!isResourceValid(resource)) {
                            rv = false;
                            m_targets.offer(target);
                            break;
                        } else {
                            targetFailed(target, e);
                        }
                    } catch (Throwable e1) {
                        m_logger.error(e1.initCause(e));
                        break;
                    }
                }
            }
            return rv;
        }

        private void targetFailed(ProcessTarget target, Throwable e) {
            try {
                m_logger.warning("Error processing (" + target + ")", e);
            } catch (Throwable e1) {
                m_logger.error(e1.initCause(e));
            }
        }
    }

    public void stopActivities() {
        synchronized(lock) {
            for (ThreadActivity activity: m_activities) {
                activity.cancelActivity();
            }
        }
    }

    public boolean close(long timeout) {
        int count1, count2;
        boolean res;
        synchronized(lock) {
            if (m_state == c_stateStopped) return true;
            if (m_bufferTask != null) {
                m_bufferTask.cancel();
                m_bufferTask = null;
            }
            count1 = m_execCount;
            stopActivities();
            if (m_state == c_stateStarted) {
                setTimer(null);
                m_state = m_execCount > 0 || m_isThreadLaunching ? c_stateStopping : c_stateStopped;
            }
            if (m_state == c_stateStopping) waitState(c_stateStopped, timeout);
            count2 = m_execCount;
            res = m_state == c_stateStopped;
        }
        debug("AsynchProcessing.close: ExecCountBefore = " + count1 + " ExecCountAfter = " + count2);
        return res;
    }

    public String toDebugString() {
        return "Targets=" + m_targets.size() + " Execs=" + m_execCount;
    }

    protected abstract Resource findResource() throws Exception;
    protected abstract boolean isResourceValid(Resource resource) throws Exception;
    protected abstract void releaseResource(Resource resource) throws Exception;
    protected abstract void releaseCancelledResource(Resource resource) throws Exception;
    protected abstract void process(Resource resource, ProcessTarget target) throws Exception;
    protected void process(Resource resource, List<ProcessTarget> targets) throws Exception {
        throw new UnsupportedOperationException();
    }
}
