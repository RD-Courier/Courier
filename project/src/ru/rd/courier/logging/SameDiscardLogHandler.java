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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * User: AStepochkin
 * Date: 15.08.2006
 * Time: 11:26:03
 */
public class SameDiscardLogHandler extends Handler {
    private final CourierLogger m_logger;
    private final Handler m_target;

    private final Messages m_msgInfos;
    private int m_bufferSize;
    private final long m_sentInterval;
    private final CleanupThread m_cleanupThread;

    private static class MessageInfo {
        private final LogRecord m_record;
        private long m_sentTime;
        private int m_omitCount = 0;

        public MessageInfo(LogRecord record) {
            m_record = record;
        }

        public final long getSentTime() {
            return m_sentTime;
        }

        public final void setSendTime(long sendTime) {
            m_sentTime = sendTime;
        }

        public void incOmitCount() {
            m_omitCount++;
        }

        public int getOmitCount() {
            return m_omitCount;
        }

        public LogRecord getRecord() {
            return m_record;
        }
    }

    public static final MessageInfo DelayMessage = new MessageInfo(null);

    private class Messages {
        private final LinkedHashMap<String, MessageInfo> m_map;

        public Messages() {
            m_map = new LinkedHashMap<String, MessageInfo>(16, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, MessageInfo> eldest) {
                    return size() > m_bufferSize;
                }
            };
        }

        public synchronized Set<String> keySet() {
            return m_map.keySet();
        }

        public synchronized MessageInfo getExpired(String key) {
            MessageInfo m = m_map.get(key);
            if (m == null) return null;
            long curTime = System.currentTimeMillis();
            if (curTime - m.getSentTime() > m_sentInterval) {
                m_map.remove(key);
                return m;
            }
            return null;
        }

        public synchronized MessageInfo getExpired() {
            long curTime = System.currentTimeMillis();

            Iterator<MessageInfo> it = m_map.values().iterator();
            while (it.hasNext()) {
                MessageInfo m = it.next();
                if (curTime - m.getSentTime() >= m_sentInterval) {
                    it.remove();
                    if (m.getOmitCount() > 0) return m;
                }
            }
            return null;
        }

        public synchronized MessageInfo getPublish(String key) {
            MessageInfo m = m_map.get(key);
            if (m == null) return null;
            long curTime = System.currentTimeMillis();
            if (curTime - m.getSentTime() <= m_sentInterval) {
                m.incOmitCount();
                return DelayMessage;
            }
            m_map.remove(key);
            return m;
        }

        public synchronized void put(String key, MessageInfo message) {
            m_map.put(key, message);
        }
    }

    private class CleanupThread extends Thread {
        private boolean m_stopped = false;
        private final CountDownLatch m_finishSignal = new CountDownLatch(1);

        public void run() {
            try {
                unsafeRun();
            } catch (Throwable e) {
                m_logger.error(e);
            }
            m_finishSignal.countDown();
        }

        public void unsafeRun() throws InterruptedException {
            while (true) {
                synchronized(this) {
                    if (m_stopped) break;
                    wait(m_sentInterval);
                    if (m_stopped) break;
                }

                while (true) {
                    if (m_stopped) break;
                    MessageInfo m = m_msgInfos.getExpired();
                    if (m == null) break;
                    LogRecord record = m.getRecord();
                    LogRecord sendRecord = new LogRecord(
                        record.getLevel(),
                        getOmitMessage(record.getMessage(), m.getOmitCount())
                    );
                    m_target.publish(sendRecord);
                }
            }
        }

        public boolean stopAndWait(long timeout) throws InterruptedException {
            synchronized(this) {
                m_stopped = true;
                notifyAll();
            }
            return m_finishSignal.await(timeout, TimeUnit.MILLISECONDS);
        }
    }

    public SameDiscardLogHandler(
        Handler target, long sentInterval, int bufferSize
    ) {
        m_logger = new LogHandlerLogger("Discard filter");
        m_target = target;

        m_bufferSize = bufferSize;
        m_sentInterval = sentInterval;
        m_msgInfos = new Messages();
        m_cleanupThread = new CleanupThread();
        m_cleanupThread.start();
    }

    public static String getOmitMessage(String message, int omitCount) {
        return message  + "\n(" + omitCount + " the same messages were omitted)";
    }

    public final void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        String curMessage = record.getMessage();
        long curTime = System.currentTimeMillis();
        MessageInfo mi = m_msgInfos.getPublish(curMessage);
        if (mi == DelayMessage) return;
        if (mi == null) {
            mi = new MessageInfo(record);
            mi.setSendTime(curTime);
            m_msgInfos.put(curMessage, mi);
        }
        if (mi.getOmitCount() > 0) {
            record.setMessage(getOmitMessage(record.getMessage(), mi.getOmitCount()));
        }
        m_target.publish(mi.getRecord());
    }

    private void innerFlush() {}

    public final void flush() {
        innerFlush();

        try { m_target.flush(); }
        catch (Exception e) { m_logger.error(e); }
    }

    public final void close() {
        try { m_cleanupThread.stopAndWait(1000); }
        catch (Exception e) { m_logger.error(e); }

        innerFlush();
        m_target.close();
        setLevel(Level.OFF);
    }

    /*
    private void printBuffer(PrintStream out) {
        out.println("Log sent messages:");
        for (Map.Entry<String, MessageInfo> e: m_msgInfos.entrySet()) {
            out.print(e.getKey());
            out.print(" --> ");
            out.println(e.getValue().getSentTime());
        }
    }
    */

    public final synchronized int getSameMessageBufferSize() {
        return m_bufferSize;
    }

    public final synchronized void setSameMessageBufferSize(int size) {
        m_bufferSize = size;
    }
}
