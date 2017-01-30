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
package ru.rd.net.synch;

import org.apache.mina.common.ByteBuffer;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.net.NetUtils;
import ru.rd.net.ProcessedMessageListener;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * User: AStepochkin
 * Date: 22.04.2009
 * Time: 13:43:55
 */
public abstract class BufferedSynchClient {
    private final CourierLogger m_logger;
    private final Timer m_timer;
    private final Executor m_exec;
    private final Socket m_socket;
    private final InputStream m_in;
    private final BufferedOutputStream m_out;
    private final SynchEncoder<Object> m_encoder;
    private final SynchDecoder<Object> m_decoder;
    private final Collection<ProcessedMessageListener> m_listeners = new LinkedList<ProcessedMessageListener>();

    private TimerTask m_sendTask = null;
    private int m_maxCalls;
    private long m_bufInterval;
    private int m_callCount;
    private List<Object> m_buffer = new LinkedList<Object>();

    public BufferedSynchClient(
        CourierLogger logger, Timer timer, Executor exec,
        String host, int port,
        SynchEncoder<Object> encoder, SynchDecoder<Object> decoder,
        int bufferSize
    ) throws IOException {
        m_logger = logger;
        m_timer = timer;
        m_exec = exec;
        m_socket = new Socket(host, port);
        m_in = m_socket.getInputStream();
        m_out = new BufferedOutputStream(m_socket.getOutputStream(), bufferSize);
        m_encoder = encoder;
        m_decoder = decoder;

        m_maxCalls = 0;
        m_callCount = 0;
        m_bufInterval = 0;
    }

    public synchronized final void setBufferInterval(long value) {
        m_bufInterval = value;
    }

    public synchronized void addListener(ProcessedMessageListener l) {
        m_listeners.add(l);
    }

    public synchronized boolean removeListener(ProcessedMessageListener l) {
        return m_listeners.remove(l);
    }

    public synchronized void removeListeners() {
        m_listeners.clear();
    }

    protected abstract Object wrapData(List<Object> data);

    public synchronized void writeBuffered(Object message) throws Exception {
        if (m_maxCalls <= 0 && m_bufInterval <= 0) {
            write(message);
            return;
        }
        m_callCount++;
        m_buffer.add(message);
        if (m_maxCalls > 0 && m_callCount >= m_maxCalls) {
            doBufferedWrite();
            return;
        }
        if (m_sendTask == null) {
            m_sendTask = new TimerTask() {
                public void run() {
                    m_exec.execute(new Runnable() {
                        public void run() {
                            try {
                                doBufferedWrite();
                            } catch (Exception e) {
                                m_logger.error(e);
                            }
                        }
                    });
                }
            };
            m_timer.schedule(m_sendTask, m_bufInterval);
        }
    }

    public synchronized Object write(Object message) throws Exception {
        doBufferedWrite();
        Object ret = writeMessage(message);
        processedMessage(message);
        return ret;
    }

    public Socket getSocket() {
        return m_socket;
    }

    private void debug(String message) {
        System.out.println(message);
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private void writeDebug(Object message, String num, String extra) {
        StringBuffer m = new StringBuffer(
            "SynchClient.write " + num + ": "
            + message.getClass().getSimpleName() + "(" + message + ")"
        );
        if (extra != null && extra.length() > 0) {
            m.append(" ").append(extra);
        }
        debug(m.toString());
    }

    private synchronized void doBufferedWrite() throws Exception {
        if (m_sendTask != null) {
            m_sendTask.cancel();
            m_sendTask = null;
        }
        m_callCount = 0;
        if (m_buffer.size() > 0) {
            if (m_buffer.size() == 1) {
                writeMessage(m_buffer.get(0));
                processedMessage(m_buffer.get(0));
            } else {
                writeMessage(wrapData(m_buffer));
                for (Object m: m_buffer) {
                    processedMessage(m);
                }
            }
            m_buffer.clear();
        }
    }

    private Object writeMessage(Object message) throws Exception {
        final byte[] buffer = new byte[1024];
        //writeDebug(message, "10", null);
        m_encoder.encode(
            message,
            new SynchEncoderOutput() {
                public void write(ByteBuffer buf) throws IOException {
                    NetUtils.bufToStream(buffer, buf, m_out);
                }
            }
        );
        m_out.flush();
        Object result = null;
        for (;;) {
            //writeDebug(message, "20", null);
            int count = m_in.read(buffer);
            //writeDebug(message, "30", "count = " + count);
            if (count < 0) break;
            if (count > 0) {
                result = m_decoder.decode(ByteBuffer.wrap(buffer, 0, count));
                if (result != null) break;
            }
        }
        return result;
    }

    public synchronized void close() throws Exception {
        if (m_sendTask != null) {
            m_sendTask.cancel();
            m_sendTask = null;
        }
        try { doBufferedWrite(); } catch (Exception e) { m_logger.error(e); }
        try { m_out.close(); } catch (Exception e) { m_logger.error(e); }
        try { m_socket.close(); } catch (Exception e) { m_logger.error(e); }
    }

    private void processedMessage(Object message) {
        for (ProcessedMessageListener l: m_listeners) {
            l.processedMessage(message);
        }
    }
}
