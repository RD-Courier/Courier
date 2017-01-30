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

import java.util.concurrent.CountDownLatch;

/**
 * User: AStepochkin
 * Date: 18.05.2005
 * Time: 15:16:51
 */
public abstract class LaunchAtSignalThread extends TestThread {
    protected int m_number;
    private CountDownLatch m_readyToStartSignal;
    private CountDownLatch m_startSignal;
    private CountDownLatch m_doneSignal;

    protected abstract void runTest() throws Exception;

    public LaunchAtSignalThread() {
        super();
    }

    void init(
        String desc, boolean loggable, int number,
        CountDownLatch readyToStartSignal, CountDownLatch waitObj, CountDownLatch doneSignal
    ) {
        super.init(desc, loggable);
        m_number = number;
        m_readyToStartSignal = readyToStartSignal;
        m_startSignal = waitObj;
        m_doneSignal = doneSignal;
    }

    public final void testRun() throws Exception {
        log("waiting");
        m_readyToStartSignal.countDown();
        m_startSignal.await();
        log("started");
        runTest();
        log("finished");
        m_doneSignal.countDown();
    }
}
