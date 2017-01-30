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
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import ru.rd.net.BufferEncoder;

import java.util.List;

/**
 * User: AStepochkin
 * Date: 26.09.2008
 * Time: 13:14:47
 */
public abstract class ExpandedSynchEncoder<Message> implements BufferEncoder<Message>, SynchEncoder<Message>, ProtocolEncoder {
    public final void encode(Message message, SynchEncoderOutput output) throws Exception {
        output.write(getEncodedBuffer(message));
    }

    public final ByteBuffer getEncodedBuffer(Message message) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        buffer.setAutoExpand(true);
        encodeToBuffer(message, buffer);
        buffer.flip();
        return buffer;
    }

    public abstract void encodeToBuffer(Message message, ByteBuffer buffer) throws Exception;

    public final void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        //noinspection unchecked
        out.write(getEncodedBuffer((Message)message));
    }

    public void dispose(IoSession session) throws Exception {}

    public static <Message> void encodeArray(Message[] array, ByteBuffer buffer, BufferEncoder<Message> encoder) throws Exception {
        buffer.putInt(array.length);
        for (Message item: array) {
            encoder.encodeToBuffer(item, buffer);
        }
    }

    public static <Message> void encodeArray(List<Message> array, ByteBuffer buffer, BufferEncoder<Message> encoder) throws Exception {
        buffer.putInt(array.size());
        for (Message item: array) {
            encoder.encodeToBuffer(item, buffer);
        }
    }
}
