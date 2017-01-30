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
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.CourierLoggerAdapter;
import ru.rd.courier.manager.message.ProcessResultDecoder;
import ru.rd.net.MessageToProtocolDecoderAdapter;
import ru.rd.net.message.CommonAnswerEncoder;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.PoolExecutorAdapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 12:32:00
 */
public class ProcessResultServerTest {
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
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(
            new CommonAnswerEncoder(charset),
            new MessageToProtocolDecoderAdapter(new ProcessResultDecoder(charset))
        ));
        acceptor.bind(new InetSocketAddress("127.0.0.1", 4444), new ServerTestHandler(false), cfg);
    }
}
