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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.manager.message.*;
import ru.rd.net.BufferedSynchClientFactory;
import ru.rd.net.SocketProcessing;
import ru.rd.net.SynchClientFactory;
import ru.rd.net.message.*;
import ru.rd.net.synch.*;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Timer;
import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * User: STEPOCHKIN
 * Date: 15.09.2008
 * Time: 1:28:29
 */
public class StatProcessing extends SocketProcessing {
    protected final Object m_lock;
    private final String m_host;
    private volatile ManagerInfo m_minfo;

    public StatProcessing(
        CourierLogger logger, Executor exec, int maxExec, String host
    ) {
        super(logger, exec, maxExec);
        m_lock = new Object();
        m_host = host;
        m_minfo = null;
        Charset charset = Charset.forName("cp1251");
        SocketFactory socketFactory = new SocketFactory(
            logger, buildStatCodecFactory(charset), new StatCheckFactory()
        );
        setBufferProperties(30000, 100);
        setSocketFactory(new SenderFactoryAdapter(socketFactory));
    }

    public void setManagerInfo(ManagerInfo minfo) {
        boolean avail;
        synchronized(m_lock) {
            m_minfo = minfo;
            avail = minfo != null;
        }
        if (avail) resourceAvailable();
    }

    protected boolean startClient(Sender client) throws Exception {
        CommonAnswer ca = (CommonAnswer)client.write(m_minfo);
        if (ca == null) return false;
        ca.checkError();
        return true;
    }

    /*
    protected void process(SynchClient<Object, Object> resource, List<Object> targets) throws Exception {
        m_logger.debug("StatProcessing.processList: size = " + targets.size());
        BufferEncoderOutput out = new BufferEncoderOutput();
        for (Object item: targets) {
            out.getBuffer().putInt(1);
            m_prEncoder.encode((ProcessResult)item, out);
        }
        resource.write(out.flip());
    }
    */

    protected void stopClient(Sender client) {}

    protected void processedMessage(Object message) {
        if (message instanceof ProcessResult) {
            processedProcessResult((ProcessResult)message);
        }
    }

    protected void processedProcessResult(ProcessResult message) {
    }

    private static class StatCheckFactory implements CheckFactory<Object> {
        public Object create(long id) {
            return new CheckMessage(id);
        }
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private class SocketFactory extends SynchClientFactory<Object, Object> {
        public SocketFactory(
            CourierLogger logger,
            SynchProtocolCodecFactory<Object, Object> codecFactory,
            CheckFactory<Object> checkFactory
        ) {
            super(logger, codecFactory, checkFactory);
        }

        protected String getHost() {
            return m_host;
        }

        protected int getPort() {
            synchronized(m_lock) {
                if (m_minfo == null) return -1;
                return m_minfo.getStatPort();
            }
        }
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private class BufferedSocketFactory extends BufferedSynchClientFactory {
        public BufferedSocketFactory(
            CourierLogger logger, Timer timer, Executor exec,
            SynchProtocolCodecFactory<Object, Object> codecFactory,
            CheckFactory<Object> checkFactory
        ) {
            super(logger, timer, exec, codecFactory, checkFactory);
        }

        protected String getHost() {
            return m_host;
        }

        protected int getPort() {
            synchronized(m_lock) {
                if (m_minfo == null) return -1;
                return m_minfo.getStatPort();
            }
        }

        protected Object wrapData(List<Object> data) {
            return wrapPortion(data);
        }
    }

    protected Object wrapPortion(List<Object> data) {
        ProcessResultArray wrapper = new ProcessResultArray();
        List<ProcessResult> rdata = new ArrayList<ProcessResult>(data.size());
        for (Object o: data) {
            rdata.add((ProcessResult)o);
        }
        wrapper.setResults(rdata);
        return wrapper;
    }

    public static SynchProtocolCodecFactory<Object, Object> buildStatCodecFactory(final Charset charset) {
        return new SynchProtocolCodecFactory<Object, Object>() {
            public SynchEncoder<Object> getEncoder() throws Exception {
                SynchMultiEncoder encoder = new SynchMultiEncoder();
                encoder.registerEncoder(
                    ProcessResult.class, new ProcessResultEncoder(charset), 1
                );
                encoder.registerEncoder(CheckMessage.class, new CheckMessageEncoderSynch(), 2);
                encoder.registerEncoder(ManagerInfoMessage.class, new ManagerInfoMessageEncoder(charset), 3);
                encoder.registerEncoder(
                    ProcessResultArray.class, new ProcessResultArrayEncoder(charset), 4
                );
                return encoder;
            }

            public SynchDecoder<Object> getDecoder() throws Exception {
                SynchMultiDecoder<Object> decoder = new SynchMultiDecoder<Object>();
                decoder.registerDecoder(1, new CommonAnswerDecoderSynch(charset));
                decoder.registerDecoder(2, new CheckMessageDecoderSynch());
                return new SynchCumulativeDecoder<Object>(decoder);
            }
        };
    }

    public static SynchProtocolCodecFactory<Object, Object> buildMainCodecFactory() {
        final Charset charset = Charset.forName("cp1251");

        return new SynchProtocolCodecFactory<Object, Object>() {
            public SynchEncoder<Object> getEncoder() throws Exception {
                SynchMultiEncoder encoder = new SynchMultiEncoder();
                encoder.registerEncoder(
                    CourierInfoMessage.class, new CourierInfoMessageEncoder(charset), 1
                );
                encoder.registerEncoder(NullMessage.class, new NullMessageEncoder(), 2);
                return encoder;
            }

            public SynchDecoder<Object> getDecoder() throws Exception {
                SynchMultiDecoder<Object> decoder = new SynchMultiDecoder<Object>();
                decoder.registerDecoder(1, ManagerInfoMessageDecoder.createSynchDecoder(charset));
                decoder.registerDecoder(2, new CommonAnswerDecoderSynch(charset));
                return decoder;
            }
        };
    }
}
