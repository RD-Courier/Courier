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
import ru.rd.courier.manager.message.ProcessResultEncoder;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.courier.manager.message.ProcessResultDecoder;
import ru.rd.net.BufferEncoderOutput;
import ru.rd.net.MockMessage;
import ru.rd.net.MockMessageEncoder;
import ru.rd.net.MockMessageDecoder;
import ru.rd.net.synch.*;

import java.nio.charset.Charset;

/**
 * User: AStepochkin
 * Date: 25.09.2008
 * Time: 16:01:25
 */
public class ClientCodecsTest {
    public static void main(String[] args) throws Exception {
        SynchProtocolCodecFactory<Object, Object> cf = buildCodecFactory();
        SynchEncoder<Object> encoder = cf.getEncoder();
        SynchDecoder<Object> decoder = cf.getDecoder();
        BufferEncoderOutput output = new BufferEncoderOutput();
        MockMessage pr = MockMessage.createTestMessage();
        encoder.encode(pr, output);
        MockMessage pr2 = (MockMessage)sendMessage(
            decoder, output.getBuffer().flip(), 1
        );
        if (!pr.equals(pr2)) throw new RuntimeException();
    }

    private static Object sendMessage(
        SynchDecoder<Object> d, ByteBuffer data, int partlength
    ) throws Exception {
        byte[] buffer = new byte[partlength];
        Object r = null;
        while (data.hasRemaining()) {
            int count = Math.min(partlength, data.remaining());
            data.get(buffer, 0, count);
            r = d.decode(ByteBuffer.wrap(buffer, 0, count));
            if (r != null) {
                if (data.hasRemaining()) {
                    throw new RuntimeException("Object decoded but buffer is not empty: remaining = " + data.remaining());
                }
                break;
            }
        }
        return r;
    }

    private static SynchProtocolCodecFactory<Object, Object> buildCodecFactory() {
        final Charset charset = Charset.forName("cp1251");

        return new SynchProtocolCodecFactory<Object, Object>() {
            public SynchEncoder<Object> getEncoder() throws Exception {
                SynchMultiEncoder encoder = new SynchMultiEncoder();
                encoder.registerEncoder(MockMessage.class, new MockMessageEncoder(charset), 1);
                return encoder;
            }

            public SynchDecoder<Object> getDecoder() throws Exception {
                SynchMultiDecoder<Object> decoder = new SynchMultiDecoder<Object>();
                decoder.registerDecoder(1, new MessageToSynchDecoderAdapter(new MockMessageDecoder(charset)));
                return new SynchCumulativeDecoder<Object>(decoder);
            }
        };
    }
}
