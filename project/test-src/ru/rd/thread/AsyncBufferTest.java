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
import ru.rd.courier.logging.LoggerAdapter;
import ru.rd.courier.utils.CloseEventListener;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolException;
import ru.rd.pool.SynchObjectPool;

import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AsyncBufferTest extends TestCase {
    private Set m_data;
    private TestReceiver m_receiver;
    private CloseEventListenerMock m_closeListener;
    private LoggerAdapter m_logger;
    private TestLogHandler m_logHandler;
    private ObjectPoolIntf m_threadPool;
    private AsyncBuffer m_buffer;

    public AsyncBufferTest(String name) {
        super(name);
    }

    /*
    private static int intCeilDivide(int i, int divider) {
        int remainder = i % divider;
        int ret = i / divider;
        if (remainder > 0) ret++;
        return ret;
    }
    */

    protected void setUp() throws Exception {
        m_closeListener = new CloseEventListenerMock();
        m_logger = new LoggerAdapter(null, "test", false);
        m_logHandler = new TestLogHandler();
        m_logger.getInnerLogger().setLevel(Level.SEVERE);
        m_logger.getInnerLogger().addHandler(m_logHandler);
        m_logger.getInnerLogger().setUseParentHandlers(false);
        m_threadPool = new SynchObjectPool(
            "mock thread pool",
            m_logger,
            new ThreadFactory(m_logger, "Test"),
            0, 1, -1, -1, -1, -1, -1
        );
        m_threadPool.start();
        m_data = new HashSet();
    }

    protected void tearDown() throws Exception {
        m_receiver = null;
        m_closeListener = null;
        m_logger = null;
        m_logHandler = null;
        m_threadPool = null;
        m_buffer = null;
        m_data = null;
    }

    private void addData(
        final Object[] data,
        Object waitEvent, Semaphore allWaitSemaphore, final Semaphore doneSemaphore
    ) throws PoolException {
        for (int i = 0; i < data.length; i++) {
            final Object item = data[i];
            if (m_data.contains(item)) {
                throw new RuntimeException("Data already has element: " + data[i].toString());
            }
            m_data.add(item);
        }
        if (waitEvent == null) {
            m_buffer.add(data);
        } else {
            doneSemaphore.inc();
            (new ReleasableWorkThread(
                m_threadPool,
                new TestRunnable(m_logger.getInnerLogger(), allWaitSemaphore, waitEvent) {
                    protected void testRun() throws PoolException {
                        m_buffer.add(data);
                        doneSemaphore.dec();
                    }
                }
            )).launch();
        }
    }

    private void addData(Object[] data) throws PoolException {
        addData(data, null, null, null);
    }

    private abstract class BufferTest {
        private Object m_closeEvent = null;

        public BufferTest(TestReceiver receiver, AsyncBuffer buffer, Object closeEvent) {
            m_receiver = receiver;
            m_buffer = buffer;
            m_closeEvent = closeEvent;
        }

        public BufferTest(TestReceiver receiver, AsyncBuffer buffer) {
            this(receiver, buffer, null);
        }

        private void test() throws Exception {
            customTest();
            m_buffer.close();
            assertBuffer();
            if (m_closeEvent != null) {
                synchronized(m_closeEvent) {
                    m_closeEvent.wait();
                }
            }
            m_threadPool.close();
        }

        protected abstract void customTest() throws Exception;
    }

    private abstract class StdBufferBufferTest extends BufferTest {
        public StdBufferBufferTest(
            TestReceiver receiver, int maxThreadCount, long period,
            int partSize, Object closeEvent
        ) {
            super(
                receiver,
                new AsyncBuffer(
                    m_logger,
                    m_threadPool,
                    maxThreadCount,
                    null,
                    period,
                    receiver,
                    partSize,
                    m_closeListener
                ),
                closeEvent
            );
        }

        public StdBufferBufferTest(
            TestReceiver receiver, int maxThreadCount, long period, int partSize
        ) {
            this(receiver, maxThreadCount, period, partSize, null);
        }
    }

    private static class TestObject {
        private int m_partNumber;
        private int m_index;

        public TestObject(int partNumber, int index) {
            m_partNumber = partNumber;
            m_index = index;
        }

        public boolean equals(Object obj) {
            TestObject tobj = (TestObject)obj;
            return m_partNumber == tobj.m_partNumber && m_index == tobj.m_index;
        }

        public String toString() {
            return m_partNumber + "." + m_index;
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public int getPartNumber() {
            return m_partNumber;
        }

        public int getIndex() {
            return m_index;
        }
    }

    private static Object[] getDataPart(int number, int size) {
        TestObject[] ret = new TestObject[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new TestObject(number, i);
        }
        return ret;
    }

    private boolean incDescsIndexes(int[] descs, int templateSize) {
        for (int i = 0; i < descs.length; i++) {
            if (descs[i] == templateSize - 1) {
                descs[i] = 0;
            } else if (descs[i] < templateSize) {
                descs[i]++;
                return true;
            }
        }
        return false;
    }

    private void debugMessage(String message) {
        //System.out.println(message);
    }

    private static String intArrayToString(int[] ar) {
        final String cSep = ", ";
        StringBuffer ret = new StringBuffer((cSep.length() + 1) * ar.length);
        for (int i = 0; i < ar.length; i++) {
            if (i > 0) ret.append(cSep);
            ret.append(ar[i]);
        }
        return ret.toString();
    }

    public void testPartCombinations() throws Exception {
        //testPartCombinations(30, 10, 3, 10, 4, 3);
        //testPartCombinations(5, 15, 3, 30, 3, 3);
        testPartCombinations(5, 20, 5, 3, 3, 3);
        //testPartCombinations(0, 200, 2, 3, 3, 3);
    }

    public void testCloseWait() throws Exception {
        testParts(
            0, 200, 3, 10, 3, false, new int[] {10}
        );
        assertEquals(3, m_receiver.m_threads.size());
        tearDown();
        setUp();
        testParts(
            0, 200, 3, -1, 3, false, new int[] {10}
        );
        assertEquals(1, m_receiver.m_threads.size());
    }

    public void testSimultaniousParts() throws Exception {
        int[] partSizes = new int[100];
        for (int i = 0; i < partSizes.length; i++) {
            partSizes[i] = 100;
        }
        testSimultaniousParts(10, 30, 7, 5, partSizes);
    }

    private void testPartCombinations(
        long partSleepPeriod, int sleepPeriod, int maxThreadCount,
        long period, int partSize, int partAmount
    ) throws Exception {
        int[] descTemplate = new int[] {0, 1, partSize, partSize + 1, partSize * 3};
        for (int curPartAmount = 0; curPartAmount < partAmount; curPartAmount++) {
            int[] descIndexes = new int[curPartAmount];
            int[] descs = new int[curPartAmount];
            for (int i = 0; i < descIndexes.length; i++) descIndexes[i] = 0;
            do {
                for (int i = 0; i < descs.length; i++) {
                    descs[i] = descTemplate[descIndexes[i]];
                }
                debugMessage("Testing: " + intArrayToString(descs));
                setUp();
                testParts(
                    partSleepPeriod, sleepPeriod, maxThreadCount,
                    period, partSize, false, descs
                );
                tearDown();
            } while(incDescsIndexes(descIndexes, descTemplate.length));
        }
    }

    private void testParts(
        final long partSleepPeriod, int sleepPeriod, int maxThreadCount,
        long period, int partSize, final boolean launchThreadAfterPart,
        final int[] dataSizes
    ) throws Exception {
        BufferTest test = new StdBufferBufferTest(
            new TestReceiver(sleepPeriod), maxThreadCount, period, partSize
        ) {
            protected void customTest() throws Exception {
                for (int i = 0; i < dataSizes.length; i++) {
                    addData(getDataPart(i, dataSizes[i]));
                    if (launchThreadAfterPart) m_buffer.launchThread();
                    Thread.sleep(partSleepPeriod);
                }
            }
        };
        test.test();
    }

    private void testSimultaniousParts(
        int sleepPeriod, int maxThreadCount, long period,
        int partSize, final int[] dataSizes
    ) throws Exception {
        final Object event = new Object();
        final Semaphore startSemaphore = new Semaphore();
        startSemaphore.setCounter(dataSizes.length);
        final Semaphore doneSemaphore = new Semaphore();
        BufferTest test = new StdBufferBufferTest(
            new TestReceiver(sleepPeriod), maxThreadCount, period, partSize
        ) {
            protected void customTest() throws Exception {
                for (int i = 0; i < dataSizes.length; i++) {
                    addData(
                        getDataPart(i, dataSizes[i]),
                        event, startSemaphore, doneSemaphore
                    );
                }
                //Thread.sleep(1000);
                startSemaphore.waitEvent();
                synchronized(event) {
                    event.notifyAll();
                }
                doneSemaphore.waitEvent();
            }
        };
        test.test();
    }


    private void assertBuffer() throws Exception {
        if (m_logHandler.getErrors().length > 0) {
            throw (Exception)m_logHandler.getErrors()[0];
        }
        //assertEquals(0, m_logHandler.getErrors().length);

        if (m_receiver.hasErrors()) {
            throw (Exception)m_receiver.getFirstError();
        }
        //assertFalse(m_receiver.hasErrors());

        assertEquals("Object count -", m_data.size(), m_receiver.getObjectsCount());
        //assertEquals(m_partCount, m_receiver.m_partCount);
        for (Iterator it = m_data.iterator(); it.hasNext();) {
            TestObject obj = (TestObject) it.next();
            Integer countObj = (Integer)m_receiver.m_handledObjectCounts.get(obj);
            assertNotNull(countObj);
            assertEquals(1, countObj.intValue());
        }
        assertEquals(1, m_receiver.m_closeCount);
        assertEquals(1, m_closeListener.m_closedCount);
        //System.out.println("Engaged thread count = " + m_receiver.m_threads.size());
        assertTrue(m_receiver.m_threads.size() <= m_buffer.getMaxThreadCount());
    }

    private static class TestReceiver implements AsyncBuffer.Receiver {
        private Set m_threads = new HashSet();
        private List m_handledObjects = new LinkedList();
        private Map m_handledObjectCounts = new HashMap();
        //private int m_partCount = 0;
        private int m_closeCount = 0;
        private long m_sleepPeriod;
        private List m_errors = new LinkedList();

        public TestReceiver(long sleepPeriod) {
            if (sleepPeriod < 0) {
                throw new IllegalArgumentException(
                    "Sleep period cannot be less than 0: period = " + sleepPeriod
                );
            }
            m_sleepPeriod = sleepPeriod;
        }

        private void addError(Throwable error) {
            m_errors.add(error);
        }

        private synchronized void gatherData(List dataPart) throws Exception {
            m_threads.add(Thread.currentThread());
            //m_partCount++;
            for (Iterator it = dataPart.iterator(); it.hasNext();) {
                TestObject obj = (TestObject)it.next();
                m_handledObjects.add(obj);
                Integer oldCount = (Integer)m_handledObjectCounts.get(obj);
                int count;
                if (oldCount == null) {
                    count = 1;
                } else {
                    count = oldCount.intValue() + 1;
                }
                m_handledObjectCounts.put(obj, new Integer(count));
            }
        }

        public void handleData(List dataPart) {
            /*
            System.out.println(
                System.currentTimeMillis() + " Begin handle part " +
                dataPart.get(0) + " - " + dataPart.get(dataPart.size() - 1)
            );
            */
            try {
                if (m_sleepPeriod > 0) Thread.sleep(m_sleepPeriod);
                gatherData(dataPart);
            } catch (Exception e) {
                addError(e);
            }
            /*
            System.out.println(
                System.currentTimeMillis() + " End handle part " +
                dataPart.get(0) + " - " + dataPart.get(dataPart.size() - 1)
            );
            */
        }

        public synchronized void close() {
            m_closeCount++;
        }

        public synchronized int getObjectsCount() {
            return m_handledObjects.size();
        }

        public synchronized boolean hasErrors() {
            return m_errors.size() > 0;
        }

        public synchronized Throwable getFirstError() {
            if (m_errors.isEmpty()) return null;
            return (Throwable)m_errors.get(0);
        }
    }

    private static class TestLogHandler extends Handler {
        private List m_throwns = new LinkedList();

        public void close() throws SecurityException {
        }

        public void flush() {
        }

        public synchronized void publish(LogRecord record) {
            if (record.getThrown() != null) {
                m_throwns.add(record.getThrown());
            }

            if (record.getLevel().intValue() <= Level.FINE.intValue()) {
                System.out.println(record.getMessage());
            }
        }

        public synchronized Throwable[] getErrors() {
            return (Throwable[])m_throwns.toArray(new Throwable[m_throwns.size()]);
        }
    }

    private abstract static class TestRunnable implements Runnable {
        private Logger m_logger;
        private Semaphore m_semaphore;
        private Object m_event;

        public TestRunnable(Logger logger, Semaphore semaphore, Object event) {
            if (logger == null) {
                throw new IllegalArgumentException("logger cannot be null");
            }
            if (semaphore == null) {
                throw new IllegalArgumentException("semaphore cannot be null");
            }
            if (event == null) {
                throw new IllegalArgumentException("event cannot be null");
            }
            m_logger = logger;
            m_semaphore = semaphore;
            m_event = event;
        }

        public void run() {
            try {
                synchronized(m_event) {
                    m_semaphore.dec();
                    m_event.wait();
                }
                testRun();
            } catch (Exception e) {
                m_logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        protected abstract void testRun() throws PoolException;
    }

    private static class CloseEventListenerMock implements CloseEventListener {
        private int m_closedCount = 0;
        public synchronized void closed() {
            m_closedCount++;
        }
    }

    private static class Semaphore {
        private int m_counter = 0;

        public Semaphore() {
        }

        public synchronized void waitEvent() throws InterruptedException {
            if (!needToWait()) return;
            wait();
        }

        private boolean needToWait() {
            return m_counter > 0;
        }

        public synchronized void inc() {
            m_counter++;
        }

        public synchronized void dec() {
            if (m_counter == 0) {
                throw new RuntimeException("Invalid dec invocation because counter = 0");
            }
            m_counter--;
            if (m_counter == 0) notifyAll();
        }

        public synchronized void setCounter(int counter) {
            m_counter = counter;
        }
    }

    private static class ReleasableWorkThread {
        private final ObjectPoolIntf m_threadPool;
        private final Runnable m_work;

        public ReleasableWorkThread(ObjectPoolIntf threadPool, Runnable work) {
            m_threadPool = threadPool;
            m_work = work;
        }

        public void launch() throws PoolException {
            final WorkThread workThread = (WorkThread)m_threadPool.getObject();
            workThread.launchWork(
                m_work,
                new FreeListener() {
                    public void free(Runnable w) {
                        m_threadPool.releaseObject(workThread);
                    }
                }
            );
        }
    }
}
