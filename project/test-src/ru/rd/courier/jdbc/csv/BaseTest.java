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

import ru.rd.courier.FileDataTestCase;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 20.07.2005
 * Time: 17:10:50
 */
public abstract class BaseTest extends FileDataTestCase {
    private String[][] m_data;
    protected int m_row, m_col, m_checkRow, m_checkCol;
    protected File m_curFile;
    protected ResultSet m_rs;
    private boolean m_appendLastNewLineChar;
    private MockLoggerHandler m_logHandler;
    private String[] m_expectedLogMessages;
    private CustomCheck m_customCheck;

    protected interface CustomCheck {
        void check() throws SQLException;
    }


    protected static final String c_filePrefixParam = "test-";
    protected static final String c_filePostfixParam = ".data";
    protected static final String c_dateFormatParam = "yyyyMMdd";
    protected static final String c_stringBracketParam = "'";
    protected static final String c_separatorParam = ",";
    public static final String c_needToTrimParam = "no";
    public static final String c_loggerName = "";

    protected void courierSetUp() throws Exception {
        m_logHandler = new MockLoggerHandler();
        Logger.getLogger(c_loggerName).addHandler(m_logHandler);
    }

    private static class MockLoggerHandler extends Handler {
        private List<LogRecord> m_records = new LinkedList<LogRecord>();

        public void publish(LogRecord record) {
            m_records.add(record);
            System.out.println(record.getMessage());
        }
        public void flush() {}
        public void close() throws SecurityException {}

        public List<LogRecord> getRecords() {
            return m_records;
        }
        public void clear() {
            m_records.clear();
        }
    }

    protected abstract void initConnection(int rowCount, int colCount, Properties extPars);
    protected abstract void closeConnection();
    protected abstract void customRequest();
    protected abstract boolean needFormHeader();

    protected void courierTearDown() throws Exception {
        if (m_rs != null) {
            m_rs.close();
            Statement stmt = m_rs.getStatement();
            if (stmt != null) stmt.close();
        }
        closeConnection();
    }

    private String[] m_cols;

    protected String getColName(int index) {
        return m_cols[index];
    }

    private String[][] formData(int rowCount, int colCount) {
        String[][] res = new String[rowCount + (needFormHeader() ? 1 : 0)][];
        int r = 0;
        if (needFormHeader()) {
            res[r] = new String[colCount];
            System.arraycopy(m_cols, 0, res[r], 0, colCount);
            r++;
        }
        for (; r < res.length; r++) {
            res[r] = formRow(r, colCount);
        }
        return res;
    }

    protected void insertRow(int index, String[] rowData) {
        String[][] res = new String[m_data.length + 1][];
        System.arraycopy(m_data, 0, res, 0, index + 1);
        res[index + 1] = rowData;
        System.arraycopy(m_data, index + 1, res, index + 2, m_data.length - index - 1);
        m_data = res;
    }

    protected abstract void formCellData(StringBuffer suggestedData, int col, int row);

    protected String[] formRow(int row, int colCount) {
        String[] res = new String[colCount];
        StringBuffer sb = new StringBuffer();
        for (int c = 0; c < colCount; c++) {
            sb.setLength(0);
            sb.append(row).append('.').append(c + 1).append('-');
            sb.append(Double.toString(Math.random()));
            formCellData(sb, c, row);
            res[c] = sb.toString();
        }
        return res;
    }

    protected String dataToString(String[] data) throws IOException {
        StringWriter out = new StringWriter();
        for (int i = 0; i < data.length; i++) {
            appendCellData(out, i, data[i]);
        }
        return out.toString();
    }

    protected abstract void appendCellData(Writer out, int col, String preparedData) throws IOException;

    private void appendDataToFile(
        int count, boolean appendLastNewLineChar
    ) throws IOException, SQLException {
        Writer out = new FileWriter(m_curFile, true);
        if (m_appendLastNewLineChar) {
            out.write('\n');
            m_appendLastNewLineChar = false;
        }
        for (int c = count; c > 0; c--) {
            if (m_row >= m_data.length) {
                fail("Too big count = " + count + " left " + c);
            }
            appendCellData(out, m_col, m_data[m_row][m_col]);
            m_col++;
            if (m_col >= m_data[m_row].length) {
                m_col = 0;
                m_row++;
                if (appendLastNewLineChar) out.write('\n');
                else m_appendLastNewLineChar = true;
            }
        }
        out.close();
        request();
        checkData();
    }

    protected void appendDataToFile(int count) throws IOException, SQLException {
        appendDataToFile(count, true);
    }

    private void checkData() throws SQLException {
        if (m_row == 0 && needFormHeader()) {
            assertFalse("Data retrieved while only header was sent", m_rs.next());
            return;
        }
        while (m_rs.next()) {
            if (m_checkRow >= m_row && m_checkCol >= m_col) break;

            while (true) {
                assertEquals(
                    "mismatch at row = " + m_checkRow + " col = " + m_checkCol,
                    m_data[m_checkRow][m_checkCol],
                    m_rs.getString(getColName(m_checkCol)));

                m_checkCol++;
                if (m_checkCol >= m_data[m_checkRow].length) {
                    m_checkCol = 0;
                    m_checkRow++;
                    break;
                }
            }
            if (m_customCheck != null) m_customCheck.check();
        }

        setCustomCheck(null);
    }

    protected void setCustomCheck(CustomCheck value) {
        m_customCheck = value;
    }

    /*
    private void skipData(int count) {
        for (int c = count; c > 0; c--) {
            if (m_row >= m_data.length) {
                fail("Too big count = " + count + " left " + c);
            }
            m_col++;
            if (m_col >= m_data[0].length) {
                m_col = 0;
                m_row++;
            }
        }
    }
    */

    protected void appendCustomData(String dataStr, String[][] data)
    throws SQLException, IOException {
        appendString(dataStr);
        request();
        checkCustomData(data);
        //skipData(data.length);
    }

    private void checkCustomData(String[][] data) throws SQLException {
        int count = 0;
        int col = 0;
        int row = 0;
        while (m_rs.next()) {
            while (true) {
                assertEquals(
                    "mismatch at custom row = " + row + " col = " + col,
                    data[row][col],
                    m_rs.getString(m_data[0][col])
                );

                count++;
                col++;
                boolean nextFetch = false;
                if (col >= data[row].length) {
                    col = 0;
                    row++;
                    nextFetch = true;
                }
                if (count >= data.length) return;
                if (nextFetch) break;
            }
        }
    }

    protected void appendToEndOfRow() throws IOException, SQLException {
        appendDataToFile(m_data[0].length - m_col);
    }

    protected void appendRows(int rowCount) throws IOException, SQLException {
        appendRows(rowCount, true);
    }

    protected void appendRows(int rowCount, boolean appendLastNewLineChar)
    throws IOException, SQLException {
        appendDataToFile(rowCount * m_data[0].length, appendLastNewLineChar);
    }

    protected void appendToEnd() throws IOException, SQLException {
        int count = 0;
        int r = m_row;
        if (m_col > 0) {
            count += m_data[r].length - m_col;
            r++;
        }
        for (; r < m_data.length; r++) {
            count += m_data[r].length ;
        }
        appendDataToFile(count);

        Logger.getLogger(c_loggerName).removeHandler(m_logHandler);
        if (m_expectedLogMessages != null) {
            assertEquals(
                m_expectedLogMessages.length,
                m_logHandler.getRecords().size()
            );
            for (int i = 0; i < m_expectedLogMessages.length; i++) {
                if (m_expectedLogMessages[i] != null) {
                    boolean b = m_logHandler.getRecords().get(i).getMessage().startsWith(
                        m_expectedLogMessages[i]
                    );
                    if (!b) throw new RuntimeException(
                        "Log message not found: " + m_expectedLogMessages[i]
                    );
                }
            }
        }
        m_logHandler.close();
    }

    private void appendString(String str) throws IOException {
        Writer out = new FileWriter(m_curFile, true);
        out.write(str);
        out.close();
    }

    protected void beginTest(
        int rowCount, int colCount, String[] extPars, String[] expectedLogMessages
    ) {
        m_expectedLogMessages = expectedLogMessages;
        m_row = 0;
        m_col = 0;
        clearCheckPositions();
        m_appendLastNewLineChar = false;

        final String colPrefix = "col";
        m_cols = new String[colCount];
        for (int c = 0; c < colCount; c++) {
            m_cols[c] = colPrefix + c;
        }

        Properties props = new Properties();
        if (extPars != null) {
            for (int i = 0; i < extPars.length; i += 2) {
                props.setProperty(extPars[i], extPars[i + 1]);
            }
        }

        initConnection(rowCount, colCount, props);
        m_data = formData(rowCount, colCount);
        if (m_curFile.exists()) {
            if(!m_curFile.delete()) fail(
                "Failed to delete file: " + m_curFile.getAbsolutePath());
        }
    }

    protected void beginTest(
        int rowCount, int colCount, String[] extPars
    ) {
        beginTest(rowCount, colCount, extPars, new String[] {});
    }

    private void clearCheckPositions() {
        m_checkRow = needFormHeader() ? 1 : 0;
        m_checkCol = 0;
    }

    private void request() throws SQLException {
        if (m_rs != null) {
            Statement stmt = m_rs.getStatement();
            if (stmt != null) stmt.close();
            else m_rs.close();
        }
        customRequest();
    }

    public void testIntervalFullRead()
    throws SQLException, IOException, InterruptedException {
        final long fullReadInterval = 200;
        beginTest(4, 3, new String[] {
            FileConnection.c_fullReadInterval, Long.toString(fullReadInterval)
        });
        appendRows(2);
        appendRows(1);
        Thread.sleep(fullReadInterval + 1);
        clearCheckPositions();
        appendToEnd();
    }

    public void testFullReadEveryNRequests()
    throws SQLException, IOException, InterruptedException {
        final int fullReadCount = 10;
        final int checkAmount = 3;
        beginTest(
            (checkAmount + 1) * fullReadCount + 1, 3,
            new String[] {
                FileConnection.c_fullReadEveryParam,
                Long.toString(fullReadCount)
            }
        );
        for (int c = 0; c < checkAmount; c++) {
            for (int i = 0; i < fullReadCount; i++) {
                appendRows(1);
            }
            clearCheckPositions();
        }
        appendToEnd();
    }
}
