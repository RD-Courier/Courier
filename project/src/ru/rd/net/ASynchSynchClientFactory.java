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

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.net.ASynchSynchClient;
import ru.rd.courier.logging.CourierLogger;

import java.net.SocketAddress;

/**
 * User: STEPOCHKIN
 * Date: 11.09.2008
 * Time: 23:08:34
 */
public class ASynchSynchClientFactory implements PoolObjectFactory {
    private final CourierLogger m_logger;
    private final long m_connectTimeout;
    private final SocketAddress m_address;
    private final SocketConnector m_connector;
    private final SocketConnectorConfig m_cfg;

    public ASynchSynchClientFactory(
        CourierLogger logger,
        SocketAddress address, int workerTimeout, int connectTimeout,
        ProtocolCodecFactory codecFactory
    ) {
        m_logger = logger;
        m_address = address;
        m_connectTimeout = connectTimeout;
        m_connector = new SocketConnector();
        m_connector.setWorkerTimeout(workerTimeout);
        m_cfg = new SocketConnectorConfig();
        m_cfg.setConnectTimeout(connectTimeout);
        m_cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(codecFactory));
    }

    public Object getObject(ObjectPoolIntf pool) throws Exception {
        return new ASynchSynchClient(
            m_logger, m_connector, m_address, m_cfg, null, m_connectTimeout
        );
    }

    public void returnObject(Object o) {
        ((ASynchSynchClient)o).close(m_connectTimeout);
    }

    public boolean checkObject(Object o) {
        return ((ASynchSynchClient)o).isValid();
    }
}
