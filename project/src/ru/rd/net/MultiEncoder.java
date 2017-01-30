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

import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import ru.rd.utils.ObjectFactory;

/**
 * User: STEPOCHKIN
 * Date: 15.09.2008
 * Time: 1:13:10
 */
public class MultiEncoder extends BufferedEncoder {
    private static class EncoderEntry {
        public final ObjectFactory<? extends ProtocolEncoder> factory;
        public final short code;
        public ProtocolEncoder encoder;

        public EncoderEntry(ObjectFactory<? extends ProtocolEncoder> factory, short code) {
            this.factory = factory;
            this.code = code;
            encoder = null;
        }
    }

    public static class EncoderData {
        public final Class cls;
        public final short code;
        public final ObjectFactory<? extends ProtocolEncoder> factory;

        public EncoderData(Class cls, short code, ObjectFactory<? extends ProtocolEncoder> decoderFactory) {
            this.cls = cls;
            this.code = code;
            this.factory = decoderFactory;
        }
    }

    private final Map<Class, EncoderEntry> m_encoders = new HashMap<Class, EncoderEntry>();

    public final void registerEncoders(Collection<EncoderData> encoders) {
        for (EncoderData d: encoders) {
            registerEncoder(d.cls, d.code, d.factory);
        }
    }

    public void registerEncoder(Class cls, short code, ObjectFactory<? extends ProtocolEncoder> encoderFactory) {
        m_encoders.put(cls, new EncoderEntry(encoderFactory, code));
    }

    public void registerEncoder(Class cls, short code, Class<? extends ProtocolEncoder> fcls) throws NoSuchMethodException {
        registerEncoder(cls, code, new ReflectFactory<ProtocolEncoder>(fcls));
    }

    public void registerEncoder(Class cls, short code, Class<? extends ProtocolEncoder> fcls, Object[] params) throws NoSuchMethodException {
        registerEncoder(cls, code, new ReflectFactory<ProtocolEncoder>(fcls, params));
    }

    public void registerEncoder(
        Class cls, short code, Class<? extends ProtocolEncoder> fcls, Class[] sig, Object[] params
    ) throws NoSuchMethodException {
        registerEncoder(cls, code, new ReflectFactory<ProtocolEncoder>(fcls, sig, params));
    }

    protected void encodeToBufferOutput(IoSession session, Object message, BufferEncoderOutput out) throws Exception {
        EncoderEntry ev = m_encoders.get(message.getClass());
        if (ev == null) throw new RuntimeException("Encoder for " + message.getClass().getName() + " not found");
        //System.out.println("Encoder for " + message.getClass().getName() + " Code = " + ev.code);
        out.getBuffer().putShort(ev.code);
        if (ev.encoder == null) {
            ev.encoder = ev.factory.create();
        }
        ev.encoder.encode(session, message, out);
    }

    public void dispose(IoSession session) throws Exception {
        for (EncoderEntry ee: m_encoders.values()) {
            if (ee.encoder != null) ee.encoder.dispose(session);
        }
    }
}
