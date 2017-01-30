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

import ru.rd.courier.jdbc.AbstractConnectionSkeleton;

import java.sql.SQLException;
import java.util.Properties;
import java.io.Writer;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 03.02.2006
 * Time: 17:23:18
 */
public class CsvTest extends BaseTest {
    private FileConnection m_con;
    private final String m_separator = ",";
    private final String m_stringBracket = "'";

    protected void initConnection(int rowCount, int colCount, Properties extPars) {
        Properties props = new Properties();
        props.setProperty(
            FileConnection.c_dirParam, getTempDir("").getAbsolutePath());
        props.setProperty(FileConnection.c_filePrefixParam, c_filePrefixParam);
        props.setProperty(FileConnection.c_filePostfixParam, c_filePostfixParam);
        props.setProperty(FileConnection.c_dateFormatParam, c_dateFormatParam);
        props.setProperty(FileConnection.c_stringBracketParam, c_stringBracketParam);
        props.setProperty(FileConnection.c_separatorParam, c_separatorParam);
        props.setProperty(FileConnection.c_needToTrimParam, c_needToTrimParam);
        props.setProperty(AbstractConnectionSkeleton.c_loggerName, c_loggerName);
        if (extPars != null) props.putAll(extPars);

        try {
            m_con = new CsvConnection(props);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        m_curFile = getTempFile(m_con.getFileName());
    }

    protected void closeConnection() {
        if (m_con.getActive()) {
            try {
                m_con.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        m_con = null;
    }

    protected void customRequest() {
        try {
            m_rs = m_con.createStatement().executeQuery("");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean needFormHeader() {
        return true;
    }

    protected void formCellData(StringBuffer suggestedData, int col, int row) {
    }

    protected void appendCellData(Writer out, int col, String preparedData) throws IOException {
        if (col > 0) out.write(m_separator);
        out.write(m_stringBracket);
        out.write(preparedData);
        out.write(m_stringBracket);
    }

    public void test() throws SQLException, IOException {
        beginTest(10, 3, null);
        appendDataToFile(1);
        appendToEndOfRow();
        appendRows(1);
        appendRows(2);
        appendRows(1, false);
        appendRows(3);
        appendToEnd();
    }

    public void testFieldsNumber() throws SQLException, IOException {
        beginTest(
            3, 3, new String[] {
                FileConnection.c_addAbsentAsNull, "no"
            },
            new String[] {
                "Line 3: Wrong fields number (expected 3)"
            }
        );
        insertRow(1, formRow(m_row, 2));
        appendToEnd();
    }

    public void testParseError() throws SQLException, IOException {
        beginTest(
            3, 3, null,
            new String[] {"Parse error"}
        );
        appendRows(2);
        String[] data = formRow(m_row, 2);
        appendCustomData(dataToString(data) + "'\n", new String[][] {data});
        appendToEnd();
    }

    public void testLineNumber() throws SQLException, IOException {
        beginTest(3, 3, null);
        setCustomCheck(new CustomCheck() {
            public void check() throws SQLException {
                assertEquals(
                    Integer.toString(m_checkRow - 1),
                    m_rs.getString(FileConnection.c_lineNumberTitle)
                );
            }
        });
        appendToEnd();
    }
}
