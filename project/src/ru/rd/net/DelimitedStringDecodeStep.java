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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * User: STEPOCHKIN
 * Date: 30.07.2008
 * Time: 14:50:34
 */
public abstract class DelimitedStringDecodeStep extends DelimitedBytesDecodeStep {
    private CharsetDecoder m_charset;

    private static final byte[] s_delimiter = new byte[] {0};

    public DelimitedStringDecodeStep(byte[] delimiter, Charset charset) {
        super(delimiter);
        m_charset = charset.newDecoder();
    }

    public DelimitedStringDecodeStep(Charset charset) {
        this(s_delimiter, charset);
    }

    protected void setDecodedProperty(Object message, ByteBuffer in) {
        try {
            setDecodedString(message, m_charset.decode(in.buf()).toString());
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void setDecodedString(Object message, String value);
}
