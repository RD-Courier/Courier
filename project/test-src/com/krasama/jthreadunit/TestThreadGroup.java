package com.krasama.jthreadunit;

import java.util.LinkedList;
import java.util.List;

/**
 * User: Alexander Stepochkin
 * Date: 19.05.2005
 * Time: 15:01:06
 */
public class TestThreadGroup extends ThreadGroup {
    private List<TestThread> m_errorThreads = new LinkedList<TestThread>();

    public TestThreadGroup(String name) {
        super(name);
    }

    synchronized void addErrorThread(TestThread errorThread) {
        m_errorThreads.add(errorThread);
    }

    synchronized int getErrorCount() {
        return m_errorThreads.size();
    }

    synchronized TestThread[] getErrorThreads() {
        return (TestThread[])m_errorThreads.toArray();
    }

    synchronized TestThread getErrorThread(int index) {
        return m_errorThreads.get(index);
    }

    synchronized String getErrorThreadsNames() {
        StringBuffer ret = new StringBuffer();
        for (TestThread thread: m_errorThreads) {
            if (ret.length() > 0) ret.append(", ");
            ret.append("'" + thread.getName() + "'");
        }
        return ret.toString();
    }

    private TestThread[] getActiveThreads() {
        TestThread[] threads = new TestThread[activeCount()];
        enumerate(threads);
        return threads;
    }

    public synchronized void stopTest() {
        for (TestThread thread: getActiveThreads()) {
            thread.kill(); // i assume that stopTest does not throw exception
        }
        checkErrors();
    }

    synchronized void checkErrors() throws RuntimeException {
        if (getErrorCount() > 0) throw new ThreadTestException(this);
    }
}
