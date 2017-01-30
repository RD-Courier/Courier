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

import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import ru.rd.net.message.ActionMessage;

/**
 * User: STEPOCHKIN
 * Date: 20.10.2008
 * Time: 1:22:28
 */
public abstract class ActionReceiver<T> {
    private final ProtocolDecoder m_decoder;
    private final ProtocolDecoderOutput m_out;
    private final T m_target;
    private final ProtocolEncoder m_encoder;
    private final IoSession m_session;
    private final ProtocolEncoderOutput m_in;

    public ActionReceiver(ProtocolDecoder decoder, ProtocolEncoder encoder, T target, IoSession session) {
        m_decoder = decoder;
        m_encoder = encoder;
        m_target = target;
        m_session = session;
        m_in = new ProtocolEncoderOutput() {
            public void write(ByteBuffer buf) {
                m_session.write(buf);
            }

            public void mergeAll() {}
            public WriteFuture flush() {
                return null;
            }
        };
        m_out = new ProtocolDecoderOutput() {
            public void write(Object message) {
                Object res = switchAction((ActionMessage)message, m_target);
                try {
                    m_encoder.encode(m_session, res, m_in);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public void flush() {}
        };
    }

    public void dataReceived(ByteBuffer data) throws Exception {
        m_decoder.decode(null, data, m_out);
    }

    protected abstract Object switchAction(ActionMessage action, T target);
}
