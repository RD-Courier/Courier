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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.TestObject;
import ru.rd.utils.TimeElapsed;

import java.util.LinkedList;
import java.util.List;

/**
 * User: Astepochkin
 * Date: 01.09.2008
 * Time: 12:05:58
 */
public class PerformanceTest extends Test {
    protected PoolObjectFactory createObjectFactory(CourierLogger logger) {
        ObjFactory f = new ObjFactory(1);
        f.setGetSleepInterval(0);
        f.setCheckSleepInterval(0);
        return f;
    }

    protected void setPoolProps() {
        super.setPoolProps();
        m_pool.setRinseInterval(100000);
    }

    public void test() throws Exception {
        ObjectPool2 p = m_pool;
        List<TestObject> objs = new LinkedList<TestObject>();
        for (int i = 0; i < 10000; i++) {
            Object obj = p.getObject(10000);
            objs.add((TestObject)obj);
        }
        TimeElapsed te = new TimeElapsed();
        for (int i = 0; i < 1000000; i++) {
            Object obj = p.getObject(10000);
            p.releaseObject(obj);
        }
        System.out.println("Time = " + te.elapsed());
        //LineReader lr = new LineReader(new InputStreamReader(System.in));
        //lr.readLine();
        for (TestObject po: objs) {
            p.releaseObject(po);
        }
        p.close();
    }

    public static void main1(String[] args) throws Exception {
        PerformanceTest mt = new PerformanceTest();
        mt.setUp();
        try {
            mt.test();
        } catch (Exception e) {
            mt.tearDown();
            throw e;
        }
    }
}
