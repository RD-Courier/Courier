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
package ru.rd.courier.logging;

import ru.rd.courier.FileDataTestCase;
import ru.rd.utils.IntervalHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * User: AStepochkin
 * Date: 16.08.2006
 * Time: 16:32:01
 */
public class SameDiscardLogHandlerTest extends FileDataTestCase {
    private SameDiscardLogHandler m_handler;
    private TestHandler m_target;
    private List<TestRecordData> m_expected;
    private int messageId;

    private final long cPublishTime = 0;
    private final long cSentInterval = 100;
    private final int cBufferSize = 1;

    private static class TestHandler extends Handler {
        private final long m_publishTime;
        private final List<LogRecord> m_records = new LinkedList<LogRecord>();

        public TestHandler(long publishTime) {
            m_publishTime = publishTime;
        }

        public synchronized void publish(LogRecord record) {
            try {
                /*
                StringBuffer mes = new StringBuffer();
                mes.append(m_records.size()).append(": ").append(record.getMessage());
                if (record instanceof TestLogRecord) {
                    mes.append(": id = ").append(((TestLogRecord) record).getId());
                }

                System.out.println(mes);
                */
                m_records.add(record);
                if (m_publishTime > 0) Thread.sleep(m_publishTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void flush() {}
        public void close() throws SecurityException {}

        public synchronized List<LogRecord> getRecords() {
            return m_records;
        }
    }

    private static class TestLogRecord extends LogRecord {
        private final int m_id;

        public TestLogRecord(String message, int id) {
            super(Level.ALL, message);
            m_id = id;
        }

        public TestLogRecord(int id) {
            super(Level.ALL, Integer.toString(id));
            m_id = id;
        }

        public int getId() {
            return m_id;
        }
    }

    private static class TestRecordData {
        public final String message;
        public final int id;

        public TestRecordData(String message, int id) {
            this.message = message;
            this.id = id;
        }

        public TestRecordData(TestLogRecord rec) {
            this.message = rec.getMessage();
            this.id = rec.getId();
        }
    }

    private void sendRecord(String message, int id) {
        TestLogRecord rec = new TestLogRecord(message, id);
        m_handler.publish(rec);
    }

    private void sendRecord(int id) {
        sendRecord(Integer.toString(id), id);
    }

    private void sendRecord() {
        sendRecord(messageId);
    }

    private void addExpectedRecord(String message, int id) {
        m_expected.add(new TestRecordData(message, id));
    }

    private void addExpectedRecord(String message, int id, int omitCount) {
        m_expected.add(new TestRecordData(
            SameDiscardLogHandler.getOmitMessage(message, omitCount), id
       ));
    }

    private void addExpectedRecord(int id) {
        addExpectedRecord(Integer.toString(id), id);
    }

    private void addExpectedRecord() {
        addExpectedRecord(messageId);
    }

    private void addOmitExpectedRecord(String message, int omitCount) {
        addExpectedRecord(message, -1, omitCount);
    }

    private void addOmitExpectedRecord(int id, int omitCount) {
        addOmitExpectedRecord(Integer.toString(id), omitCount);
    }

    private void addOmitExpectedRecord(int omitCount) {
        addOmitExpectedRecord(-1, omitCount);
    }

    private void assertRecords() {
        List<LogRecord> actual = m_target.getRecords();
        assertEquals(m_expected.size(), actual.size());
        int i = 0;
        //Iterator<TestLogRecord> actIt = actual.listIterator();
        for (TestRecordData erd: m_expected) {
            //TestLogRecord ard = actIt.next();
            LogRecord ard = actual.get(i);
            if (ard instanceof TestLogRecord) {
                TestLogRecord tard = (TestLogRecord)ard;
                //System.out.println("Pos " + i + ": expected = '" + erd.id + "' actual = '" + tard.getId() + "'");
                assertEquals("Pos " + i + ": Error in id", erd.id, tard.getId());
            }

            //System.out.println("Pos " + i + ": expected = '" + erd.message + "' actual = '" + ard.getMessage() + "'");
            assertEquals("Pos " + i + ": Error in message", erd.message, ard.getMessage());
            i++;
        }
    }

    public void testFastDifferent() {
        initHandler(cSentInterval, 1);

        sendRecord();
        addExpectedRecord();

        messageId++;
        sendRecord();
        addExpectedRecord();

        assertRecords();
    }

    public void testFastSame() {
        initHandler(cSentInterval, 1);

        sendRecord();
        sendRecord();
        addExpectedRecord();

        assertRecords();
    }

    public void testSlowSame() {
        initHandler(cSentInterval, 1);

        sendRecord();
        addExpectedRecord();
        sleep(4*cSentInterval);
        sendRecord();
        addExpectedRecord();

        assertRecords();
    }

    public void testFastSlowSame() {
        initHandler(cSentInterval, 1);

        sendRecord("a", 1);
        sendRecord("a", 2);
        addExpectedRecord("a", 1);
        addOmitExpectedRecord("a", 1);
        sleep(5*cSentInterval);
        sendRecord("a", 3);
        addExpectedRecord("a", 3);

        assertRecords();
    }

    public void testBufferSize() {
        initHandler(cSentInterval, 1);

        int messageId1 = messageId;
        sendRecord();
        addExpectedRecord();

        messageId++;
        sendRecord();
        addExpectedRecord();

        sendRecord(messageId1);
        addExpectedRecord(messageId1);

        assertRecords();
    }

    public void testBufferCleanup() {
        final int cBufSize = 6;

        IntervalHelper ih = new IntervalHelper();
        initHandler(cSentInterval, cBufSize);

        List<Integer> omitIds = new LinkedList<Integer>();
        for (int i = 0; i < cBufSize; i++) {
            sendRecord();
            addExpectedRecord();
            if (i < cBufSize / 2) {
                sendRecord();
                sendRecord();
                omitIds.add(messageId);
            }
            messageId++;
        }
        for (Integer i: omitIds) {
            addOmitExpectedRecord(i, 2);
        }
        //System.out.println("Send time = " + ih.getInterval());
        assertTrue(ih.getInterval() < cSentInterval);

        sleep(5*cSentInterval);
        assertRecords();
    }

    private static void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initHandler(long sentInterval, int bufferSize) {
        m_handler = new SameDiscardLogHandler(
            m_target, sentInterval, bufferSize
        );
    }

    protected void courierSetUp() {
        messageId = 1;
        m_expected = new LinkedList<TestRecordData>();
        m_target = new TestHandler(cPublishTime);
    }

    protected void courierTearDown() {
        if (m_handler != null) {
            m_handler.close();
            m_handler = null;
        }
    }
}
