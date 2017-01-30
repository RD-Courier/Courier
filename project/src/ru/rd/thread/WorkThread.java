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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.LinkedList;

public class WorkThread extends Thread {
    private final CourierLogger m_logger;
    private final String m_desc;
    private final Object m_lock = new Object();
    private boolean m_stop = false;
    private Runnable m_work = null;
    private FreeListener m_freeListener = null;

    public WorkThread(CourierLogger logger, ThreadGroup group, String desc, boolean delayed) {
        super(group, desc);
        m_logger = logger;
        m_desc = desc;
        setName(desc);
        if (!delayed) start();
    }

    public WorkThread(CourierLogger logger, String desc, boolean delayed) {
        this(logger, null, desc, delayed);
    }
    public WorkThread(CourierLogger logger, String desc) {
        this(logger, desc, false);
    }

    private void checkAlreadyRunning() {
        if (m_work != null) {
            throw new IllegalStateException(
                "Thread " + toString() +
                "\nis trying to launch new work while " +
                "old one has not been completed"
            );
        }
    }

    public void launchWork(final Runnable work, final FreeListener fl) {
        synchronized(m_lock) {
            checkAlreadyRunning();
            m_work = work;
            m_freeListener = fl;
            m_lock.notifyAll();
        }
    }

    public boolean launchWorkAndWait(
        final Runnable work, long timeout
    ) {
        return launchWorkAndWait(work, timeout, null);
    }

    public boolean launchWorkAndWait(
        final Runnable work, long timeout, final FreeListener fl
    ) {
        launchWork(work, fl);
        return waitWork(timeout);
    }

    private boolean waitWork(long timeout) {
        long lastTime;
        if (timeout > 0) lastTime = System.currentTimeMillis();
        else lastTime = 0;
        long timeLeft = timeout;
        while (true) {
            if (timeout > 0 && timeLeft <= 0) break;
            synchronized(m_lock) {
                if (m_stop || m_work == null) break;
                try {
                    m_lock.wait(timeout <= 0 ? timeout : timeLeft);
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (timeout > 0) {
                long now = System.currentTimeMillis();
                timeLeft -= now - lastTime;
                lastTime = now;
            }
        }
        synchronized(m_lock) {
          return (m_work == null);
        }
    }

    public boolean launchWorkAndWait(final Runnable work) {
        return launchWorkAndWait(work, 0);
    }

    public void launchWork(final Runnable work) {
        launchWork(work, null);
    }

    public boolean isActive() {
        synchronized(m_lock) {
            return !m_stop;
        }
    }

    public boolean isBusy() {
        synchronized(m_lock) {
            return (m_work != null);
        }
    }

    public boolean setFreeListener(final FreeListener fl) {
        synchronized(m_lock) {
            if (m_work == null) return false;
            m_freeListener = fl;
            return true;
        }
    }

    public Runnable getWork() {
        synchronized(m_lock) {
            return m_work;
        }
    }

    public void stopRequest() {
        synchronized(m_lock) {
            m_stop = true;
            m_lock.notifyAll();
        }
    }

    private static final List<String> s_hangingThreads = new LinkedList<String>();
    private static int m_discardedCount = 0;
    private synchronized static void addHangingThread(WorkThread thread) {
        if (s_hangingThreads.size() >= 100) {
            m_discardedCount++;
            s_hangingThreads.remove(0);
        }
        s_hangingThreads.add(thread.toString());
    }

    public synchronized static String[] getHangingThreadsInfo() {
        return s_hangingThreads.toArray(new String[s_hangingThreads.size()]);
    }

    public synchronized static int getDiscardedInfoCount() {
        return m_discardedCount;
    }

    public boolean closeOrDump(long timeout) {
        boolean ret = close(timeout);
        if (!ret) {
            interrupt();
            addHangingThread(this);
        }
        return ret;
    }

    private void logError(String message, Throwable e) {
        if (m_logger == null) {
            e.printStackTrace(System.err);
        } else {
            if (message == null) {
                m_logger.error(e);
            } else {
                m_logger.error(message, e);
            }
        }
    }

    public boolean close(long timeout) {
        stopRequest();

        try {
            return waitWork(timeout);
        } catch (Exception e) {
            logError(null, e);
            synchronized(m_lock) {
                return m_work == null;
            }
        }
    }

    public void close() {
        close(0);
    }

    public void run() {
        try {
            mainLoop();
        } catch (Throwable e) {
            logError("WorkThread '" + m_desc + "' was interrupted by exception", e);
        } finally {
            synchronized(m_lock) {
                m_stop = true;
                m_work = null;
                m_lock.notifyAll();
            }
        }
    }

    protected final void debug(String message) {
        m_logger.debug(
            "ThreadWork '" + m_desc + "': " + m_work + ": " + message
        );
    }

    public void mainLoop() throws InterruptedException {
        Runnable work;
        FreeListener fl;
        while (true) {
            synchronized(m_lock) {
                while ((m_work == null) && !(m_stop || isInterrupted())) {
                    m_lock.wait();
                }
                if (m_stop || isInterrupted()) {
                    break;
                }
                work = m_work;
                fl = m_freeListener;
            }

            if (work != null) {
                //debug("work starting");
                work.run();
                //debug("work finished");
                synchronized(m_lock) {
                    m_work = null;
                    m_freeListener = null;
                    m_lock.notifyAll();
                }

                if (fl != null) fl.free(work);
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("'").append(m_desc).append("'");
        sb.append("; state = ").append(getState());
        if (m_work != null) {
            sb.append(stackTraceToString(getStackTrace()));
            sb.append('\n');
        } else {
            sb.append(' ');
        }
        sb.append("work: ").append(m_work);
        if (m_work != null) sb.append("; class = ").append(m_work.getClass().getName());
        return sb.toString();
    }

    private static String stackTraceToString(StackTraceElement[] st) {
        StringBuffer sb = new StringBuffer(32*st.length);
        for (StackTraceElement ste : st) {
            sb.append("\n\tat ").append(ste.getClassName()).append(".");
            sb.append(ste.getMethodName()).append("(").append(ste.getFileName());
            sb.append(":").append(ste.getLineNumber()).append(")");
        }
        return sb.toString();
    }

}
