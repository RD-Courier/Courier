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

/**
 * User: STEPOCHKIN
 * Date: 30.07.2008
 * Time: 12:52:36
 */
public class StatedDecoderState<Message> {
    private int m_step;
    private int m_callCount;
    private Message m_target = null;
    private int m_processed;
    public int auxInt;

    public int getStep() {
        return m_step;
    }

    public void incStep() {
        m_step++;
        setCallCount(0);
    }

    public void setStep(int step) {
        m_step = step;
    }

    public int getCallCount() {
        return m_callCount;
    }

    public void setCallCount(int callCount) {
        m_callCount = callCount;
    }

    public void incCallCount() {
        m_callCount++;
    }

    public Message getMessage() {
        return m_target;
    }

    public void setMessage(Message target) {
        m_target = target;
        if (target != null) {
            m_step = 0;
            m_callCount = 0;
            m_processed = 0;
        }
    }

    public int getProcessed() {
        return m_processed;
    }

    public void setProcessed(int processed) {
        m_processed = processed;
    }
}
