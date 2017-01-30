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

import junit.framework.TestCase;

import java.util.Collection;
import java.util.LinkedList;

public class TestObjectFactory extends TestObjectValues implements PoolObjectFactory {
    private final Collection<TestObject> m_objects = new LinkedList<TestObject>();
    private long m_getSleepInterval;
    private long m_checkSleepInterval;
    private int m_successfulGetCount;
    private int m_getCounter = 0;

    public TestObjectFactory(
        long getSleepInterval, long checkSleepInterval, int successfulGetCount
    ) {
        m_getSleepInterval = getSleepInterval;
        m_checkSleepInterval = checkSleepInterval;
        m_successfulGetCount = successfulGetCount;
    }

    public TestObjectFactory() {
        this(10L, 50L, -1);
    }

    protected TestObject createObject(int number) {
        return new TestObject(number);
    }

    public Object getObject(ObjectPoolIntf pool) {
        if (m_getSleepInterval > 0) {
            try {
                Thread.sleep(m_getSleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        synchronized(m_objects) {
            if (m_successfulGetCount == 0) throw new RuntimeException("Get object failed");
        }
        TestObject o = createObject(m_getCounter);

        synchronized(m_objects) {
            if (m_successfulGetCount > 0) m_successfulGetCount--;
            m_objects.add(o);
            m_getCounter++;
        }
        return o;
    }

    public void returnObject(final Object o) {
        ((TestObject)o).close();
    }

    public boolean checkObject(final Object o) {
        boolean res = ((TestObject)o).isValid();
        if (m_checkSleepInterval > 0) {
            try {
                Thread.sleep(m_checkSleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public int getSuccessfulGetCount() {
        return m_successfulGetCount;
    }

    public void setSuccessfulGetCount(int value) {
        m_successfulGetCount = value;
    }

    public void incSuccessfulGetCount(int increment) {
        m_successfulGetCount += increment;
    }

    public void setGetSleepInterval(int value) {
        m_getSleepInterval = value;
    }

    public void setCheckSleepInterval(int value) {
        m_checkSleepInterval = value;
    }

    public int getAllocatedCount() {
        return m_objects.size();
    }

    public int getWrongClosedCount() {
        int res = 0;
        for (TestObject o: m_objects) {
            if (o.getCloseCount() != 1) res++;
        }
        return res;
    }

    public int getClosedCount() {
        int res = 0;
        for (TestObject o: m_objects) {
            if (o.getCloseCount() == 1) res++;
        }
        return res;
    }

    public int getOpenedCount() {
        int res = 0;
        for (TestObject o: m_objects) {
            if (o.getCloseCount() == 0) res++;
        }
        return res;
    }

    public void checkResults() {
        TestCase.assertEquals(0, getOpenedCount());
        TestCase.assertEquals(0, getWrongClosedCount());
    }
}
