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

import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.WriteFuture;
import ru.rd.net.synch.SynchEncoderOutput;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 9:33:52
 */
public class BufferEncoderOutput implements ProtocolEncoderOutput, SynchEncoderOutput {
    private ByteBuffer m_data;

    public BufferEncoderOutput() {
        m_data = ByteBuffer.allocate(0, false);
        m_data.setAutoExpand(true);
    }

    public void write(ByteBuffer buf) {
        m_data.put(buf);
    }

    public void mergeAll() {}

    public WriteFuture flush() {
        return null;
    }

    public ByteBuffer getBuffer() {
        return m_data;
    }

    public void reset() {
        m_data.position(0);
    }

    public ByteBuffer flip() {
        return m_data.flip();
    }
}
