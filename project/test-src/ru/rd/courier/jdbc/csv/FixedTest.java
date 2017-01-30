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

import ru.rd.courier.CourierException;
import ru.rd.courier.datalinks.FixedColumnsFileSourceFactory;
import ru.rd.courier.logging.LoggerAdapter;
import ru.rd.courier.scripting.DataSource;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 03.02.2006
 * Time: 15:31:33
 */
public class FixedTest extends BaseTest {
    private DataSource m_con;
    private List<FixedColumnsFileSource.Field> m_fields = new LinkedList<FixedColumnsFileSource.Field>();

    protected void initConnection(int rowCount, int colCount, Properties extPars) {
        final String cFileName = "test-file";
        m_fields.clear();
        int colPos = 0;
        int colWidth = 16;
        for (int c = 0; c < colCount; c++) {
            m_fields.add(new FixedColumnsFileSource.Field(getColName(c), colPos, colPos + colWidth));
            colPos += colWidth;
            colWidth += 5;
        }
        long fullReadInterval = 0;
        if (extPars.containsKey(FileConnection.c_fullReadInterval)) {
            fullReadInterval = Long.valueOf(extPars.getProperty(FileConnection.c_fullReadInterval));
        }
        int fullReadEvery = 0;
        if (extPars.containsKey(FileConnection.c_fullReadEveryParam)) {
            fullReadEvery = Integer.valueOf(extPars.getProperty(FileConnection.c_fullReadEveryParam));
        }
        FixedColumnsFileSourceFactory f = new FixedColumnsFileSourceFactory(
            new LoggerAdapter(null, c_loggerName, true),
            getTempDir("").getAbsolutePath(),
            new FileSource.ConstFileSelector(cFileName),
            true, fullReadEvery, fullReadInterval, 0, false,
            new FakeFilter(), "title", null,
            m_fields
        );
        m_con = (DataSource)f.getObject(null);
        m_curFile = getTempFile(cFileName);
    }

    protected void closeConnection() {
        try {
            m_con.close();
        } catch (CourierException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    protected void customRequest() {
        try {
            m_rs = m_con.request("");
        } catch (CourierException e) {
            throw new RuntimeException(e);
        }

    }

    protected boolean needFormHeader() {
        return false;
    }

    protected void formCellData(StringBuffer suggestedData, int col, int row) {
        FixedColumnsFileSource.Field f = m_fields.get(col);
        if (suggestedData.length() >= f.getWidth()) {
            suggestedData.setLength(f.getWidth());
        } else {
            suggestedData.append(m_repChar, 0, f.getWidth() - suggestedData.length());
        }
    }

    private static final char[] m_repChar = new char[128];
    static {
        for (int i = 0; i < m_repChar.length; i++) {
            m_repChar[i] = '#';
        }
    }

    protected void appendCellData(Writer out, int col, String preparedData) throws IOException {
        out.write(preparedData);
    }

    public void test() throws SQLException, IOException {
        beginTest(10, 3, null, null);
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
                "Line 4: Wrong fields number (expected 3)"
            }
        );
        insertRow(1, formRow(m_row, 2));
        appendToEnd();
    }
}
