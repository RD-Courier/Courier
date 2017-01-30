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

/**
 * User: STEPOCHKIN
 * Date: 12.09.2008
 * Time: 20:52:10
 */
public class SynchCumulativeDecoder<Message> implements SynchDecoder<Message> {
    private final SynchDecoder<Message> m_decoder;
    private ByteBuffer m_buffer;

    public SynchCumulativeDecoder(SynchDecoder<Message> decoder) {
        m_decoder = decoder;
        m_buffer = null;
    }

    public Message decode(ByteBuffer in) throws Exception {
        boolean usingBuffer;
        ByteBuffer buf = m_buffer;
        if (buf != null) {
            buf.put(in);
            buf.flip();
            usingBuffer = true;
        } else {
            buf = in;
            usingBuffer = false;
        }

        int oldPos = buf.position();
        Message result = m_decoder.decode(buf);
        if (result != null) {
            if (buf.position() == oldPos) {
                throw new IllegalStateException(
                        "doDecode() can't return result when buffer is not consumed");
            }
        }

        if (buf.hasRemaining()) {
            if (usingBuffer) {
                buf.compact();
            } else {
                storeRemainingInSession(buf);
            }
        } else {
            if (usingBuffer) removeSessionBuffer();
        }
        return result;
    }

    private void removeSessionBuffer() {
        if (m_buffer != null) {
            m_buffer.release();
            m_buffer = null;
        }
    }

    private void storeRemainingInSession(ByteBuffer buf) {
        ByteBuffer remainingBuf = ByteBuffer.allocate(buf.capacity());
        remainingBuf.setAutoExpand(true);
        remainingBuf.order(buf.order());
        remainingBuf.put(buf);
        m_buffer = remainingBuf;
    }
}
