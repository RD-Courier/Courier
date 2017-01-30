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

/**
 * User: STEPOCHKIN
 * Date: 10.09.2008
 * Time: 17:52:34
 */

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.TestObject;
import ru.rd.pool.TestObjectFactory;
import ru.rd.test.LaunchAtOnceTestCase;
import ru.rd.test.LaunchAtSignalThread;
import ru.rd.test.TestThreadFactory;
import ru.rd.utils.TimeElapsed;

public class GenericProbabilisticTest extends Test {

    protected PoolObjectFactory createObjectFactory(CourierLogger logger) {
        return new TestObjectFactory(10L, 10L, -1) {
            protected TestObject createObject(int number) {
                return new TestObject(new TestObject.Checker() {
                    public void testCheck(int count) {
                        if (count > 2) throw new RuntimeException();
                    }

                    public void testClose() {}
                });
            }
        };
    }

    public void testGeneral() throws InterruptedException {
        final boolean c_needToLog = false;
        final int c_threadNumber = 16;

        TimeElapsed tte = new TimeElapsed();
        LaunchAtOnceTestCase.launchAllTest(
            c_needToLog, c_threadNumber,
            new TestThreadFactory() {
                public LaunchAtSignalThread create() {
                    return new LaunchAtSignalThread() {
                        protected void runTest() throws Exception {
                            TimeElapsed te = new TimeElapsed();
                            for(int i = 0; i < 100; i++) {
                                Object resource = m_pool.getObject(1000);
                                m_pool.releaseObject(resource);
                            }
                            slog("Time = " + te.elapsed());
                        }
                    };
                }
            },
            100, 600*1000
        );
        TestObjectFactory of = (TestObjectFactory)m_pool.getObjectFactory();
        System.out.println("TotalTime = " + tte.elapsed() + " Count = " + of.getAllocatedCount());

        //System.out.println(m_pool.toString());
        //assertEquals(initialCapacity, pool.size());
        //assertEquals(pool.size(), pool.freeCount());
        //assertEquals(pool.size(), pool.freeCount());
        m_pool.close();
        assertEquals("There are improperly closed objects", 0, of.getWrongClosedCount());
    }

}
