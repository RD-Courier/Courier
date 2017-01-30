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
package ru.rd.courier.jdbc.ResultSets;

import junit.framework.TestCase;
import ru.rd.courier.jdbc.databuffer.IntegerColumnInfo;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;

import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 14.12.2006
 * Time: 15:58:33
 */
public class TypedIteratingResultSetTest extends TestCase {
    private static class TestResultSet extends TypedIteratingResultSet {
        private String[][] m_data;
        private int m_pos;

        public TestResultSet(String[][] data) {
            super(null);
            m_data = data;
            m_pos = 0;
        }

        protected boolean getRecord() throws SQLException {
            if (m_pos >= m_data.length) return false;
            String[] data = m_data[m_pos];
            for (int i = 1; i <= data.length; i++) {
                updateString(i, data[i-1]);
            }
            m_pos++;
            return true;
        }

        protected int skipRecords(int count) throws SQLException {
            int oldPos = m_pos;
            m_pos = Math.min(m_pos + count, m_data.length);
            return (m_pos - oldPos);
        }
    }

    public void test() throws SQLException {
        String[][] data = new String[][] {
              new String[] {"a1.1", "a1.2", "346"}
            , new String[] {"a1.1", "a1.2", "567"}
        };
        TypedIteratingResultSet rs = new TestResultSet(data);
        rs.addColumn(new StringColumnInfo("StrCol1", 16));
        rs.addColumn(new StringColumnInfo("StrCol2", 16));
        rs.addColumn(new IntegerColumnInfo("IntCol3", false));
        rs.initialized();

        int i = 0;
        while (i < data.length) {
            assertTrue(rs.next());
            for (int c = 0; c < data[0].length; c++) {
                String value = rs.getString(c + 1);
                String exp = data[i][c];
                assertEquals(exp == null, rs.wasNull());
                assertEquals(exp, value);
            }
            i++;
        }
    }
}
