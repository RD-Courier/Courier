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

import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.logging.test.NullLogger;
import ru.rd.test.TestCourierLogger;

import java.util.LinkedList;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 26.05.2005
 * Time: 13:20:03
 */
public class CommonTest extends FileDataTestCase {
    public void testGeneric() throws PoolException, InterruptedException {
        final int incCapacity = 3;
        final int expandCount = 3;
        final int totalCount = expandCount * incCapacity;
        final int checkFailBout = 2;
        final int checkFailObjectCount = incCapacity;
        final int shrinkCapacity = 2;

        TestObjectFactory of = new TestObjectFactory(1, 1, -1){
            protected TestObject createObject(final int number) {
                return new TestObject(
                    new TestObject.Checker() {
                        public void testCheck(int count) {
                            if (number < checkFailObjectCount && count >= checkFailBout) {
                                throw new RuntimeException("Object check failed");
                            }
                        }

                        public void testClose() {
                        }
                    }
                );
            }
        };
        final ObjectPool pool = new SynchObjectPool(
            "Pool test",
            new TestCourierLogger(),
            of,
            0, // initialCapacity
            incCapacity, // incCapacity
            -1, // maxCapacity
            -1, // shrinkInterval
            shrinkCapacity, // shrinkCapacity
            1, // shrinkObjPeriod
            -1 // checkInterval
        );
        pool.start();
        assertEquals(0, pool.size());
        List<TestObject> objs = new LinkedList<TestObject>();
        for (int expandCounter = 0; expandCounter < expandCount; expandCounter++) {
            for (int getCounter = 0; getCounter < incCapacity; getCounter++) {
                objs.add((TestObject)pool.getObject());
                assertEquals((expandCounter + 1) * incCapacity, pool.size());
            }
        }
        assertEquals(0, pool.freeCount());
        int i = 1;
        for (TestObject to: objs) {
            pool.releaseObject(to);
            assertEquals(i, pool.freeCount());
            i++;
        }
        assertEquals(totalCount, pool.size());
        pool.check();
        assertEquals(totalCount, pool.size());
        assertEquals(pool.size(), pool.freeCount());
        pool.check();
        assertEquals(totalCount, pool.size());
        assertEquals(pool.size(), pool.freeCount());
        Thread.sleep(2);
        for (i = 0; i < 3; i++) {
            pool.shrink();
            assertEquals(totalCount - shrinkCapacity * (i+1), pool.size());
            assertEquals(pool.size(), pool.freeCount());
        }
        objs.get(0).setFailOnClose(true);
        pool.close();
        assertEquals(0, pool.size());
        of.checkResults();
    }

    public void testMaxCapacity() throws PoolException, InterruptedException {
        final int incCapacity = 3;
        final int expandCount = 3;
        final int totalCount = expandCount * incCapacity;
        final int maxCapacity = totalCount;

        TestObjectFactory of = new TestObjectFactory();
        final ObjectPool pool = new SynchObjectPool(
                "Pool test",
                //ConsoleCourierLogger.instance(),
                new NullLogger(),
                of,
                0, // initialCapacity
                incCapacity, // incCapacity
                maxCapacity, // maxCapacity
                -1, // shrinkInterval
                -1, // shrinkCapacity
                -1, // shrinkObjPeriod
                -1 // checkInterval
        );
        pool.start();
        List<TestObject> objs = new LinkedList<TestObject>();
        for (int i = 0; i < totalCount; i++) {
            objs.add((TestObject)pool.getObject());
        }
        assertEquals(totalCount, pool.size());
        assertEquals(0, pool.freeCount());
        boolean errorThrown = false;
        try {
            objs.add((TestObject)pool.getObject());
        } catch (Exception e) {
            errorThrown = true;
        }
        assertTrue("Max capacity reached exception not thrown", errorThrown);
        pool.close();
        assertEquals(0, pool.size());
        of.checkResults();
    }

    public void testExpireOnGet() throws PoolException, InterruptedException {
        TestObjectFactory of = new TestObjectFactory();
        final ObjectPool pool = new SynchObjectPool(
                "Pool test",
                //ConsoleCourierLogger.instance(),
                new TestCourierLogger(),
                of,
                 0, // initialCapacity
                 1, // incCapacity
                -1, // maxCapacity
                -1, // shrinkInterval
                -1, // shrinkCapacity
                -1, // shrinkObjPeriod
                -1, // checkInterval
                 5  // expirePeriod
        );
        pool.start();
        List<TestObject> objs = new LinkedList<TestObject>();
        objs.add((TestObject)pool.getObject());
        assertEquals(1, pool.size());
        assertEquals(0, pool.freeCount());
        assertEquals(1, of.getAllocatedCount());

        Thread.sleep(6);
        pool.releaseObject(objs.get(0));
        objs.add((TestObject)pool.getObject());
        assertEquals(2, pool.size());
        assertEquals(1, pool.freeCount());
        assertEquals(2, of.getAllocatedCount());

        pool.close();
        assertEquals(0, pool.size());
        of.checkResults();
    }

}
