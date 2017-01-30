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

import org.apache.mina.common.*;
import org.apache.mina.transport.socket.nio.SocketConnector;
import ru.rd.courier.logging.CourierLogger;

import java.net.SocketAddress;

/**
 * User: STEPOCHKIN
 * Date: 12.09.2008
 * Time: 13:00:02
 */
public class ASynchSynchClient extends AbstractASynchSynchClient {
    private final IoHandler m_outerHandler;

    public ASynchSynchClient(
        CourierLogger logger,
        SocketConnector connector, SocketAddress address,
        IoServiceConfig config,
        IoHandler outerHandler, long connectTimeout
    ) {
        super(logger, null);
        m_outerHandler = outerHandler;
        ConnectFuture future = connector.connect(address, new InnerHandler(), config);
        if (!future.join(connectTimeout) || !future.isConnected()) {
            throw new RuntimeException("Connect to " + address + " failed");
        }
        setSession(future.getSession());
    }

    private class InnerHandler implements IoHandler {
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            m_logger.warning("Error on session: " + session.toString(), cause);
            if (m_outerHandler != null) m_outerHandler.exceptionCaught(session, cause);
        }

        public void sessionCreated(IoSession session) throws Exception {
            if (m_outerHandler != null) m_outerHandler.sessionCreated(session);
        }

        public void sessionOpened(IoSession session) throws Exception {
            if (m_outerHandler != null) m_outerHandler.sessionOpened(session);
        }

        public void sessionClosed(IoSession session) throws Exception {
            ASynchSynchClient.this.sessionClosed();
            if (m_outerHandler != null) m_outerHandler.sessionClosed(session);
        }

        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            if (m_outerHandler != null) m_outerHandler.sessionIdle(session, status);
        }

        public void messageSent(IoSession session, Object message) throws Exception {
            if (m_outerHandler != null) m_outerHandler.messageSent(session, message);
        }

        public void messageReceived(IoSession session, Object message) throws Exception {
            resultReceived(message);
            if (m_outerHandler != null) m_outerHandler.messageReceived(session, message);
        }
    }
}
