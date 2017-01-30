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
package ru.rd.courier.jdbc.csv;

import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.LineReader;
import ru.rd.courier.logging.CourierLoggerAdapter;

import java.util.Properties;
import java.util.logging.Logger;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 02.02.2006
 * Time: 17:24:56
 */
public class CsvConnection extends FileConnection {
    private final LineSplitter m_headSplitter;
    private final boolean m_numbericColumnTitles;
    private final String m_colTitlePrefix;

    private static final String c_csvSplitter = "csv";
    private static final String c_glTradeSplitter = "gl-trade";

    public CsvConnection(Properties props) throws SQLException {
        super(props);

        m_numbericColumnTitles = StringHelper.boolParam(props, c_numbericColumnTitles, false);
        m_colTitlePrefix = StringHelper.stringParam(props, c_colTitlePrefix, "");

        CsvLineSplitterInfo info = new CsvLineSplitterInfo(props);

        m_headSplitter = new CsvLineParser(
            info.getBracket(), info.getUseBracket(),
            info.getSeparator(), info.getNeedToTrim(), null
        );

        String splitterType =
            StringHelper.stringParam(props, c_lineSplitter, c_csvSplitter);

        if (splitterType.equals(c_csvSplitter)) {
            m_lineSplitter = new CsvLineParser(info);
        } else if (splitterType.equals(c_glTradeSplitter)) {
            m_lineSplitter = new GlTradeLineParser(info);
        } else {
            throw new SQLException("Unknown line splitter type '" + splitterType + "'");
        }
    }

    protected boolean initColumns(LineReader lr) throws IOException {
        StringBuffer sb = new StringBuffer();
        if (m_numbericColumnTitles) {
            if (lr.appendFullLines(sb, 1) < 1) {
                m_logger.severe(
                    "FileConnection.initFile: file does not have data rows"
                );
                return false;
            }
            StringHelper.trimLFCR(sb);
            String[] cols = m_lineSplitter.parse(sb);
            m_cols = new String[cols.length];
            for (int i = 0; i < cols.length; i++) {
                m_cols[i] = m_colTitlePrefix + Integer.toString(i);
            }
        } else {
            if (lr.appendFullLines(sb, 1) < 1) {
                m_logger.fine(
                    "FileConnection.initFile: file does not have full header"
                );
                return false;
            }
            StringHelper.trimLFCR(sb);
            m_cols = m_headSplitter.parse(sb);
            m_curPos = lr.getBytesRead();
        }
        return true;
    }

    protected ResultSet createResultSet(
        Logger logger, Statement stmt, ResultSetInfo rsInfo
    ) throws SQLException {
        return new CsvStringResultSet(new CourierLoggerAdapter(m_logger), stmt, rsInfo);
    }
}
