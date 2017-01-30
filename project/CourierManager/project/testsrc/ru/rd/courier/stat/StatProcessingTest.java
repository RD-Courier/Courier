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
package ru.rd.courier.stat;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SimpleByteBufferAllocator;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.CourierLoggerAdapter;
import ru.rd.courier.logging.SimpleFormatter;
import ru.rd.courier.logging.test.NullLogger;
import ru.rd.courier.manager.StatProcessing;
import ru.rd.courier.manager.message.CourierInfoMessage;
import ru.rd.courier.manager.message.ManagerInfoMessage;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.courier.utils.ErrorHelper;
import ru.rd.net.synch.SynchClient;
import ru.rd.net.synch.SynchProtocolCodecFactory;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.PoolExecutorAdapter;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: STEPOCHKIN
 * Date: 15.09.2008
 * Time: 2:58:15
 */
public class StatProcessingTest {
    private static long s_processId = 1;
    public static ProcessResult createResult() {
        ProcessResult pr = new ProcessResult();
        pr.setId(s_processId++);
        pr.setPipe("TestPipe");
        pr.setSourceDbName("TestSource");
        pr.setTargetDbName("TestTarget");
        pr.setErrorCount(1);
        pr.setError("TestError");
        pr.setSourceTime(4444);
        pr.setTargetTime(8888);
        return pr;
    }

    public static void main(String[] args) throws Exception {
        Logger rlog = Logger.getLogger("");
        rlog.setLevel(Level.ALL);
        for (Handler h: rlog.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleFormatter("{0,time,mm:ss.SSS}"));
        }
        final CourierLogger logger = new CourierLoggerAdapter(rlog);

        ObjectPoolIntf m_threadPool = PoolExecutorAdapter.createThreadPool(new NullLogger(), "ThreadPool");
        m_threadPool.start();

        SynchProtocolCodecFactory<Object, Object> mainCodec = StatProcessing.buildMainCodecFactory();
        SynchClient<Object, Object> client = new SynchClient<Object, Object>(
            "127.0.0.1", 4444, mainCodec.getEncoder(), mainCodec.getDecoder()
        );
        ManagerInfoMessage mim = (ManagerInfoMessage)client.write(new CourierInfoMessage("MyTestConfig.xml"));

        StatProcessing cp = new StatProcessing(
            logger, new PoolExecutorAdapter(m_threadPool), 1, "127.0.0.1"
        ) {
            protected void processedProcessResult(ProcessResult message) {
                if (message == null) return;
                logger.debug("Processed: " + message);
            }
        };
        cp.setCheckInterval(1000);
        cp.setLastExecSleepInterval(1000);
        cp.setManagerInfo(mim);

        for (int i = 0; i < 10; i++) {
            cp.addTarget(createResult());
        }
        Thread.sleep(3000);
        for (int i = 0; i < 10; i++) {
            cp.addTarget(createResult());
        }
        Thread.sleep(3000);
        for (int i = 0; i < 10; i++) {
            cp.addTarget(createResult());
        }

        logger.debug("StatProcessing: before cp close");
        cp.close(3000);
        logger.debug("StatProcessing: after cp close");

        m_threadPool.close();

        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());

        for (;;) {
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            System.out.println(ErrorHelper.stackTracesToString(tg, false));
            if (tg.activeCount() <= 1) break;
            Thread.sleep(1000);
        }
    }
}
