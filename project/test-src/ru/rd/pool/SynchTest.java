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

import ru.rd.test.TestCourierLogger;
import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.utils.StringHelper;
import ru.rd.thread.WorkThread;
import ru.rd.thread.ThreadFactory;
import ru.rd.test.ErrorInfo;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

/**
 * User: AStepochkin
 * Date: 25.07.2005
 * Time: 15:46:28
 */
public class SynchTest extends FileDataTestCase {
    private List<ErrorInfo> m_errors = new LinkedList<ErrorInfo>();
    private List<TestObject> m_objs = new LinkedList<TestObject>();
    private TestCourierLogger m_logger = new TestCourierLogger(false);
    private TestObjectFactory m_of;
    private ObjectPool m_pool;

    protected void courierSetUp() {
    }

    protected void courierTearDown() throws PoolException {
        m_pool.close();
        assertEquals(0, m_pool.size());
        if (m_logger.getErrors().size() > 0) {
            fail(StringHelper.stringListToXml(m_logger.getErrors(), "error"));
        }
        m_of.checkResults();
        if (m_errors.size() > 0) {
            fail(StringHelper.stringListToXml(m_errors, "error"));
        }
    }

    private void addError(String message, Exception e) {
        m_logger.error(message, e);
    }

    private void addError(Exception e) {
        addError(null, e);
    }

    private void addError(String message) {
        addError(message, null);
    }

    private void initTest(TestObjectFactory of) throws PoolException {
        m_objs.clear();
        m_of = of;
        SynchObjectPool threadPool = new SynchObjectPool(
            "thread pool", m_logger, new ThreadFactory(m_logger, "Test")
        );
        threadPool.start();
        m_pool = new AsynchObjectPool(
            "Test pool", m_logger, threadPool, of
        );
    }

    private void startTest() throws PoolException {
        m_pool.start();
    }

    private TestObject getObject() {
        TestObject o = (TestObject)m_pool.getObject();
        m_objs.add(o);
        return o;
    }

    private void getObjects(int count) {
        for (int i = 0; i < count; i++) {
            getObject();
        }
    }

    private void releaseObject(int index) {
        assertTrue(index < m_objs.size());
        m_pool.releaseObject(m_objs.remove(index));
    }

    private void releaseObjects(int beginIndex, int count) {
        for (int i = 0; i < count; i++) {
            releaseObject(beginIndex);
        }
    }

    private void releaseObjects(int count) {
        releaseObjects(0, count);
    }
    private void checkPool(int expSize, int expFreeCount, int expAllocCount) {
        assertEquals(expSize, m_pool.size());
        assertEquals(expFreeCount, m_pool.freeCount());
        assertEquals(expAllocCount, m_of.getAllocatedCount());
    }

    public void test() throws PoolException, InterruptedException {
        final int objectCount = 1;
        TestObjectFactory of = new TestObjectFactory(0, 200, -1);
        initTest(of);
        m_pool.setCapacityPars(objectCount, 1, -1);
        startTest();
        //getObjects(objectCount);
        //checkPool(objectCount, 0, objectCount);
        //releaseObjects(objectCount);
        checkPool(objectCount, objectCount, objectCount);
        WorkThread thread = new WorkThread(m_logger, "test");
        final CountDownLatch checkDoneSignal = new CountDownLatch(1);
        thread.launchWork(new Runnable() {
            public void run() {
                try {
                    m_pool.check();
                } catch (Exception e) {
                    addError(e);
                }
                checkDoneSignal.countDown();
            }
        });
        Thread.sleep(20);
        getObject();
        checkDoneSignal.await();
        thread.close();
    }
}
