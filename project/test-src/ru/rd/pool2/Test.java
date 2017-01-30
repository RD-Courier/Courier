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

import junit.framework.TestCase;

import java.util.logging.Logger;
import java.util.logging.Handler;

import ru.rd.courier.logging.SimpleFormatter;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.CourierLoggerAdapter;
import ru.rd.courier.logging.test.NullLogger;
import ru.rd.thread.PoolExecutorAdapter;
import ru.rd.pool.*;

/**
 * User: STEPOCHKIN
 * Date: 11.09.2008
 * Time: 11:27:51
 */
public abstract class Test extends TestCase {
    protected CourierLogger m_logger;
    private ObjectPoolIntf m_threadPool;
    protected ObjectPool2 m_pool;

    protected static class ObjFactory extends TestObjectFactory {
        private int m_validCheckCount;

        public ObjFactory(int validCheckCount) {
            m_validCheckCount = validCheckCount;
        }

        protected TestObject createObject(int number) {
            TestObject res = new TestObject(number);
            res.setValidCheckCount(m_validCheckCount);
            return res;
        }
    }

    protected void setUp() throws java.lang.Exception {
        super.setUp();
        Logger rlog = Logger.getLogger("");
        for (Handler h: rlog.getHandlers()) {
            h.setFormatter(new SimpleFormatter("{0,time,mm:ss.SSS}"));
        }
        //rlog.addHandler(new ConsoleHandler());
        m_logger = new CourierLoggerAdapter(rlog);
        //CourierLogger logger = new ConsoleCourierLogger("");
        m_threadPool = PoolExecutorAdapter.createThreadPool(new NullLogger(), "ThreadPool");
        m_threadPool.start();

        m_pool = new DefaultObjectPool2(m_logger, "TestPool", createObjectFactory(m_logger));
        m_pool.setExecutor(new PoolExecutorAdapter(m_threadPool));
        setPoolProps();
        m_pool.start();
    }

    protected void setPoolProps() {}
    protected abstract PoolObjectFactory createObjectFactory(CourierLogger logger);

    protected void tearDown() throws java.lang.Exception {
        m_threadPool.close();
        m_threadPool = null;
        m_pool.close();
        m_pool = null;
        m_logger = null;
        super.tearDown();
    }

    protected final void slog(String m) {
        System.out.println(System.currentTimeMillis() + " - " + m);
    }

    protected final void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
