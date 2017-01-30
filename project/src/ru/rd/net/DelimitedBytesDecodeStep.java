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

/**
 * User: STEPOCHKIN
 * Date: 30.07.2008
 * Time: 12:41:32
 */
public abstract class DelimitedBytesDecodeStep implements DecodeStep {
    private byte[] m_delimiter;

    public DelimitedBytesDecodeStep(byte[] delimiter) {
        m_delimiter = delimiter;
    }

    public boolean decode(IoSession session, ByteBuffer in, StatedDecoderState state) {
        int iniPos = in.position();
        if (state.getCallCount() == 0) {
            state.auxInt = 0;
            state.setProcessed(0);
        } else {
            in.skip(state.getProcessed());
        }
        int startPos = in.position();
        while (in.hasRemaining()) {
            byte current = in.get();
            if (m_delimiter[state.auxInt] == current) {
                if (state.auxInt + 1 >= m_delimiter.length) {
                    int finPos = in.position();
                    int oldLimit = in.limit();
                    try {
                        in.limit(in.position() - m_delimiter.length);
                        in.position(iniPos);
                        setDecodedProperty(state.getMessage(), in);
                    } finally {
                        in.limit(oldLimit);
                        in.position(finPos);
                    }
                    return true;
                } else {
                    state.auxInt++;
                }
            }
        }
        state.setProcessed(state.getProcessed() + in.position() - startPos);
        in.position(iniPos);
        return false;
    }

    protected abstract void setDecodedProperty(Object message, ByteBuffer in);
}
