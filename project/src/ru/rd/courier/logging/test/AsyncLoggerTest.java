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
package ru.rd.courier.logging.test;

import ru.rd.courier.logging.ConsoleCourierLogger;
import ru.rd.courier.logging.data.AsyncDataLogger;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.logging.data.DaysFileLog;
import ru.rd.thread.WorkThread;
import ru.rd.courier.utils.CloseEventListener;
import ru.rd.pool.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class AsyncLoggerTest {
    private ObjectPoolIntf m_threadPool;
    private long m_bt;

    private static String c_logString;
    private static final int c_loopCnt = 5000;


    static {
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[500];
        FileReader fr = null;
        try {
            fr = new FileReader("AsyncTestString.txt");
            int bytesRead;
            while ((bytesRead = fr.read(buf)) >= 0) {
                sb.append(buf, 0, bytesRead);
            }
            c_logString = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
    }

    public AsyncLoggerTest() {
        m_threadPool = new SynchObjectPool(
            "AsyncLoggerTest thread pool",
            ConsoleCourierLogger.instance(),
            new PoolObjectFactory() {
                public Object getObject(ObjectPoolIntf pool) {
                    return new WorkThread(ConsoleCourierLogger.instance(), "Work thread");
                }

                public void returnObject(final Object o) {
                    ((WorkThread)o).close();
                }

                public boolean checkObject(final Object o) {
                    return ((WorkThread)o).isAlive();
                }
            }, 4, 1, -1, 5*60*1000, 4, 5*60*1000, 5*60*1000
        );
    }

    public static void main(String[] args) throws PoolException {
        AsyncLoggerTest o = new AsyncLoggerTest();
        o.testSync();
        o.testAsync();
        o.close();
    }

    private void close() throws PoolException {
        m_threadPool.close();
    }

    private void testSync() {
        long bt = System.currentTimeMillis();
        DataLogger l = getLogger("AsyncTestOutput_Sync.txt");
//      l = new AsyncDataLogger(this, l, getThreadPool(), getTimer(), 5000, 100, 1000000);
        for (int i = 0; i < c_loopCnt; i++) {
            l.log(c_logString);
        }
        l.close();
        System.out.println("sync --> " + (System.currentTimeMillis() - bt));
    }

    private void testAsync() {
        m_bt = System.currentTimeMillis();
        DataLogger l = getLogger("AsyncTestOutput_Async.txt");
        l = new AsyncDataLogger(
            ConsoleCourierLogger.instance(), l, m_threadPool, null, 500000, 100, 10000000,
            new CloseEventListener() {
                public void closed() {
                    System.out.println("closed async --> " + (System.currentTimeMillis() - m_bt));
                }
            }
        );
        for (int i = 0; i < c_loopCnt; i++) {
            l.log(c_logString);
        }
        l.close();
        System.out.println("async --> " + (System.currentTimeMillis() - m_bt));
    }

    private DataLogger getLogger(String name) {
        return new DaysFileLog(
            ConsoleCourierLogger.instance(), new File(name), null, true,
            "yyyy-MM-dd",
            1, "", ".log", false, false, 0
        );
    }
}
