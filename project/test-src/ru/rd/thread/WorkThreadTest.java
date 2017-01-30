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
package ru.rd.thread;

import junit.framework.TestCase;
import ru.rd.courier.logging.ConsoleCourierLogger;

import java.util.Collection;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 28.06.2005
 * Time: 15:03:37
 */
public class WorkThreadTest extends TestCase {
    private WorkThread m_thread;
    private Collection<WaitRunnable> m_waitWorks = new LinkedList<WaitRunnable>();

    protected void setUp() throws Exception {
        m_thread = new WorkThread(new ConsoleCourierLogger("test"), "test");
    }

    protected void tearDown() throws Exception {
        for (WaitRunnable wr: m_waitWorks) {
            wr.stop();
            System.out.println("WaitRunnable stopped");
        }
        m_thread.close();
        System.out.println("Thread stopped");            
        m_thread = null;
    }

    private void launchWaitWork(long workTimeout) {
        WaitRunnable wr = new WaitRunnable(workTimeout);
        m_waitWorks.add(wr);
        m_thread.launchWork(wr);
    }

    private boolean launchWaitWorkWait(long workTimeout, long timeout) {
        WaitRunnable wr = new WaitRunnable(workTimeout);
        m_waitWorks.add(wr);
        return m_thread.launchWorkAndWait(wr, timeout);
    }

    public void testWait() throws InterruptedException {
        boolean succeeded;

        succeeded = m_thread.launchWorkAndWait(
            new Runnable() { public void run() {} }, 100
        );
        assertTrue(succeeded);

        succeeded = launchWaitWorkWait(10, 100);
        assertTrue(succeeded);

        succeeded = launchWaitWorkWait(100, 10);
        assertTrue(!succeeded);
    }

    public void testBusy() {
        launchWaitWork(0);
        boolean exceptionThrown = false;
        try {
            launchWaitWork(0);
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        System.out.println("testBusy end");
    }
}