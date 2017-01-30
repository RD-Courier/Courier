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

import ru.rd.courier.logging.ConsoleCourierLogger;
import ru.rd.utils.TimeElapsed;
import ru.rd.net.message.CommonAnswerDecoder;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * User: STEPOCHKIN
 * Date: 12.09.2008
 * Time: 14:03:05
 */
public class SynchClientTest {
    public static void main(String[] args) throws Exception {
        Charset charset = Charset.forName("cp1251");

        ASynchSynchClientFactory sf = new ASynchSynchClientFactory(
            new ConsoleCourierLogger("Test"),
            new InetSocketAddress("127.0.0.1", 4444), 1, 10,
            new ConstProtocolCodecFactory(
                new MockMessageEncoder(charset),
                new MessageToProtocolDecoderAdapter(new CommonAnswerDecoder(charset))
            )
        );

        ASynchSynchClient sc = (ASynchSynchClient)sf.getObject(null);

        final int c_messageCount = 10000;
        MockMessage pr = MockMessage.createTestMessage();
        TimeElapsed te = new TimeElapsed();
        for (int i = 0; i < c_messageCount; i++) {
            //CommonAnswer res = (CommonAnswer)
            sc.write(pr, 10000);
            //System.out.println("Answer.error = '" + res.error + "'");
        }
        System.out.println("Time = " + te.elapsed());
        sc.close(1);
    }
}
