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

import ru.rd.courier.jdbc.csv.CsvStringResultSet;
import ru.rd.courier.jdbc.csv.ResultSetInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.LineReader;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;

/**
 * User: AStepochkin
 * Date: 10.08.2006
 * Time: 17:16:46
 */
public class CsvParser implements StreamParser {
    private final CourierLogger m_logger;
    private final String m_encoding;
    private final int m_skipFirstLines;
    private final HeaderReader m_headerParser;
    private final ResultSetInfo m_rsi;

    public interface HeaderReader {
        String[] readHeader(Reader r) throws IOException;
    }

    public CsvParser(
        CourierLogger logger,
        String encoding,
        int skipFirstLines, HeaderReader headerParser,
        ResultSetInfo rsi
    ) {
        m_logger = logger;
        m_encoding = encoding;
        m_skipFirstLines = skipFirstLines;
        m_headerParser = headerParser;
        m_rsi = rsi;
    }

    public void parseProperties(StringSimpleParser p) {}

    public ResultSet parse(InputStream is) {
        try {
            ResultSetInfo rsi = new ResultSetInfo(m_rsi);
            Reader r = new InputStreamReader(is, m_encoding);
            rsi.setReader(r);
            LineReader lr = new LineReader(r);
            if (m_skipFirstLines > 0) {
                for (int i = 0; i < m_skipFirstLines; i++) {
                    if (!lr.skipLine()) break;
                }
            }
            if (rsi.m_cols == null && m_headerParser != null) {
                rsi.m_cols = m_headerParser.readHeader(r);
            }
            return new CsvStringResultSet(m_logger, null, rsi);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel() {}
}
