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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User: AStepochkin
 * Date: 27.07.2005
 * Time: 12:41:18
 */
public class PoolExecutor {
    private final ObjectPoolIntf m_threadPool;

    public PoolExecutor(ObjectPoolIntf threadPool) {
        m_threadPool = threadPool;
    }

    public void exec(final Runnable work) {
        final WorkThread thread = (WorkThread)m_threadPool.getObject();
        thread.launchWork(work, new FreeListener(){
            public void free(Runnable w) {
                if (m_threadPool.isStarted()) m_threadPool.releaseObject(thread);
            }
        });
    }

    public void exec(
        final Runnable work, final long timeout, final CourierLogger logger
    ) {
        final WorkThread thread = (WorkThread)m_threadPool.getObject();
        thread.launchWork(
            new Runnable() {
                public void run() {
                    WorkThread toThread = (WorkThread)m_threadPool.getObject();
                    boolean success = toThread.launchWorkAndWait(work, timeout);
                    if (success) {
                        m_threadPool.releaseObject(toThread);
                    } else {
                        logger.warning(
                            "Timeout (" + timeout +
                            ") expired executing work: " + work.toString()
                        );
                        m_threadPool.releaseAndRemoveObject(toThread);
                    }
                }
            },
            new FreeListener(){
                public void free(Runnable w) {
                    m_threadPool.releaseObject(thread);
                }
            }
        );
    }

    public boolean synchExec(
        final Runnable work, final long timeout,
        final CourierLogger logger
    ) {
        return synchExec(work, timeout, logger, true);
    }
    
    public boolean synchExec(
        final Runnable work, final long timeout,
        final CourierLogger logger, boolean reportError
    ) {
        final WorkThread thread = (WorkThread)m_threadPool.getObject();
        boolean success = thread.launchWorkAndWait(work, timeout);
        if (success) {
            m_threadPool.releaseObject(thread);
        } else {
            if (reportError) {
                logger.error(
                    "Timeout (" + timeout +
                    ") expired executing work: " + work.toString()
                );
            }
            m_threadPool.releaseAndRemoveObject(thread);
        }
        return success;
    }

    public boolean synchExec(
        final Runnable work, final long timeout,
        final CourierLogger logger,
        final Runnable correction, final long correctTimeout
    ) {
        final WorkThread thread = (WorkThread)m_threadPool.getObject();
        final CountDownLatch releasedSignal = new CountDownLatch(1);
        boolean success = thread.launchWorkAndWait(
            work, timeout,
            new FreeListener() {
                public void free(Runnable w) {
                    releasedSignal.countDown();
                    m_threadPool.releaseObject(thread);
                }
            }
        );
        if (!success) {
            logger.error(
                "Timeout (" + timeout +
                ") expired executing work: " + work.toString()
            );
            correction.run();
            final WorkThread wt = (WorkThread)m_threadPool.getObject();
            wt.launchWork(
                new Runnable() {
                    public void run() {
                        try {
                            if (!releasedSignal.await(correctTimeout, TimeUnit.MILLISECONDS)) {
                                if (releasedSignal.getCount() > 0) {
                                    m_threadPool.releaseAndRemoveObject(thread);
                                }
                            }
                        } catch (InterruptedException e) {
                            logger.warning(e);
                        }
                    }
                },
                new FreeListener() {
                    public void free(Runnable w) {
                        m_threadPool.releaseObject(wt);
                    }
                }
            );
        }
        return success;
    }
}
