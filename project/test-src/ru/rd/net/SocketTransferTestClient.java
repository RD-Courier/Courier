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
import ru.rd.TestUtils;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.net.synch.*;
import ru.rd.utils.TimeElapsed;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * User: AStepochkin
 * Date: 09.10.2008
 * Time: 16:05:40
 */
public class SocketTransferTestClient {
    public static void main(String[] args) throws Exception {
        CourierLogger logger = TestUtils.getTestLogger("SocketTransferTestServer.log");
        Socket s = new Socket("127.0.0.1", 4444);
        try {
            useSocket(logger, s.getInputStream(), s.getOutputStream());
        } finally {
            s.close();
        }
    }
    private static void useSocket(CourierLogger logger, InputStream in, OutputStream out) throws Exception {
        final int ShoudBeSent = 512*1024;
        final int bufsize = 1024;
        MockMessage pr = MockMessage.createTestMessage();
        SynchProtocolCodecFactory<Object, Object> codec = buildStatCodecFactory();
        SynchEncoder<Object> encoder = codec.getEncoder();
        SynchDecoder<Object> decoder = codec.getDecoder();
        byte[] buffer = new byte[bufsize];
        int totalSent = 0;
        TimeElapsed te = new TimeElapsed();
        BufferEncoderOutput eout = new BufferEncoderOutput();
        try {
            for (int i = 0; i < 10000; i++) {
                int count;
                eout.reset();
                encoder.encode(pr, eout);
                ByteBuffer buf = eout.getBuffer();
                buf.flip();
                count = buf.remaining();
                buf.get(buffer, 0, count);
                out.write(buffer, 0, count);
                totalSent += count;
                while ((count = in.read(buffer)) >= 0) {
                    Object answer = decoder.decode(ByteBuffer.wrap(buffer, 0, count));
                    if (answer != null) break;
                }
            }
            logger.info("Bytes sent: " + totalSent + " TimeElapsed: " + te.elapsed());
        } finally {
            in.close();
            out.close();
        }
    }

    public static SynchProtocolCodecFactory<Object, Object> buildStatCodecFactory() {
        final Charset charset = Charset.forName("cp1251");

        return new SynchProtocolCodecFactory<Object, Object>() {
            public SynchEncoder<Object> getEncoder() throws Exception {
                SynchMultiEncoder encoder = new SynchMultiEncoder();
                encoder.registerEncoder(
                    MockMessage.class, new MockMessageEncoder(charset), 1
                );
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
