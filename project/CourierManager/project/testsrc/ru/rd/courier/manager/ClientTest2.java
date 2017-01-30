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

import ru.rd.TestUtils;
import ru.rd.thread.PoolExecutorAdapter;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.manager.message.CourierInfoMessage;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.courier.stat.StatProcessingTest;
import ru.rd.courier.utils.ErrorHelper;
import ru.rd.courier.utils.FileHelper;
import ru.rd.pool.ObjectPoolIntf;

import java.util.Timer;
import java.io.File;
import java.io.IOException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SimpleByteBufferAllocator;

/**
 * User: AStepochkin
 * Date: 03.10.2008
 * Time: 20:12:50
 */
public class ClientTest2 {
    public static void main(String[] args) throws Exception {
        Timer timer = new Timer("Timer-ClientTest2");
        CourierLogger logger = TestUtils.getTestLogger("ClientTest2.log");
        ObjectPoolIntf threadPool = PoolExecutorAdapter.createThreadPool2(
            logger, "ThreadPool-ClientTest2"
        );
        threadPool.start();

        CourierInfoMessage courier = new CourierInfoMessage("MyTestConfig.xml");
        ManagerClient client = new ManagerClient(
            logger, threadPool, timer,
            "10.30.1.75", 4444, courier
        );
        client.setTimeout(1000);
        client.setCheckInterval(1000);
        //client.setTimer(timer);
        client.start();
        client.shouldBeStarted();
        Thread.sleep(500);
        logger.info("Start sending resuts ...");
        for (int i = 0; i < 10000; i++) {
            ProcessResult pr = StatProcessingTest.createResult();
            client.sendStat(pr);
            if (i % 100 == 0) {
                logger.info("Sending resuts " + i + " ID = " + pr.getId() + " ...");
                //showThreads(null);
            }
            //Thread.sleep(1000);
        }
        logger.info("Stop sending resuts ...");

        Thread.sleep(100);
        logger.info("Sleeping ...");
        Thread.sleep(20000);
        //showThreads(null);
        logger.info("Stopping test ...");
        client.stop();
        Thread.sleep(3000);
        //showThreads(null);
        timer.cancel();
        threadPool.close();
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        Thread.sleep(500);
        String st = ErrorHelper.stackTracesToString(Thread.currentThread().getThreadGroup());
        showThreads(null);
        System.exit(0);
    }

    private static void showThreads(String fileName) throws IOException {
        String st = ErrorHelper.stackTracesToString(Thread.currentThread().getThreadGroup());
        System.out.println(st);
        if (fileName != null) {
            FileHelper.stringToFile(st, new File(fileName));
        }
    }
}
