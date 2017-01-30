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

import org.apache.mina.common.*;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.CourierLoggerAdapter;
import ru.rd.net.*;
import ru.rd.net.message.*;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.PoolExecutorAdapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * User: STEPOCHKIN
 * Date: 15.09.2008
 * Time: 2:47:52
 */
public class MultiCodecServerTest {
    public static void main(String[] args) throws IOException, NoSuchMethodException {
        final CourierLogger logger = new CourierLoggerAdapter(Logger.getLogger(""));
        final ObjectPoolIntf threadPool = PoolExecutorAdapter.createThreadPool(logger, "");
        threadPool.start();
        IoAcceptor acceptor = new SocketAcceptor(4, new PoolExecutorAdapter(threadPool));
        acceptor.addListener(new IoServiceListener() {
            public void serviceActivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config) {}

            public void serviceDeactivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config) {
                threadPool.close();
            }

            public void sessionCreated(IoSession session) {}
            public void sessionDestroyed(IoSession session) {}
        });
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.setReuseAddress(true);
        Charset charset = Charset.forName("cp1251");

        MultiCodecFactory codec = new MultiCodecFactory();
        codec.registerEncoder(
            CommonAnswer.class, 1,
            new ReflectFactory<ProtocolEncoder>(
                CommonAnswerEncoder.class,
                new Class[] {Charset.class}, new Object[] {charset}
            )
        );
        codec.registerEncoder(
            CheckMessage.class, 2,
            new ReflectFactory<ProtocolEncoder>(CheckMessageEncoder.class)
        );

        codec.registerDecoder(
            1, ReflectFactory.factoryOnCharset(MockMessageDecoder.class, charset)
        );
        codec.registerDecoder(2, new ReflectFactory<MessageDecoder>(CheckMessageDecoder.class));

        /*
        MultiEncoder encoder = new MultiEncoder();
        encoder.registerEncoder(
            CommonAnswer.class, 1, CommonAnswerEncoder.class,
            new Class[] {Charset.class}, new Object[] {charset}
        );
        encoder.registerEncoder(CheckMessage.class, 2, CheckMessageEncoder.class);

        MultiDecoder decoder = new MultiDecoder();
        decoder.registerDecoder(
            1, ProcessResultDecoder.class,
            new Class[] {Charset.class}, new Object[] {charset}
        );
        decoder.registerDecoder(2, CheckMessageDecoder.class);
        */

        cfg.getFilterChain().addLast(
            "codec", new ProtocolCodecFilter(
                new CumulativeDecoderProtocolFactory(codec)
                //encoder, new DelegatingCumulativeDecoder(decoder)
            )
        );
        acceptor.bind(new InetSocketAddress("127.0.0.1", 4444), new ServerTestHandler(), cfg);
    }
}
