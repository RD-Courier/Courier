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
package ru.rd.net;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import ru.rd.TestUtils;
import ru.rd.net.message.CommonAnswer;
import ru.rd.net.message.CommonAnswerEncoder;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool2.ObjectPool2;
import ru.rd.thread.PoolExecutorAdapter;
import ru.rd.thread.AsynchProcessing;
import ru.rd.utils.ObjectFactory;
import ru.rd.utils.TimeElapsed;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * User: AStepochkin
 * Date: 09.10.2008
 * Time: 15:52:34
 */
public class SocketTransferTestServer {
    public static void main1(String[] args) throws IOException {
        CourierLogger logger = TestUtils.getTestLogger("SocketTransferTestServer.log");
        ServerSocket acceptor = new ServerSocket();
        try {
            acceptor.bind(new InetSocketAddress("127.0.0.1", 4444));
            Socket s = acceptor.accept();
            try {
                useSocket(logger, s.getInputStream(), s.getOutputStream());
            } finally {
                s.close();
            }
        } finally {
            acceptor.close();
        }
    }

    private static void useSocket(CourierLogger logger, InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4*1024];
        int count;
        int totalCount = 0;
        TimeElapsed te = new TimeElapsed();
        try {
            while ((count = in.read(buffer)) >= 0) {
                totalCount += count;
                out.write(buffer, 0, count);
            }
            logger.info("Bytes read: " + totalCount + " TimeElapsed: " + te.elapsed());
        } finally {
            in.close();
            out.close();
        }
    }

    private static AbstractASynchSynchClient sessionClient(IoSession session) {
        AbstractASynchSynchClient client = (AbstractASynchSynchClient)session.getAttribute(cSessionClient);
        if (client == null) throw new RuntimeException("Absent session client");
        return client;
    }

    private static final String cSessionClient = "TestSynchClient";

    private static class AsynchProcessingPair<T> {
        public final IoSession session;
        public final T data;

        public AsynchProcessingPair(IoSession session, T data) {
            this.session = session;
            this.data = data;
        }
    }

    public static void main(String[] args) throws IOException, NoSuchMethodException {
        final CourierLogger logger = TestUtils.getTestLogger("SocketTransferTestServer.log");
        final ObjectPool2 threadPool = PoolExecutorAdapter.createThreadPool2(logger, "");
        final Executor exec = new PoolExecutorAdapter(threadPool);
        final AsynchProcessing<Object, AsynchProcessingPair<MockMessage>> mockProc = new AsynchProcessing<Object, AsynchProcessingPair<MockMessage>>(logger, exec, 1) {
            private final Object m_res = new Object();
            private int m_count = 0;

            protected Object findResource() throws Exception {
                return m_res;
            }

            protected boolean isResourceValid(Object resource) throws Exception {
                return true;
            }

            protected void releaseResource(Object resource) throws Exception {}
            protected void releaseCancelledResource(Object resource) throws Exception {}
            protected void process(Object resource, AsynchProcessingPair<MockMessage> target) throws Exception {
                if (m_count % 1000 == 0) {
                    //String st = ErrorHelper.stackTracesToString(Thread.currentThread().getThreadGroup());
                    //logger.debug(st);
                }
                m_count++;
            }
            protected void process(Object resource, List<AsynchProcessingPair<MockMessage>> targets) throws Exception {
                for (AsynchProcessingPair<MockMessage> target: targets) {
                    process(resource, target);
                }
            }
        };
        mockProc.setChunkSize(100);
        threadPool.start();
        IoAcceptor acceptor = new SocketAcceptor(2, exec);
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.setReuseAddress(true);
        cfg.setThreadModel(ThreadModel.MANUAL);
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(buildCodec(mockProc)));
        acceptor.bind(new InetSocketAddress("127.0.0.1", 4444), new IoHandlerAdapter() {
            public void sessionOpened(IoSession session) throws Exception {
                AbstractASynchSynchClient client = new AbstractASynchSynchClient(logger, session);
                session.setAttribute(cSessionClient, client);
            }

            public void sessionClosed(IoSession session) throws Exception {
                sessionClient(session).sessionClosed();
            }

            public void messageReceived(IoSession session, final Object message) throws Exception {
                //sessionClient(session).resultReceived(message);
                //ByteBuffer buffer = (ByteBuffer)message;
                //final ByteBuffer res = ByteBuffer.allocate(buffer.remaining());
                //res.put(buffer);
                //res.flip();
                mockProc.addTarget(new AsynchProcessingPair<MockMessage>(session, (MockMessage)message));
            }
        }, cfg);
    }

    private static ProtocolCodecFactory buildCodec(final AsynchProcessing<Object, AsynchProcessingPair<MockMessage>> processing) throws NoSuchMethodException {
        final Charset charset = Charset.forName("cp1251");
        MultiCodecFactory codec = new MultiCodecFactory();
        codec.registerEncoder(
            CommonAnswer.class, 1,
            ReflectFactory.factoryOnCharset(CommonAnswerEncoder.class, charset)
        );

        codec.registerDecoder(
            1,
            new ObjectFactory<MessageDecoder>() {
                public MessageDecoder create() throws Exception {
                    return new MockDecoder<MockMessage>(MockMessage.class, charset) {
                        protected void customProcessing(IoSession session, MockMessage message) throws Exception {
                            processing.addTarget(new AsynchProcessingPair<MockMessage>(session, message));
                            //debug("MockMessage: {0}", message);
                            //processResult(c, message);
                            session.write(new CommonAnswer());
                        }
                    };
                }
            }
        );
        return new CumulativeDecoderProtocolFactory(codec);
    }
}
