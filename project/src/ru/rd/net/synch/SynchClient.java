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
import ru.rd.net.NetUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * User: STEPOCHKIN
 * Date: 14.09.2008
 * Time: 20:25:54
 */
public class SynchClient<InputMessage, OutputMessage> {
    private final Socket m_socket;
    private final InputStream m_in;
    private final OutputStream m_out;
    private final SynchEncoder<InputMessage> m_encoder;
    private final SynchDecoder<OutputMessage> m_decoder;

    public SynchClient(
        String host, int port,
        SynchEncoder<InputMessage> encoder, SynchDecoder<OutputMessage> decoder
    ) throws IOException {
        m_socket = new Socket(host, port);
        m_in = m_socket.getInputStream();
        m_out = m_socket.getOutputStream();
        m_encoder = encoder;
        m_decoder = decoder;
    }

    public Socket getSocket() {
        return m_socket;
    }

    private void debug(String message) {
        System.out.println(message);
    }

    private void writeDebug(InputMessage message, String num, String extra) {
        StringBuffer m = new StringBuffer(
            "SynchClient.write " + num + ": "
            + message.getClass().getSimpleName() + "(" + message + ")"
        );
        if (extra != null && extra.length() > 0) {
            m.append(" ").append(extra);
        }
        debug(m.toString());
    }

    public OutputMessage write(InputMessage message) throws Exception {
        final byte[] buffer = new byte[8*1024];
        //writeDebug(message, "10", null);
        m_encoder.encode(
            message,
            new SynchEncoderOutput() {
                public void write(ByteBuffer buf) throws IOException {
                    NetUtils.bufToStream(buffer, buf, m_out);
                }
            }
        );
        OutputMessage result = null;
        for (;;) {
            //writeDebug(message, "20", null);
            int count = m_in.read(buffer);
            //writeDebug(message, "30", "count = " + count);
            if (count < 0) break;
            if (count > 0) {
                result = m_decoder.decode(ByteBuffer.wrap(buffer, 0, count));
                if (result != null) break;
            }
        }
        return result;
    }

    public void close() throws IOException {
        m_socket.close();
    }
}
