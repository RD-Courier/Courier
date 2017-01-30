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
package ru.rd.courier.stat;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.courier.manager.message.ProcessResultEncoder;
import ru.rd.net.MessageToProtocolDecoderAdapter;
import ru.rd.net.message.CommonAnswerDecoder;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 14:10:22
 */
public class SocketClientTest {
    private static class ClientIoHandler extends IoHandlerAdapter {
        private final CountDownLatch m_completeSignal;

        public ClientIoHandler(CountDownLatch completeSignal) {
            m_completeSignal = completeSignal;
        }

        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            System.out.println("exceptionCaught: " + cause.getMessage());
            session.close();
        }

        public void messageReceived(IoSession session, Object message) throws Exception {
            //System.out.println("messageReceived");
            //CommonAnswer ca = (CommonAnswer) message;
            //System.out.println("CommonAnswer = " + ca.error);
            m_completeSignal.countDown();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SocketConnector connector = new SocketConnector();
        connector.setWorkerTimeout(1);
        SocketConnectorConfig cfg = new SocketConnectorConfig();
        cfg.setConnectTimeout(30);
        Charset charset = Charset.forName("cp1251");
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(
            new ProcessResultEncoder(charset),
            new MessageToProtocolDecoderAdapter(new CommonAnswerDecoder(charset))
        ));

        IoSession session;
        final int c_messageCount = 10000;
        CountDownLatch completeSignal = new CountDownLatch(c_messageCount);
        ConnectFuture future = connector.connect(
            new InetSocketAddress("127.0.0.1", 4444), new ClientIoHandler(completeSignal), cfg
        );

        future.join();
        session = future.getSession();
        ProcessResult pr = StatProcessingTest.createResult();

        long beginTime = System.currentTimeMillis();
        for (int i = 0; i < c_messageCount; i++) {
            pr.setRecordCount(i);
            session.write(pr).join();
        }
        System.out.println("WriteTime = " + (System.currentTimeMillis() - beginTime));

        completeSignal.await();

        System.out.println("ReadTime = " + (System.currentTimeMillis() - beginTime));

        session.close().join();
    }
}
