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

import ru.rd.net.synch.*;
import ru.rd.net.message.CommonAnswer;
import ru.rd.net.message.CommonAnswerDecoderSynch;
import ru.rd.net.message.CommonAnswerEncoder;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.utils.ObjectFactory;
import ru.rd.utils.TimeElapsed;
import ru.rd.TestUtils;

import java.nio.charset.Charset;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;

/**
 * User: AStepochkin
 * Date: 10.10.2008
 * Time: 9:17:10
 */
public class DecoderPerfTest {
    public static void main(String[] args) throws Exception {
        CourierLogger logger = TestUtils.getTestLogger("DecoderPerfTest.log");
        SynchProtocolCodecFactory<Object, Object> codecs1 = buildStatCodecFactory();
        SynchEncoder<Object> encoder = codecs1.getEncoder();
        ProtocolCodecFactory codecs2 = buildCodec();
        ProtocolDecoder decoder = codecs2.getDecoder();
        //BufferEncoderOutput eout = new BufferEncoderOutput();
        MockDecoderOutput dout = new MockDecoderOutput();
        MockMessage pr = MockMessage.createTestMessage();
        MockSession session = new MockSession();
        int totalSent = 0;
        TimeElapsed te = new TimeElapsed();
        for (int i = 0; i < 10000; i++) {
            //eout.reset();
            BufferEncoderOutput eout = new BufferEncoderOutput();
            encoder.encode(pr, eout);
            ByteBuffer buffer = eout.getBuffer();
            buffer.flip();
            totalSent += buffer.remaining();
            decoder.decode(session, buffer, dout);
        }
        logger.info("Bytes sent: " + totalSent + " TimeElapsed: " + te.elapsed());
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
                decoder.registerDecoder(1, new CommonAnswerDecoderSynch(charset));
                return new SynchCumulativeDecoder<Object>(decoder);
            }
        };
    }

    private static ProtocolCodecFactory buildCodec() throws NoSuchMethodException {
        final Charset charset = Charset.forName("cp1251");
        MultiCodecFactory codec = new MultiCodecFactory();
        codec.registerEncoder(
            CommonAnswer.class, 1,
            ReflectFactory.factoryOnCharset(CommonAnswerEncoder.class, charset)
        );

        codec.registerDecoder(
            1,
            new ObjectFactory<MessageDecoder>() {
                public MessageDecoder create() throws Exception {
                    return new MockDecoder<MockMessage>(MockMessage.class, charset) {
                        protected void customProcessing(IoSession session, MockMessage message) throws Exception {
                            session.write(new CommonAnswer());
                        }
                    };
                }
            }
        );
        return new CumulativeDecoderProtocolFactory(codec);
    }
}
