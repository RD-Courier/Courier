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
package ru.rd.courier.jdbc.mock;

import ru.rd.courier.jdbc.EmptyResultSetMetaData;
import ru.rd.courier.utils.StringHelper;

import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class MockSimpleResultSet extends MockAbstractResultSet {
    private int m_rowCnt;
    private int m_colCnt;
    private String m_colNamePrefix;

    public MockSimpleResultSet(Statement stmt, int rowCnt, int colCnt, String colNamePrefix) {
        super(stmt);
        m_rowCnt = rowCnt;
        m_colCnt = colCnt;
        m_colNamePrefix = colNamePrefix;
    }

    public static MockSimpleResultSet createFromRequest(Statement stmt, Properties props) throws SQLException {
        if (!props.containsKey("RowCount")) {
            throw new SQLException("RowCount not specified");
        }
        int rowCnt = Integer.parseInt(props.getProperty("RowCount"));
        if (!props.containsKey("ColCount")) {
            throw new SQLException("ColCount not specified");
        }
        int colCnt = Integer.parseInt(props.getProperty("ColCount"));
        return new MockSimpleResultSet(
                stmt,
                rowCnt, colCnt,
                props.containsKey("ColPrefix") ? props.getProperty("ColPrefix") : ""
        );
        /*
        String[] args = request.split(" ");
        if (args.length < 2) throw new SQLException("Wrong sql syntax");
        int rowCnt = Integer.parseInt(args[0]);
        int colCnt = Integer.parseInt(args[1]);
        return new MockSimpleResultSet(
            stmt,
            rowCnt, colCnt,
            args.length > 2 ? args[2] : ""
        );
        */
    }

    protected int getRowCount() {
        return m_rowCnt;
    }

    protected int getColCount() {
        return m_colCnt;
    }

    public int findColumn(String columnName) throws SQLException {
        return Integer.parseInt(
            columnName.substring(m_colNamePrefix.length())
        );
    }

    public String getString(int columnIndex) throws SQLException {
        if (columnIndex > getColCount()) {
            throw new SQLException("Large column index (" + columnIndex + ")");
        }
        return columnIndex + "." + m_pos;
    }

    public Date getDate(int columnIndex) throws SQLException {
        return new Date(System.currentTimeMillis());
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new MetaData();
    }

    public void close() throws SQLException {
    }

    private class MetaData extends EmptyResultSetMetaData {
        public int getColumnCount() throws SQLException {
            return getColCount();
        }

        public String getColumnName(int column) throws SQLException {
            return (m_colNamePrefix + column);
        }

        public int getPrecision(int column) throws SQLException {
            return 128;
        }

        public int getScale(int column) throws SQLException {
            return 0;
        }

        public int getColumnType(int column) throws SQLException {
            return java.sql.Types.VARCHAR;
        }

        public String getColumnTypeName(int column) throws SQLException {
            return "VARCHAR";
        }
    }
}
