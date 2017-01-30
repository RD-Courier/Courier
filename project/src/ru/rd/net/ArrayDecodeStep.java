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
import org.apache.mina.common.ByteBuffer;

import java.util.List;
import java.util.ArrayList;

/**
 * User: AStepochkin
 * Date: 24.04.2009
 * Time: 15:19:35
 */
public abstract class ArrayDecodeStep<MessageType> implements DecodeStep {
    private final MessageDecoder m_decoder;
    private List<MessageType> m_array = null;
    private int m_length = 0;
    private int m_index = 0;

    public ArrayDecodeStep(MessageDecoder decoder) {
        m_decoder = decoder;
    }

    public boolean decode(IoSession session, ByteBuffer in, StatedDecoderState state) throws Exception {
        if (m_array == null) {
            if (in.remaining() < 4) {
                return false;
            }
            //noinspection unchecked
            m_length = in.getInt();
            m_array = new ArrayList<MessageType>(m_length);
            m_index = 0;
        }
        for (;;) {
            if (m_index >= m_length) {
                setDecodedProperty(state.getMessage(), m_array);
                m_array = null;
                return true;
            }
            //noinspection unchecked
            MessageType item = (MessageType)m_decoder.decode(session, in);
            if (item == null) break;
            m_array.add(item);
            m_index++;
        }
        return false;
    }

    protected abstract void setDecodedProperty(Object message, List<MessageType> value);
}
