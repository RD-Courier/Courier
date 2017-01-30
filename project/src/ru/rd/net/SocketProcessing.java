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
import ru.rd.net.synch.BufferedSynchClient;
import ru.rd.net.synch.SynchClient;
import ru.rd.net.synch.SynchProtocolCodecFactory;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool2.DefaultObjectPool2;
import ru.rd.pool2.ObjectPool2;
import ru.rd.pool2.PoolListener;
import ru.rd.thread.AsynchProcessing;

import java.util.concurrent.Executor;
import java.util.List;

/**
 * User: STEPOCHKIN
 * Date: 14.09.2008
 * Time: 19:36:23
 */
public class SocketProcessing extends AsynchProcessing<SocketProcessing.Sender, Object> {
    private final Executor m_exec;
    private PoolObjectFactory m_socketFactory;
    private ObjectPool2 m_sockets;

    public interface CheckFactory<InputMessage> {
        InputMessage create(long id);
    }

    public SocketProcessing(
        CourierLogger logger, Executor exec, int maxExec
    ) {
        super(logger, exec, maxExec);
        m_exec = exec;
    }

    public SocketProcessing(
        CourierLogger logger, Executor exec, int maxExec,
        String host, int port,
        SynchProtocolCodecFactory<Object, Object> codecFactory,
        CheckFactory<Object> checkFactory
    ) {
        this(logger, exec, maxExec);
        setSocketFactory(
            new ConstSynchClientFactory<Object, Object>(
                logger, host, port, codecFactory, checkFactory
            )
        );
    }

    public final void setSocketFactory(PoolObjectFactory socketFactory) {
        if (m_sockets != null) {
            m_sockets.close();
            m_sockets = null;
        }
        m_socketFactory = socketFactory;
        m_sockets = new DefaultObjectPool2(getLogger(), "Sockets", m_socketFactory);
        m_sockets.setExecutor(m_exec);

        m_sockets.addListener(new PoolListener() {
            public void objectAdded() { resourceAvailable(); }
        });
        m_sockets.start();
    }

    public final PoolObjectFactory getSocketFactory() {
        return m_socketFactory;
    }

    public boolean close(long timeout) {
        boolean res = super.close(timeout);
        try { m_sockets.close(); } catch (Throwable e) { getLogger().warning(e); }
        return res;
    }

    public interface Sender {
        Object write(Object message) throws Exception;
        void writeBuffered(Object message) throws Exception;
        void close() throws Exception;
    }

    private interface PoolObjectWrapper {
        Object getWrappedObject();
    }

    public static class SynchClientSender
        implements Sender, PoolObjectWrapper {
        private final SynchClient<Object, Object> m_client;

        public SynchClientSender(SynchClient<Object, Object> client) {
            m_client = client;
        }

        public Object write(Object message) throws Exception {
            return m_client.write(message);
        }

        public void writeBuffered(Object message) throws Exception {
            write(message);
        }

        public void close() throws Exception {
            m_client.close();
        }

        public Object getWrappedObject() {
            return m_client;
        }
    }

    public static class SenderFactoryAdapter implements PoolObjectFactory {
        private final PoolObjectFactory m_pof;

        public SenderFactoryAdapter(PoolObjectFactory pof) {
            m_pof = pof;
        }

        public Object getObject(ObjectPoolIntf pool) throws Exception {
            return new SynchClientSender((SynchClient<Object, Object>)m_pof.getObject(pool));
        }

        private static Object wo(Object o) {
            return ((PoolObjectWrapper)o).getWrappedObject();
        }

        public void returnObject(Object o) {
            m_pof.returnObject(wo(o));
        }

        public boolean checkObject(Object o) {
            return m_pof.checkObject(wo(o));
        }
    }

    public static class BufferedSynchClientSender implements Sender, PoolObjectWrapper {
        private final BufferedSynchClient m_client;

        public BufferedSynchClientSender(BufferedSynchClient client) {
            m_client = client;
        }

        public Object write(Object message) throws Exception {
            return m_client.write(message);
        }

        public void writeBuffered(Object message) throws Exception {
            m_client.writeBuffered(message);
        }

        public void close() throws Exception {
            m_client.close();
        }

        public Object getWrappedObject() {
            return m_client;
        }
    }

    public static class BufferedSenderFactoryAdapter implements PoolObjectFactory {
        private final PoolObjectFactory m_pof;

        public BufferedSenderFactoryAdapter(PoolObjectFactory pof) {
            m_pof = pof;
        }

        public Object getObject(ObjectPoolIntf pool) throws Exception {
            return new BufferedSynchClientSender((BufferedSynchClient)m_pof.getObject(pool));
        }

        private static Object wo(Object o) {
            return ((PoolObjectWrapper)o).getWrappedObject();
        }

        public void returnObject(Object o) {
            m_pof.returnObject(wo(o));
        }

        public boolean checkObject(Object o) {
            return m_pof.checkObject(wo(o));
        }
    }


    protected Sender findResource() throws Exception {
        Sender ret = (Sender)m_sockets.peekObject();
        if (ret == null) return null;
        try {
            if (!startClient(ret)) return null;
        } catch (Exception e) {
            try { ret.close(); } catch (Exception e1) { e1.initCause(e); getLogger().warning(e1); }
            throw e;
        }
        return ret;
    }

    protected boolean isResourceValid(Sender resource) throws Exception {
        return m_socketFactory.checkObject(resource);
    }

    protected void releaseResource(Sender resource) throws Exception {
        try { stopClient(resource); } catch (Exception e) { getLogger().warning(e); }
        m_sockets.releaseObject(resource);
    }

    protected void releaseCancelledResource(Sender resource) throws Exception {
        m_sockets.releaseAndRemoveObject(resource);
    }

    protected void process(Sender resource, Object target) throws Exception {
        resource.write(target);
        processedMessage(target);
    }

    protected void process(Sender resource, List<Object> targets) throws Exception {
        resource.write(wrapPortion(targets));
        for (Object o: targets) {
            processedMessage(o);
        }
    }

    protected Object wrapPortion(List<Object> targets) {
        throw new UnsupportedOperationException();
    }

    protected void processedMessage(Object message) {}
    protected boolean startClient(Sender client) throws Exception { return true; }
    protected void stopClient(Sender client) throws Exception {}
}
