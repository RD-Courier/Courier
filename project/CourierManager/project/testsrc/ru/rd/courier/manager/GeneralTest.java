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
package ru.rd.courier.manager;

import ru.rd.courier.logging.ConsoleCourierLogger;
import ru.rd.courier.logging.ConsoleHandlerEx;
import ru.rd.courier.logging.SimpleFormatter;
import ru.rd.courier.manager.message.CourierInfoMessage;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.courier.stat.StatProcessingTest;
import ru.rd.net.ServerContext;
import ru.rd.net.message.CommonAnswer;
import ru.rd.thread.Condition;
import ru.rd.thread.PoolExecutorAdapter;
import ru.rd.thread.ThreadHelper;
import ru.rd.utils.Disposable;
import ru.rd.utils.LogHelper;
import ru.rd.utils.SysUtils;
import ru.rd.test.LaunchAtOnceTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 16.10.2008
 * Time: 12:33:03
 */
public class GeneralTest {
    public static class TestLogProvider implements LogProvider {
        private final LoggerAdapter m_logger;

        public TestLogProvider() {
            m_logger = new LoggerAdapter(null, "", true);
            Logger logger = m_logger.getInnerLogger();
            LogHelper.clearHandlers(logger);
            Handler h = new ConsoleHandlerEx(System.out);
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleFormatter("{0,time}"));
            logger.addHandler(h);
        }

        public LoggerAdapter getLogger(String name) {
            return m_logger;
        }
    }

    public static class TestServerContext extends DefaultServerContext {
        public TestServerContext() {
            super();
            setTimer(new Timer("Test-Timer"));
            setLogProvider(new TestLogProvider());
            setThreadPool(PoolExecutorAdapter.createThreadPool2(getLogger(null), "TestThreadPool"));
            getThreadPool().start();
            setDebugMode(true);
        }
    }

    private static class ClientTestRunnable implements Runnable, Disposable {
        private final ServerContext m_ctx;
        private final Object m_lock = new Object();
        private final ManagerClient m_client;
        private final long m_prTimeout = 1000;
        private final int m_sendCount;
        private int m_sentCount;
        private boolean m_waiting = false;

        public ClientTestRunnable(ServerContext ctx, CourierInfoMessage courier, int sendCount) throws NoSuchMethodException, InterruptedException {
            m_sendCount = sendCount;
            m_ctx = ctx;
            m_client = new ManagerClient(
                ctx.getLogger(null), ctx.getThreadPool(), ctx.getTimer(),
                "127.0.0.1", 4444, courier
            ){
                protected void processedProcessResult(ProcessResult message) {
                    //debug("processedProcessResult");
                    synchronized(m_lock) {
                        m_sentCount--;
                        if (m_sentCount == 0) {
                            //debug(this + ": SentCount = 0 " + m_waiting);
                            m_lock.notifyAll();
                        }
                    }
                }
            };
            m_client.setTimeout(2000);
            m_client.setCheckInterval(1000);
            m_client.start();
            m_client.setShouldBeStarted(true);
            //Thread.sleep(10);
        }

        @SuppressWarnings({"UNUSED_SYMBOL"})
        private void debug(String message) {
            m_ctx.getLogger(null).debug(message);
        }

        public int getSentCount() {
            synchronized(m_lock) {
                return m_sentCount;
            }
        }

        public ProcessResult createResult() {
            return StatProcessingTest.createResult();
        }

        public void run() {
            synchronized(m_lock) {
                m_sentCount = m_sendCount;
            }
            for (int i = 0; i < m_sendCount; i++) {
                m_client.sendStat(createResult());
            }
            long timeout = m_sentCount * m_prTimeout;
            m_waiting = true;
            boolean waitres = ThreadHelper.waitEvent(m_lock, new Condition() {
                public boolean isTrue() {
                    //debug("WaitCondition: SentCount = " + m_sentCount);
                    return m_sentCount == 0;
                }
            }, timeout);
            m_waiting = false;
            if (!waitres) {
                //debug("ClientTestRunnable.run: timeout (" + timeout + ") expired");
                throw new RuntimeException(m_client + ": Timeout " + timeout + " expired");
            }
        }

        public void dispose() {
            m_client.dispose();
        }

        public String toString() {
            return m_client.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerContext ctx = new TestServerContext();
        try {
            List<ClientTestRunnable> rs = new LinkedList<ClientTestRunnable>();
            try {
                for (int i = 0; i < 40; i++) {
                    rs.add(new ClientTestRunnable(ctx, new CourierInfoMessage("TestCourier" + i), 20));
                }
                LaunchAtOnceTestCase.launchAllTest(rs, 1000, 10000);
            } finally {
                int totalLeft = 0;
                for (ClientTestRunnable r: rs) {
                    System.out.println(r + ": left " + r.getSentCount());
                    totalLeft += r.getSentCount();
                    SysUtils.dispose(r, ctx.getLogger(null));
                }
                System.out.println("Total left = " + totalLeft);
                //ErrorHelper.showThreads(null);
            }
        } finally {
            SysUtils.dispose(ctx, new ConsoleCourierLogger(""));
        }
    }
}
