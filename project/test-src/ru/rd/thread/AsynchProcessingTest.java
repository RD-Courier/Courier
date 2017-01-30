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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.CourierLoggerAdapter;
import ru.rd.courier.logging.SimpleFormatter;
import ru.rd.pool.ObjectPoolIntf;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: STEPOCHKIN
 * Date: 14.09.2008
 * Time: 14:33:25
 */
public class AsynchProcessingTest {
    private static class MockTarget {
        private int m_id;
        private int m_processedCount = 0;

        public MockTarget(int id) {
            m_id = id;
        }

        public int getId() {
            return m_id;
        }

        public int getProcessedCount() {
            return m_processedCount;
        }

        public void setProcessed() {
            m_processedCount++;
        }

        public String toString() {
            return Integer.toString(m_id);
        }
    }

    private static class MockResource {
        private final List<MockTarget> m_targets = new LinkedList<MockTarget>();

        public void process(MockTarget target) throws InterruptedException {
            Thread.sleep(5);
            target.setProcessed();
            m_targets.add(target);
        }

        public List<MockTarget> getTargets() {
            return m_targets;
        }
    }

    private static class MockProcessing extends AsynchProcessing<MockResource, MockTarget> {
        public MockProcessing(CourierLogger logger, Executor exec, int maxExec) {
            super(logger, exec, maxExec);
        }

        protected MockResource findResource() {
            slog("findResource");
            return new MockResource();
        }

        protected boolean isResourceValid(MockResource resource) throws Exception {
            slog("isResourceValid");
            return true;
        }

        protected void releaseResource(MockResource resource) throws Exception {
            slog("releaseResource");
        }

        protected void releaseCancelledResource(MockResource resource) throws Exception {
            slog("releaseCancelledResource");
        }

        protected void process(MockResource resource, MockTarget target) throws Exception {
            resource.process(target);
        }
    }

    private static void slog(String m) {
        System.out.println(System.currentTimeMillis() + " - " + m);
    }

    public static void main(String[] args) throws Exception {
        Logger rlog = Logger.getLogger("");
        for (Handler h: rlog.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleFormatter("{0,time,mm:ss.SSS}"));
        }
        //rlog.addHandler(new ConsoleHandler());
        CourierLogger logger = new CourierLoggerAdapter(rlog);
        //CourierLogger logger = new ConsoleCourierLogger("");
        ObjectPoolIntf threadPool = PoolExecutorAdapter.createThreadPool2(logger, "ThreadPool");
        threadPool.start();

        MockProcessing ap = new MockProcessing(logger, new PoolExecutorAdapter(threadPool), 10);
        ap.setCheckInterval(3000);
        final List<MockTarget> targets = new LinkedList<MockTarget>();

        for (int i = 0; i < 10000; i++) {
            MockTarget t = new MockTarget(i);
            targets.add(t);
            ap.addTarget(t);
        }

        Thread.sleep(20000);

        slog("Before ap.close");
        boolean closed = ap.close(30000);
        slog("ProcessingClosed = " + closed);

        slog("Before threadPool.close");
        threadPool.close();

        slog("Before check");
        int notCount = 0;
        for (MockTarget t: targets) {
            if (t.getProcessedCount() != 1) notCount++;
        }

        slog("NotCount = " + notCount);

        slog("End");

        //ThreadGroup tg = Thread.currentThread().getThreadGroup();
        //String stackTraces = ErrorHelper.stackTracesToString(tg, false);
        //slog(stackTraces);

    }
}
