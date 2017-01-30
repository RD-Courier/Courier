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
package ru.rd.pool;

import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.scripting.ErrorAwareObjectFactory;
import ru.rd.pool2.DefaultObjectPool2;
import ru.rd.pool2.AsynchExecutePolicy;
import ru.rd.pool2.TimedObjectPoolAdapter;
import ru.rd.pool2.ObjectPool2;
import ru.rd.thread.PoolExecutorAdapter;

import java.util.Timer;

/**
 * User: AStepochkin
 * Date: 16.04.2009
 * Time: 14:41:28
 */
public class ConfHelper {
    public static ObjectPool asynchPoolFromXml(
        ObjectPoolIntf threadPool, PoolObjectFactory pof,
        String name, Node n, CourierLogger poolLogger
    ) {
        AsynchObjectPool ret = new AsynchObjectPool(name, poolLogger, threadPool, pof);
        initAsyncPoolFromXml(ret, n);
        return ret;
    }

    public static ObjectPoolIntf asynchPool2FromXml(
        CourierLogger poolLogger, ObjectPoolIntf threadPool, Timer timer,
        String name, Node n, PoolObjectFactory pof
    ) {
        DefaultObjectPool2 pool = new DefaultObjectPool2(poolLogger, name, pof);
        pool.setTimer(timer);
        pool.setExecutor(new PoolExecutorAdapter(threadPool));
        AsynchExecutePolicy execp = new AsynchExecutePolicy(poolLogger, threadPool);
        execp.setAllocateTimeout(DomHelper.getTimeNodeAttr(n, "allocate-timeout", 30*1000, "ms"));
        execp.setCheckTimeout(DomHelper.getTimeNodeAttr(n, "check-timeout", 10*1000, "ms"));
        pool.setExecPolicy(execp);
        initPool2FromXml(pool, n);
        return new TimedObjectPoolAdapter(pool, DomHelper.getTimeNodeAttr(n, "wait-timeout", 30*1000, "ms"));
    }

    public static ObjectPoolIntf asynchPoolFromXmlEx(
        CourierLogger poolLogger, ObjectPoolIntf threadPool, Timer timer,
        String name, Node n, PoolObjectFactory pof, boolean isPool2
    ) {
        int maxErrorCount = DomHelper.getIntNodeAttr(n, "max-error-count", -1);
        if (maxErrorCount > 0) pof = new ErrorAwareObjectFactory(pof, maxErrorCount);
        if (isPool2) {
            return asynchPool2FromXml(poolLogger, threadPool, timer, name, n, pof);
        } else {
            return asynchPoolFromXml(threadPool, pof, name, n, poolLogger);
        }
    }

    public static ObjectPool synchPoolFromXml(
        PoolObjectFactory pof,  String dbProfileName, Node n, CourierLogger poolLogger
    ) {
        SynchObjectPool ret = new SynchObjectPool(
            dbProfileName + "-Connections", poolLogger, pof
        );
        initPoolFromXml(ret, n);
        return ret;
    }

    public static void initPoolFromXml(ObjectPool pool, Node n) {
        pool.setCapacityPars(
            DomHelper.getIntNodeAttr(n, "initial-capacity", 0),
            DomHelper.getIntNodeAttr(n, "increment-capacity", 1),
            DomHelper.getIntNodeAttr(n, "max-capacity", -1)
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
    }

    public static void initAsyncPoolFromXml(AsynchObjectPool pool, Node n) {
        initPoolFromXml(pool, n);
        pool.setTimeouts(
            DomHelper.getTimeNodeAttr(n, "allocate-timeout", 30*1000),
            DomHelper.getTimeNodeAttr(n, "check-timeout", 10*1000)
        );
    }

    public static void initPool2FromXml(ObjectPool2 pool, Node n) {
        pool.setCapacityPars(
            DomHelper.getIntNodeAttr(n, "initial-capacity", 0),
            DomHelper.getIntNodeAttr(n, "increment-capacity", 1),
            DomHelper.getIntNodeAttr(n, "min-capacity", -1),
            DomHelper.getIntNodeAttr(n, "max-capacity", -1)
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
    }
}
