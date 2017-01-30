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

import ru.rd.courier.jdbc.ResultSets.StringBufferedResultSet;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.logging.CourierLogger;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

public class CsvStringResultSet extends StringBufferedResultSet {
    private final CourierLogger m_logger;
    private final Reader m_reader;
    private final StringBuffer m_lineBuffer = new StringBuffer();
    private final LineSplitter m_lineParser;
    private int m_lineNum;
    private int m_linesToRead;
    private final boolean m_addAbsentAsNull;
    private final LineFilter m_filter;
    private final String m_lineNumberColName;
    private final Properties m_constFields;

    private static final int c_colSize = 256;
    private static final int c_PseudoFieldsCount = 1;

    public CsvStringResultSet(
        CourierLogger logger, Statement stmt,
        Reader reader, String lineNumTitle, String[] colTitles, LineSplitter lineParser,
        int firstLineNum, int linesToRead,
        boolean addAbsentAsNull, LineFilter filter, Properties constFields
    ) throws SQLException {
        super(stmt);
        m_logger = logger;
        m_reader = reader;
        m_lineNumberColName = lineNumTitle;
        m_lineParser = lineParser;
        m_lineNum = firstLineNum;
        m_linesToRead = linesToRead;
        m_addAbsentAsNull = addAbsentAsNull;
        m_filter = filter;
        m_constFields = constFields;
        IterColumnInfo[] colInfo = getColInfos(colTitles, constFields);
        init(colInfo);
        for(Map.Entry e: constFields.entrySet()) {
            updateString((String)e.getKey(), (String)e.getValue());
        }
    }

    private IterColumnInfo[] getColInfos(String[] cols, Properties constFields) {
        IterColumnInfo[] colInfos = new IterColumnInfo[
            cols.length + c_PseudoFieldsCount + constFields.size()
        ];

        for (int i = 0; i < cols.length; i++) {
            colInfos[i] = new IterColumnInfo(cols[i], c_colSize);
        }

        colInfos[cols.length] = new IterColumnInfo(m_lineNumberColName, 32);

        int i = c_PseudoFieldsCount + cols.length;
        for (Map.Entry entry: constFields.entrySet()) {
            colInfos[i] = new IterColumnInfo(
                (String)entry.getKey(), ((String)entry.getValue()).length()
            );
            i++;
        }

        return colInfos;
    }

    public CsvStringResultSet(
        CourierLogger logger, Statement stmt,
        ResultSetInfo rsInfo
    ) throws SQLException {
        this(
            logger, stmt, rsInfo.m_reader,
            rsInfo.m_lineNumTitle,
            rsInfo.m_cols, rsInfo.m_parser,
            rsInfo.m_firstLineNum, rsInfo.m_linesToRead,
            rsInfo.m_addAbsentAsNull, rsInfo.m_filter,
            rsInfo.m_constFields
        );
    }

    protected boolean getRecord() throws SQLException {
        if (m_linesToRead == 0) return false;
        boolean res;
        try {
            while (res = getLine()) {
                m_linesToRead--;
                m_lineNum++;
                if (!m_filter.skip(m_lineBuffer.toString())) break;
                if (m_linesToRead == 0) return false;
            }
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
        String[] fields = null;
        if (res) {
            try {
                fields = m_lineParser.parse(m_lineBuffer);
            } catch (Exception e) {
                m_logger.error("Parse error", e);
                res = false;
            }
        }
        if (res) {
            int realColsCount = m_data.length - c_PseudoFieldsCount + m_constFields.size();
            if (!m_addAbsentAsNull) {
                if (fields.length != realColsCount) {
                    m_logger.error(
                        "Line " + m_lineNum
                        + ": Wrong fields number (expected " + realColsCount + " actually " + fields.length + ") in " +
                        "'" + m_lineBuffer + "'"
                    );
                }
            }
            int i = 0;
            for (;i < realColsCount; i++) {
                updateString(i + 1, i < fields.length ? fields[i] : null);
            }
            updateString(i + 1, Integer.toString(m_lineNum - 1));
        }
        return res;
    }

    protected int skipRecords(int count) throws SQLException {
        int i;
        for (i = 0; (m_linesToRead != 0) && (i < count);) {
            try {
                if (!getLine()) break;
            } catch (IOException e) {
                throw new SQLException(e.getMessage());
            }
            if (!m_filter.skip(m_lineBuffer.toString())) i++;
            if (m_linesToRead > 0) m_linesToRead--;
            m_lineNum++;
        }
        return i;
    }

    private boolean getLine() throws IOException {
        return StringHelper.readLine(m_reader, m_lineBuffer);
    }

    public void close() throws SQLException {
        try {
            m_reader.close();
        } catch (IOException e) {
            throw new SQLException("Failed to close reader: " + e.getMessage());
        }
        super.close();
    }

}
