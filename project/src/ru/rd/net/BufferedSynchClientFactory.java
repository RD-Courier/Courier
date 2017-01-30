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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.net.synch.SynchProtocolCodecFactory;
import ru.rd.net.synch.BufferedSynchClient;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.ObjectPoolIntf;

import java.util.Timer;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * User: AStepochkin
 * Date: 23.04.2009
 * Time: 17:12:59
 */
public abstract class BufferedSynchClientFactory implements PoolObjectFactory {
    private final CourierLogger m_logger;
    private final Timer m_timer;
    private final Executor m_exec;
    private final SynchProtocolCodecFactory<Object, Object> m_codecFactory;
    private final SocketProcessing.CheckFactory<Object> m_checkFactory;
    private long m_bufInterval;


    public BufferedSynchClientFactory(
        CourierLogger logger, Timer timer, Executor exec,
        SynchProtocolCodecFactory<Object, Object> codecFactory,
        SocketProcessing.CheckFactory<Object> checkFactory
    ) {
        m_logger = logger;
        m_timer = timer;
        m_exec = exec;
        m_codecFactory = codecFactory;
        m_checkFactory = checkFactory;
        m_bufInterval = 0;
    }

    public final void setBufferInterval(long value) {
        m_bufInterval = value;
    }

    protected abstract String getHost();
    protected abstract int getPort();
    protected abstract Object wrapData(List<Object> data);

    public Object getObject(ObjectPoolIntf pool) throws Exception {
        int port = getPort();
        if (port < 0) return null;
        BufferedSynchClient ret = new BufferedSynchClient(
            m_logger, m_timer, m_exec, 
            getHost(), port,
            m_codecFactory.getEncoder(), m_codecFactory.getDecoder(), 1024 * 4
        ) {
            protected Object wrapData(List<Object> data) {
                return BufferedSynchClientFactory.this.wrapData(data);
            }
        };
        ret.setBufferInterval(m_bufInterval);
        return ret;
    }

    public void returnObject(Object o) {
        try {
            ((BufferedSynchClient)o).close();
        } catch (Exception e) {
            m_logger.warning(e);
        }
    }

    private volatile long m_checkId = 0;
    private long getCheckId() {
        return m_checkId++;
    }

    public boolean checkObject(Object o) {
        try {
            Object min = m_checkFactory.create(getCheckId());
            Object mout = ((BufferedSynchClient)o).write(min);
            return min.equals(mout);
        } catch (Exception e) {
            m_logger.warning(e);
            return false;
        }
    }
}
