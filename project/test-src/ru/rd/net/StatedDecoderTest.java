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
import ru.rd.TestUtils;

import java.nio.charset.Charset;

/**
 * User: STEPOCHKIN
 * Date: 30.07.2008
 * Time: 23:07:07
 */
public class StatedDecoderTest {
    private static final String cCharset = "cp1251";

    public static void main2(String[] args) throws Exception {
        Charset charset = Charset.forName(cCharset);
        Class mesClass = MockMessage.class;
        Object mes = TestUtils.createMockObject(mesClass);
        byte[] data = encodeMessage(mes, charset);
        MockDecoder d = new MockDecoder<MockMessage>(MockMessage.class, charset);
        IoSession session = new MockSession();
        Object mes2 = sendMessage(d, session, data);
        TestUtils.checkObjects(mes, mes2);
    }

    public static void main(String[] args) throws Exception {
        Charset charset = Charset.forName(cCharset);
        Class mesClass = MockMessage.class;
        Object mes = TestUtils.createMockObject(mesClass);
        byte[] data = encodeMessage(mes, charset);
        MockDecoder d = new MockDecoder<MockMessage>(MockMessage.class, charset);
        IoSession session = new MockSession();
        long beginTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            sendMessage(d, session, data);
        }
        System.out.println("Time = " + (System.currentTimeMillis() - beginTime));
    }

    private static byte[] encodeMessage(Object mes, Charset charset) throws Exception {
        MockMessageEncoder e = new MockMessageEncoder(charset);
        IoSession session = new MockSession();
        BufferEncoderOutput eout = new BufferEncoderOutput();
        e.encode(session, mes, eout);
        eout.getBuffer().flip();
        byte[] data = new byte[eout.getBuffer().remaining()];
        eout.getBuffer().get(data);
        return data;
    }

    private static Object sendMessage(MessageDecoder d, IoSession session, byte[] data) throws Exception {
        int partlength = 100;
        Object r = null;
        for (int p = 0; p < data.length; p += partlength) {
            r = d.decode(
                session,
                ByteBuffer.wrap(data, p, Math.min(partlength, data.length - p))
            );
            if (p >= data.length - 1) {
                if (r == null) throw new RuntimeException("Should be OK result");
            } else {
                if (r != null) throw new RuntimeException("Should be NEED_DATA result");
            }
        }
        return r;
    }
}
