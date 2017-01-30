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
package ru.rd.courier.manager;

import ru.rd.courier.manager.message.*;
import ru.rd.courier.stat.StatProcessingTest;
import ru.rd.net.synch.*;
import ru.rd.net.message.*;

import java.nio.charset.Charset;

/**
 * User: AStepochkin
 * Date: 26.09.2008
 * Time: 17:12:12
 */
public class ClientTest {
    public static void main(String[] args) throws Exception {
        Charset charset = Charset.forName("cp1251");
        SynchMultiEncoder encoder = new SynchMultiEncoder();
        encoder.registerEncoder(
            CourierInfoMessage.class, new CourierInfoMessageEncoder(charset), 1
        );
        encoder.registerEncoder(NullMessage.class, new NullMessageEncoder(), 2);

        SynchMultiDecoder<Object> decoder = new SynchMultiDecoder<Object>();
        decoder.registerDecoder(1, ManagerInfoMessageDecoder.createSynchDecoder(charset));
        decoder.registerDecoder(2, new CommonAnswerDecoderSynch(charset));

        SynchMultiEncoder sencoder = new SynchMultiEncoder();
        sencoder.registerEncoder(
            ProcessResult.class, new ProcessResultEncoder(charset), 1
        );
        sencoder.registerEncoder(CheckMessage.class, new CheckMessageEncoderSynch(), 2);
        sencoder.registerEncoder(ManagerInfoMessage.class, new ManagerInfoMessageEncoder(charset), 3);

        SynchMultiDecoder<Object> sdecoder = new SynchMultiDecoder<Object>();
        sdecoder.registerDecoder(1, new CommonAnswerDecoderSynch(charset));
        sdecoder.registerDecoder(2, new CheckMessageDecoderSynch());

        SynchClient<Object, Object> client = new SynchClient<Object, Object>(
            "127.0.0.1", 4444, encoder, decoder
        );
        ManagerInfoMessage mim = (ManagerInfoMessage)client.write(new CourierInfoMessage("MyTestConfig.xml"));
        SynchClient<Object, Object> sclient = new SynchClient<Object, Object>(
            "127.0.0.1", mim.getStatPort(), sencoder, sdecoder
        );
        sclient.write(mim);
        System.out.print("Sleep: ");
        for (int ci = 0; ci < 10; ci++) {
            for (int pri = 0; pri < 10; pri++) {
                sclient.write(StatProcessingTest.createResult());
                Thread.sleep(10);
            }
            Thread.sleep(500);
            System.out.print('.');
        }
        System.out.println();
        CommonAnswer stopAnswer = (CommonAnswer)client.write(new NullMessage());
        client.close();
    }
}
