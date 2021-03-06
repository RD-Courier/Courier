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
import ru.rd.pool.SynchObjectPool;
import ru.rd.pool2.DefaultObjectPool2;
import ru.rd.pool2.ObjectPool2;

import java.util.concurrent.Executor;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 16:02:29
 */
public class PoolExecutorAdapter implements Executor {
    private PoolExecutor m_exec;

    public static ObjectPoolIntf createThreadPool(CourierLogger logger, String desc) {
        return new SynchObjectPool(
            desc, logger,
            new ThreadFactory(logger, Thread.currentThread().getThreadGroup(), desc, false)
        );
    }

    public static ObjectPool2 createThreadPool2(CourierLogger logger, String desc) {
        return new DefaultObjectPool2(
            logger, desc,
            new ThreadFactory(logger, Thread.currentThread().getThreadGroup(), desc, false)
        );
    }

    public PoolExecutorAdapter(ObjectPoolIntf threadPool) {
        m_exec = new PoolExecutor(threadPool);
    }

    public void execute(Runnable command) {
        m_exec.exec(command);
    }
}
