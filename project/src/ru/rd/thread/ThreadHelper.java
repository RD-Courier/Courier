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

/**
 * User: AStepochkin
 * Date: 15.08.2006
 * Time: 16:15:42
 */
public class ThreadHelper {
    public static boolean waitEvent(Object lock, Condition eventOccured, long timeout) {
        long lastTime;
        if (timeout > 0) lastTime = System.currentTimeMillis();
        else lastTime = 0;

        long timeLeft = timeout;
        while (true) {
            synchronized(lock) {
                if (eventOccured.isTrue()) return true;
                if (timeout > 0 && timeLeft <= 0) return false;
                try {
                    lock.wait(timeout <= 0 ? timeout : timeLeft);
                } catch (InterruptedException e) {
                    return false;
                }
            }
            if (timeout > 0) {
                long now = System.currentTimeMillis();
                timeLeft -= now - lastTime;
                lastTime = now;
            }
        }
    }

    public static void exec(final ObjectPoolIntf threadPool, final Runnable work) {
        (new PoolExecutor(threadPool)).exec(work);
    }

    public static void exec(final ObjectPoolIntf threadPool, final Runnable work, final long timeout, final CourierLogger logger) {
        (new PoolExecutor(threadPool)).exec(work, timeout, logger);
    }

    public static boolean synchExec(
        final ObjectPoolIntf threadPool,
        final Runnable work, final long timeout, final CourierLogger logger
    ) {
        return (new PoolExecutor(threadPool)).synchExec(work, timeout, logger);
    }

    public static boolean synchExec(
        final ObjectPoolIntf threadPool,
        final Runnable work, final long timeout,
        final CourierLogger logger,
        final Runnable correction, final long correctTimeout
    ) {
        return (new PoolExecutor(threadPool)).synchExec(work, timeout, logger, correction, correctTimeout);
    }
}
