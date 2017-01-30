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

import ru.rd.courier.jdbc.ResultSets.StringBufferListResultSet;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.utils.StreamHelper;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;

/**
 * User: AStepochkin
 * Date: 14.08.2006
 * Time: 11:12:40
 */
public class NullParser implements StreamParser {
    String m_encoding;
    private final IterColumnInfo[] m_cols;

    public NullParser(
        String encoding, IterColumnInfo[] cols
    ) {
        m_encoding = encoding;
        m_cols = cols;
    }

    public void parseProperties(StringSimpleParser p) {}

    public ResultSet parse(InputStream is) throws IOException {
        String data = StreamHelper.streamToString(is, m_encoding, true);
        return new StringBufferListResultSet(
            null, m_cols,
            new StringBuffer[] {new StringBuffer(data)}
        );
    }

    public void cancel() {}
}
