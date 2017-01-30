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
package ru.rd.courier.scripting.dataaccess.streamed;

import ru.rd.courier.scripting.dataaccess.streamed.StreamConnector;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.StringReader;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;

/**
 * User: AStepochkin
 * Date: 14.08.2006
 * Time: 11:27:42
 */
public class ConstStreamConnector implements StreamConnector {
    private byte[] m_data = null;

    public ConstStreamConnector(String data) {
        setData(data);
    }

    private void setData(String data) {
        if (data == null) {
            m_data = null;
            return;
        }
        try {
            m_data = data.getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void parseProperties(StringSimpleParser p) {
        if (m_data == null) {
            setData(p.endSubstr());
        }
    }

    public InputStream createStream() {
        return new ByteArrayInputStream(m_data);
    }

    public void cancel() {}
}
