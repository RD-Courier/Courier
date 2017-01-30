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

import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;
import ru.rd.utils.ObjectFactory;

/**
 * User: AStepochkin
 * Date: 24.04.2009
 * Time: 15:09:23
 */
public abstract class ObjectDecodeStep<MessageType> implements DecodeStep {

    private final DecodeStep[] m_steps;
    private final ObjectFactory<MessageType> m_factory;

    public ObjectDecodeStep(DecodeStep[] steps, ObjectFactory<MessageType> factory) {
        m_steps = steps;
        m_factory = factory;
    }

    private final StatedDecoderState<MessageType> m_state = new StatedDecoderState<MessageType>();

    public boolean decode(IoSession session, ByteBuffer in, StatedDecoderState state) throws Exception {
        if (m_state.getMessage() == null) {
            m_state.setStep(0);
            m_state.setMessage(m_factory.create());
        }
        for (; m_state.getStep() < m_steps.length; ) {
            if (!m_steps[m_state.getStep()].decode(session, in, m_state)) {
                m_state.incCallCount();
                break;
            }
            m_state.incStep();
        }
        boolean result;
        if (m_state.getStep() >= m_steps.length) {
            MessageType value = m_state.getMessage();
            m_state.setMessage(null);
            setDecodedProperty(state.getMessage(), value);
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    protected abstract void setDecodedProperty(Object message, Object value);
}
