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
package ru.rd.courier.manager;

import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.ErrorAwareObjectFactory;
import ru.rd.thread.PoolExecutorAdapter;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool2.AsynchExecutePolicy;
import ru.rd.pool2.ObjectPool2;
import ru.rd.pool2.DefaultObjectPool2;

import java.util.concurrent.Executor;
import java.util.Timer;

/**
 * User: Astepochkin
 * Date: 30.09.2008
 * Time: 16:59:31
 */
public class PoolInitializer {
    protected final CourierLogger m_logger;
    protected final ObjectPoolIntf m_threadPool;
    protected final Executor m_exec;
    protected Timer m_timer;

    public PoolInitializer(CourierLogger logger, ObjectPoolIntf threadPool) {
        m_logger = logger;
        m_threadPool = threadPool;
        m_exec = new PoolExecutorAdapter(m_threadPool);
    }

    public void setTimer(Timer timer) {
        m_timer = timer;
    }

    public ObjectPoolIntf asynchPoolFromXml(PoolObjectFactory pof, String name, Node n) {
        int maxErrorCount = DomHelper.getIntNodeAttr(n, "max-error-count", -1);
        if (maxErrorCount > 0) pof = new ErrorAwareObjectFactory(pof, maxErrorCount);
        ObjectPool2 ret = new DefaultObjectPool2(m_logger, name + "-Connections", pof);
        ret.setExecutor(m_exec);
        if (m_timer != null) ret.setTimer(m_timer);
        initAsyncPoolFromXml(ret, n);
        return ret;
    }

    public void initAsyncPoolFromXml(ObjectPool2 pool, Node n) {
        initPoolFromXml(pool, n);
        AsynchExecutePolicy ep = new AsynchExecutePolicy(m_logger, m_threadPool);
        ep.setAllocateTimeout(DomHelper.getTimeNodeAttr(n, "allocate-timeout", 30*1000));
        ep.setCheckTimeout(DomHelper.getTimeNodeAttr(n, "check-timeout", 10*1000));
        pool.setExecPolicy(ep);
    }

    public static ObjectPool2 initPoolFromXml(ObjectPool2 pool, Node n) {
        if (n == null) return pool;
        pool.setCapacityPars(
            DomHelper.getIntNodeAttr(n, "initial-capacity", 0),
            DomHelper.getIntNodeAttr(n, "increment-capacity", 1),
            DomHelper.getIntNodeAttr(n, "min-capacity", 0),
            DomHelper.getIntNodeAttr(n, "max-capacity", 0)
        );

        pool.setShrinkPars(
            DomHelper.getIntNodeAttr(n, "shrink-interval-min", 5)*60*1000,
            DomHelper.getIntNodeAttr(n, "shrink-capacity", -1),
            DomHelper.getIntNodeAttr(n, "shrink-obsolete-interval-min", 2)*60*1000
        );

        int checkIntervalMin = DomHelper.getIntNodeAttr(n, "check-interval-min", 5);
        pool.setCheckPars(checkIntervalMin*60*1000);

        int expirePeriodMin = DomHelper.getIntNodeAttr(n, "expire-period-min", 5*60);
        pool.setExpirePars(expirePeriodMin*60*1000);
        return pool;
    }
}
