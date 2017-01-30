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

import java.util.List;
import java.util.LinkedList;

/**
 * User: STEPOCHKIN
 * Date: 19.10.2008
 * Time: 21:21:49
 */
public class CompositeSynchEncoder<Message> implements SynchEncoder<Message>{
    private final List<SynchEncoder<Object>> m_encoders = new LinkedList<SynchEncoder<Object>>();

    public CompositeSynchEncoder() {}

    public CompositeSynchEncoder(List<SynchEncoder<Object>> encoders) {
        this();
        registerEncoders(encoders);
    }

    public CompositeSynchEncoder(SynchEncoder<Object>[] encoders) {
        this();
        registerEncoders(encoders);
    }

    public void registerEncoder(SynchEncoder<Object> encoder) {
        m_encoders.add(encoder);
    }

    public void registerEncoders(List<SynchEncoder<Object>> encoders) {
        m_encoders.addAll(encoders);
    }

    public void registerEncoders(SynchEncoder<Object>[] encoders) {
        for (SynchEncoder<Object> encoder: encoders) {
            registerEncoder(encoder);
        }
    }

    public void encode(Message message, SynchEncoderOutput output) throws Exception {
        for (SynchEncoder<Object> encoder: m_encoders) {
            encoder.encode(message, output);
        }
    }
}
