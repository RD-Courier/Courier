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

import java.util.Map;
import java.util.HashMap;

/**
 * User: STEPOCHKIN
 * Date: 15.09.2008
 * Time: 0:52:46
 */
public class SynchMultiDecoder<T> implements SynchDecoder<T> {
    private SynchDecoder<? extends T> m_decoder;
    private final Map<Short, SynchDecoder<? extends T>> m_decoders = new HashMap<Short, SynchDecoder<? extends T>>();

    public SynchMultiDecoder() {
        m_decoder = null;
    }

    public void registerDecoder(short code, SynchDecoder<? extends T> decoder) {
        m_decoders.put(code, decoder);
    }

    public void registerDecoder(int code, SynchDecoder<? extends T> decoder) {
        registerDecoder((short)code, decoder);
    }

    public T decode(ByteBuffer in) throws Exception {
        if (m_decoder == null) {
            if (in.remaining() < 2) return null;
            short code = in.getShort();
            m_decoder = m_decoders.get(code);
            if (m_decoder == null)
                throw new RuntimeException("Decoder for code " + code + " not found");
        }
        T res = m_decoder.decode(in);
        if (res != null) m_decoder = null;
        return res;
    }
}
