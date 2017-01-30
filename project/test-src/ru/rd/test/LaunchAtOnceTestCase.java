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
package ru.rd.test;

import junit.framework.TestCase;
import ru.rd.test.TestThread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 26.05.2005
 * Time: 10:17:58
 */
public class LaunchAtOnceTestCase extends TestCase {

    public static void launchAllTest(
        List<? extends Runnable> works,
        long initThreadTimeout, long doneThreadTimeout
    ) throws InterruptedException {
        CountDownLatch readyToStartSignal = new CountDownLatch(works.size());
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(works.size());

        List<LaunchAtSignalWork> ws = new LinkedList<LaunchAtSignalWork>();

        for (Runnable work: works) {
            LaunchAtSignalWork lsw = new LaunchAtSignalWork(
                work, readyToStartSignal, startSignal, doneSignal
            );
            ws.add(lsw);
            Thread t = new Thread(lsw);
            t.start();
        }
        if (!readyToStartSignal.await(works.size() * initThreadTimeout, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("ReadyToStart time out");
        }
        startSignal.countDown();
        if (!doneSignal.await(works.size() * doneThreadTimeout, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Done time out");
        }

        for (LaunchAtSignalWork w: ws) {
            if (w.getError() != null) {
                throw new RuntimeException(w.getError());
            }
        }
    }

    public static void launchAllTest(
        boolean needToLog, int threadNumber, TestThreadFactory testerFactory,
        long initThreadTimeout, long doneThreadTimeout
    ) throws InterruptedException {
        CountDownLatch readyToStartSignal = new CountDownLatch(threadNumber);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threadNumber);

        List<TestThread> threads = new LinkedList<TestThread>();

        for (int i = 0; i < threadNumber; i++) {
            final LaunchAtSignalThread t = testerFactory.create();
            t.init(
                Integer.toString(i), needToLog, i,
                readyToStartSignal, startSignal, doneSignal
            );
            threads.add(t);
            t.start();
        }
        if (!readyToStartSignal.await(threadNumber * initThreadTimeout, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("ReadyToStart time out");
        }
        startSignal.countDown();
        if (!doneSignal.await(threadNumber * doneThreadTimeout, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Done time out");
        }

        for (TestThread thread: threads) {
            if (thread.hasError()) {
                throw new RuntimeException(thread.getError());
            }
        }
    }
}
