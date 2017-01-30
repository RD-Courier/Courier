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

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.manager.message.*;
import ru.rd.net.*;
import ru.rd.net.message.*;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.PoolExecutorAdapter;
import ru.rd.thread.AsynchProcessing;
import ru.rd.utils.*;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Timer;

/**
 * User: Astepochkin
 * Date: 02.10.2008
 * Time: 15:12:38
 */
public class ManagerClient extends KeepAlive implements TimedDisposable, Disposable {
    private final ObjectPoolIntf m_threadPool;
    private final CourierInfoMessage m_courier;
    private final String m_host;
    private final int m_port;
    private SocketConnector m_connector;
    private SocketConnectorConfig m_cfg;
    private final InnerIoHandler m_handler;
    private ASynchSynchClient m_client;
    private StatProcessing m_statClient;
    private int m_maxTargetCount;
    private String m_desc;

    private long m_statCheckInterval;
    private long m_statBufferInterval;

    public ManagerClient(
        CourierLogger logger, ObjectPoolIntf threadPool, Timer timer,
        String host, int port, CourierInfoMessage courier
    ) {
        super(logger, threadPool, timer);
        m_threadPool = threadPool;
        m_host = host;
        m_port = port;
        m_courier = courier;
        m_handler = new InnerIoHandler();
        m_desc = "CourierManagerClient-" + courier.getCode();
    }

    public void setStatCheckInterval(long value) {
        m_statCheckInterval = value;
    }

    public void setStatBufferInterval(long value) {
        m_statBufferInterval = value;
    }

    public String getDesc() {
        return m_desc;
    }

    public void setDesc(String value) {
        m_desc = value;
    }

    public final void setConnector(SocketConnector connector) {
        m_connector = connector;
    }

    public void setMaxCount(int value) {
        m_maxTargetCount = value;
    }

    public CourierInfo getCourierInfo() {
        return m_courier;
    }

    public void sendStat(ProcessResult res) {
        m_statClient.addTarget(res);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (getDesc() != null) {
            sb.append('\'').append(getDesc()).append("\' ");
        }
        sb.append("Courier (").append(getCourierInfo()).append(')');
        return sb.toString();
    }

    public boolean dispose(long timeout) {
        return stop(timeout);
    }

    public void dispose() {
        stop();
    }

    private class InnerIoHandler extends IoHandlerAdapter {
        public void sessionOpened(IoSession session) throws Exception {
            info("sessionOpened");
        }

        public void sessionClosed(IoSession session) throws Exception {
            info("sessionClosed");
            if (getState() == c_stateStarted) invalidateClient();
        }

        public void messageReceived(IoSession session, Object message) throws Exception {
            debug("messageReceived: " + message.getClass().getSimpleName() + " " + message);
        }

        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {}
    }

    private void invalidateClient() {
        invalidate();
        //m_statClient.setManagerInfo(null);
    }

    protected void processedProcessResult(ProcessResult message) {}

    public void start() {
        m_cfg = new SocketConnectorConfig();
        m_cfg.setThreadModel(ThreadModel.MANUAL);
        m_cfg.setConnectTimeout((int)Math.max(1, m_timeout / 1000));
        m_cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(buildMainCodecFactory()));
        //m_cfg.getFilterChain().addLast("threadPool", new ExecutorFilter(new PoolExecutorAdapter(m_threadPool)));
        m_statClient = new StatProcessing(
            m_logger, new PoolExecutorAdapter(m_threadPool), 1, m_host
        ) {
            protected void processedProcessResult(ProcessResult message) {
                ManagerClient.this.processedProcessResult(message);
            }
        };
        m_statClient.setDesc("StatProcessor-" + getCourierInfo().getCode());
        m_statClient.setTimer(m_timer);
        m_statClient.setCheckInterval(m_statCheckInterval);
        m_statClient.setBufferProperties(m_statBufferInterval, 100);
        m_statClient.setChunkSize(100);
        m_statClient.setLastExecSleepInterval(1000);
        //m_statClient.setChunkSize(100);
        m_statClient.setMaxTargetCount(m_maxTargetCount);
        m_statClient.resourceUnavailable();
        super.start();
    }

    public boolean stop(long timeout) {
        boolean ret = true;
        if (m_statClient != null) ret = m_statClient.close(timeout) & ret;
        ret = super.stop(timeout) & ret;
        return ret;
    }

    public void stop() {
        stop(m_timeout);
    }

    private void ensureConnector() {
        if (m_connector == null) {
            m_connector = new SocketConnector(1, new PoolExecutorAdapter(m_threadPool));
            m_connector.setWorkerTimeout((int)getCheckInterval()/1000 + 1);
        }
    }

    protected void init() throws Exception {
        ensureConnector();
        m_client = new ASynchSynchClient(
            m_logger, m_connector,
            new InetSocketAddress(m_host, m_port),
            m_cfg, m_handler, m_timeout
        );
        ManagerInfo mi = (ManagerInfo)m_client.write(m_courier, m_timeout);
        m_statClient.setManagerInfo(mi);
    }

    /*
    private long m_checkId = 0;
    protected void checkCustom() throws Exception {
        long id = m_checkId++;
        CheckMessage res = (CheckMessage)m_client.write(new CheckMessage(id), m_timeout);
        if (res.getId() != id) {
            throw new RuntimeException("CheckId: expected=" + id + " actual=" + res.getId());
        }
    }
    */
    
    protected Runnable getCleanupWork() {
        if (m_client == null) return null;

        //try { m_client.close(m_timeout); } catch (Exception e) { m_logger.warning(e); }
        //try { m_statClient.close(m_timeout); } catch (Exception e) { m_logger.warning(e); }
        m_statClient.resourceUnavailable();

        InvokeAllRunnable cw = new InvokeAllRunnable(m_logger, "MessageSenderCleaner");
        cw.add(
            new Invokee() {
                private final ASynchSynchClient m_closeClient = m_client;
                public void invoke() throws Exception {
                    m_closeClient.close(m_timeout);
                }
            }
        );

        if (m_statClient != null) {
            m_statClient.resourceUnavailable();
            cw.add(
                new Invokee() {
                    private final AsynchProcessing m_closeStatClient = m_statClient;
                    public void invoke() throws Exception {
                        m_closeStatClient.stopActivities();
                    }
                }
            );
        }
        m_client = null;
        return cw;
    }

    private static <T> ObjectFactory<? extends T> codecFactory(Class<? extends T> cls, Charset charset) throws NoSuchMethodException {
        return new ReflectFactory<T>(cls, new Class[] {Charset.class}, new Object[] {charset});
    }

    public ProtocolCodecFactory buildMainCodecFactory() {
        final Charset charset = Charset.forName("cp1251");
        MultiCodecFactory codec = new MultiCodecFactory();
        try {
            codec.registerEncoder(
                CourierInfoMessage.class, 1,
                codecFactory(CourierInfoMessageEncoder.class, charset)
            );
            codec.registerEncoder(
                StopMessage.class, 2,
                new ReflectFactory<NullMessageEncoder>(NullMessageEncoder.class)
            );
            codec.registerEncoder(
                CheckMessage.class, 3,
                new ReflectFactory<CheckMessageEncoder>(CheckMessageEncoder.class)
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        codec.registerDecoder(1, new ObjectFactory<MessageDecoder>() {
            public MessageDecoder create() throws Exception {
                return new ManagerInfoMessageDecoder(charset) {
                    protected void customProcessing(IoSession session, ManagerInfoMessage message) {
                        //m_statClient.setManagerInfo(message);
                        //session.write(m_courier);
                    }
                };
            }
        });

        codec.registerDecoder(2, new ObjectFactory<MessageDecoder>() {
            public MessageDecoder create() throws Exception {
                return new CommonAnswerDecoder(charset) {
                    protected void customProcessing(IoSession session, CommonAnswer message) {
                        message.checkError();
                    }
                };
            }
        });

        codec.registerDecoder(3, new ObjectFactory<MessageDecoder>() {
            public MessageDecoder create() throws Exception {
                return new CheckMessageDecoder() {
                    protected void customProcessing(IoSession session, CheckMessage message) {
                        m_client.send(message);
                    }
                };
            }
        });

        return new CumulativeDecoderProtocolFactory(codec);
    }
}
