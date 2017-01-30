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
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolDecoder;
import ru.rd.utils.ObjectFactory;
import static ru.rd.net.MultiDecoder.*;
import static ru.rd.net.MultiEncoder.*;

import java.util.Collection;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 26.09.2008
 * Time: 14:53:28
 */
public class MultiCodecFactory implements ProtocolCodecFactory {
    private final Collection<DecoderData> m_decoders = new LinkedList<DecoderData>();
    private final Collection<EncoderData> m_encoders = new LinkedList<EncoderData>();

    public void registerDecoder(DecoderData dd) {
        m_decoders.add(dd);
    }

    public void registerDecoder(short code, ObjectFactory<? extends MessageDecoder> decoder) {
        m_decoders.add(new DecoderData(code, decoder));
    }

    public void registerDecoder(int code, ObjectFactory<? extends MessageDecoder> decoder) {
        registerDecoder((short)code, decoder);
    }

    public void registerEncoder(EncoderData ed) {
        m_encoders.add(ed);
    }

    public void registerEncoder(Class cls, int code, ObjectFactory<? extends ProtocolEncoder> decoderFactory) {
        registerEncoder(cls, (short)code, decoderFactory);
    }

    public void registerEncoder(Class cls, short code, ObjectFactory<? extends ProtocolEncoder> decoderFactory) {
        m_encoders.add(new EncoderData(cls, code, decoderFactory));
    }

    public ProtocolEncoder getEncoder() throws Exception {
        MultiEncoder e = new MultiEncoder();
        e.registerEncoders(m_encoders);
        return e;
    }

    public ProtocolDecoder getDecoder() throws Exception {
        MultiDecoder d = new MultiDecoder();
        d.registerDecoders(m_decoders);
        return d;
    }
}
