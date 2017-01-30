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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import ru.rd.utils.ObjectFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: STEPOCHKIN
 * Date: 15.09.2008
 * Time: 1:05:39
 */
public class MultiDecoder implements ProtocolDecoder {
    private static class DecoderEntry {
        public final ObjectFactory<? extends MessageDecoder> factory;
        public MessageDecoder decoder;

        public DecoderEntry(ObjectFactory<? extends MessageDecoder> factory) {
            this.factory = factory;
            decoder = null;
        }
    }

    public static class DecoderData {
        public final short code;
        public final ObjectFactory<? extends MessageDecoder> decoderFactory;

        public DecoderData(short code, ObjectFactory<? extends MessageDecoder> decoderFactory) {
            this.code = code;
            this.decoderFactory = decoderFactory;
        }
    }

    private final Map<Short, DecoderEntry> m_decoders = new HashMap<Short, DecoderEntry>();

    public final void registerDecoders(Collection<DecoderData> decoders) {
        for (DecoderData dd: decoders) {
            registerDecoder(dd.code, dd.decoderFactory);
        }
    }

    public final void registerDecoder(short code, ObjectFactory<? extends MessageDecoder> decoder) {
        m_decoders.put(code, new DecoderEntry(decoder));
    }

    public final void registerDecoder(short code, Class<? extends MessageDecoder> fcls) throws NoSuchMethodException {
        registerDecoder(code, new ReflectFactory<MessageDecoder>(fcls));
    }

    public final void registerDecoder(short code, Class<? extends MessageDecoder> fcls, Object[] params) throws NoSuchMethodException {
        registerDecoder(code, new ReflectFactory<MessageDecoder>(fcls, params));
    }

    public final void registerDecoder(short code, Class<? extends MessageDecoder> fcls, Class[] sig, Object[] params) throws NoSuchMethodException {
        registerDecoder(code, new ReflectFactory<MessageDecoder>(fcls, sig, params));
    }

    private DecoderEntry m_de;

    public final void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
        for (;;) {
            if (m_de == null) {
                if (in.remaining() < 4) break;
                short code = in.getShort();
                m_de = m_decoders.get(code);
                if (m_de == null) throw new RuntimeException("Decoder for code " + code + " not found");
                if (m_de.decoder == null) {
                    m_de.decoder = m_de.factory.create();
                }
            }
            Object r = m_de.decoder.decode(session, in);
            if (r == null) break;
            out.write(r);
            m_de = null;
        }
    }

    public final void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
        if (m_de == null) return;
        Object obj = m_de.decoder.finishDecode(session);
        if (obj != null) out.write(obj);
    }

    public void dispose(IoSession session) throws Exception {
        for (DecoderEntry de: m_decoders.values()) {
            if (de.decoder != null) de.decoder.dispose(session);
        }
    }
}
