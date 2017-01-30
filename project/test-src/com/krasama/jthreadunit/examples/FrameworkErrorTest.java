package com.krasama.jthreadunit.examples;

import junit.framework.TestCase;
import com.krasama.jthreadunit.TestThread;
import com.krasama.jthreadunit.TestThreadGroup;
import com.krasama.jthreadunit.ThreadTestException;

/**
 * User: Alexander Stepochkin
 * Date: 19.05.2005
 * Time: 18:40:22
 */

public class FrameworkErrorTest extends TestCase {
    public static class ThrowExceptionThread extends TestThread {
        public ThrowExceptionThread(TestThreadGroup group, String name) {
            super(group, name);
        }

        public void doThrowException() throws Exception {
        }
    }

    public void testExceptionInAction() throws InterruptedException {
        TestThreadGroup group = new TestThreadGroup("ThrowException");
        ThrowExceptionThread thread1 = new ThrowExceptionThread(group, "thread1");
        thread1.start();
        boolean errorThrown = false;
        try {
            thread1.performAction("ThrowException");
        } catch(ThreadTestException e) {
            errorThrown = true;
        }
        try {
            group.stopTest();
        } catch(ThreadTestException e) {
        }
        if (!errorThrown) {
            assertTrue("Framework error expected", false);
        }
    }
}

