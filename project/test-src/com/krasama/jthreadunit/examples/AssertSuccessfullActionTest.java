package com.krasama.jthreadunit.examples;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import com.krasama.jthreadunit.TestThread;
import com.krasama.jthreadunit.TestThreadGroup;

/**
 * User: Alexander Stepochkin
 * Date: 18.05.2005
 * Time: 17:59:28
 */
public class AssertSuccessfullActionTest extends TestCase {
    public static class ThrowExceptionThread extends TestThread {
        public ThrowExceptionThread(TestThreadGroup group, String name) {
            super(group, name);
        }

        public void doThrowException() throws Exception {
            throw new Exception("GenericProbabilisticTest Exception");
        }
    }

    public void testExceptionInAction() throws InterruptedException {
        TestThreadGroup group = new TestThreadGroup("ThrowException");
        ThrowExceptionThread thread1 = new ThrowExceptionThread(group, "thread1");
        thread1.start();
        thread1.performAction("ThrowException");
        boolean errorThrown = false;
        try {
            thread1.assertSuccessful();
        } catch(AssertionFailedError e) {
            errorThrown = true;
        }
        if (!errorThrown) {
            assertTrue("Action error expected", false);
        }
        group.stopTest();
    }
}
