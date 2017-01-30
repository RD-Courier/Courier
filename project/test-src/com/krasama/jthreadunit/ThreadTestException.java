package com.krasama.jthreadunit;

import junit.framework.AssertionFailedError;

/**
 * User: Alexander Stepochkin
 * Date: 19.05.2005
 * Time: 15:42:33
 */
public class ThreadTestException extends AssertionFailedError {
    private final TestThreadGroup m_threadGroup;

    ThreadTestException(TestThreadGroup threadGroup) {
        m_threadGroup = threadGroup;
        if (m_threadGroup.getErrorCount() > 0) {
            initCause(m_threadGroup.getErrorThread(0).getFrameworkError());
        }
    }

    public String getMessage() {
        return "Thread test framework exception in group '" + m_threadGroup.getName() +
            "' caused by threads: " + m_threadGroup.getErrorThreadsNames() +
            ": see first cause below";
    }
}
