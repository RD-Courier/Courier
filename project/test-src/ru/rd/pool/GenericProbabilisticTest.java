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

import ru.rd.test.LaunchAtOnceTestCase;
import ru.rd.test.LaunchAtSignalThread;
import ru.rd.test.TestCourierLogger;
import ru.rd.test.TestThreadFactory;

public class GenericProbabilisticTest extends LaunchAtOnceTestCase {

    public void testGeneral() throws InterruptedException, PoolException {
        final int threadNumber = 20;
        final int initialCapacity = 0;
        TestObjectFactory of = new TestObjectFactory();
        final ObjectPool pool = new SynchObjectPool(
                "Pool test",
                new TestCourierLogger(),
                //ConsoleCourierLogger.instance(),
                of,
                initialCapacity, // initialCapacity
                5, // incCapacity
                -1, // maxCapacity
                -1, // shrinkInterval
                2, // shrinkCapacity
                10000, // shrinkObjPeriod
                -1 // checkInterval
        );
        pool.start();

        launchAllTest(
            false, threadNumber,
            new TestThreadFactory() {
                public LaunchAtSignalThread create() {
                    return new LaunchAtSignalThread() {
                        protected void runTest() throws Exception {
                            long sleepInterval = (m_number % 7) * 5;
                            for(int i = 0; i < 2; i++) {
                                Object resource;
                                resource = pool.getObject();
                                if (resource != null) {
                                    log("sleeping");
                                    Thread.sleep(sleepInterval);
                                    log("woken up");
                                    pool.releaseObject(resource);
                                }
                            }
                        }
                    };
                }
            },
            100, 60*1000
        );

        //System.out.println(pool.toString());
        //assertEquals(initialCapacity, pool.size());
        assertEquals(pool.size(), pool.freeCount());
        pool.close();
        assertEquals(pool.size(), pool.freeCount());
        assertEquals("There are improperly closed objects", 0, of.getWrongClosedCount());
    }
}
