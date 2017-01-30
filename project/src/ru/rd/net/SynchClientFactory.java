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

import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.net.synch.SynchProtocolCodecFactory;
import ru.rd.net.synch.SynchClient;

import java.io.IOException;

/**
 * User: Astepochkin
 * Date: 03.10.2008
 * Time: 17:50:21
 */
public abstract class SynchClientFactory<InputMessage, OutputMessage> implements PoolObjectFactory {
    private final CourierLogger m_logger;
    private final SynchProtocolCodecFactory<InputMessage, OutputMessage> m_codecFactory;
    private final SocketProcessing.CheckFactory<InputMessage> m_checkFactory;

    public SynchClientFactory(
        CourierLogger logger,
        SynchProtocolCodecFactory<InputMessage, OutputMessage> codecFactory,
        SocketProcessing.CheckFactory<InputMessage> checkFactory
    ) {
        m_logger = logger;
        m_codecFactory = codecFactory;
        m_checkFactory = checkFactory;
    }

    protected abstract String getHost();
    protected abstract int getPort();

    public Object getObject(ObjectPoolIntf pool) throws Exception {
        int port = getPort();
        if (port < 0) return null;
        return new SynchClient<InputMessage, OutputMessage>(
            getHost(), port,
            m_codecFactory.getEncoder(), m_codecFactory.getDecoder()
        );
    }

    public void returnObject(Object o) {
        try {
            ((SynchClient)o).close();
        } catch (IOException e) {
            m_logger.warning(e);
        }
    }

    private volatile long m_checkId = 0;
    private long getCheckId() {
        return m_checkId++;
    }

    public boolean checkObject(Object o) {
        try {
            InputMessage min = m_checkFactory.create(getCheckId());
            Object mout = ((SynchClient<InputMessage, OutputMessage>)o).write(min);
            return min.equals(mout);
        } catch (Exception e) {
            m_logger.warning(e);
            return false;
        }
    }
}
