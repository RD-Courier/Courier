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
package ru.rd.net.synch;

import org.apache.mina.common.ByteBuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * User: STEPOCHKIN
 * Date: 15.09.2008
 * Time: 0:14:23
 */
public class SynchMultiEncoder implements SynchEncoder<Object> {
    public static class EncoderValue {
        public final SynchEncoder encoder;
        public final short code;

        public EncoderValue(SynchEncoder encoder, short code) {
            this.encoder = encoder;
            this.code = code;
        }
    }

    private final Map<Class, EncoderValue> m_encoders = new HashMap<Class, EncoderValue>();

    public SynchMultiEncoder() {}

    public void registerEncoder(Class cls, SynchEncoder encoder, short code) {
        m_encoders.put(cls, new EncoderValue(encoder, code));
    }

    public void registerEncoder(Class cls, SynchEncoder encoder, int code) {
        registerEncoder(cls, encoder, (short)code);
    }

    public void encode(Object message, SynchEncoderOutput output) throws Exception {
        if (message instanceof ByteBuffer) {
            output.write((ByteBuffer)message);
            return;
        }
        EncoderValue ev = m_encoders.get(message.getClass());
        if (ev == null) throw new RuntimeException("Encoder for " + message.getClass().getName() + " not found");
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort(ev.code);
        buffer.flip();
        output.write(buffer);
        //noinspection unchecked
        ev.encoder.encode(message, output);
    }
}
