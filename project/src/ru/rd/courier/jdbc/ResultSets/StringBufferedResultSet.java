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

import ru.rd.courier.jdbc.EmptyResultSetMetaData;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public abstract class StringBufferedResultSet extends IteratingResultSet {
    private IterColumnInfo[] m_infos;
    protected StringBuffer[] m_data;
    private boolean[] m_wasNulls;
    private int m_dynColCount;

    protected StringBufferedResultSet(Statement stmt) {
        super(stmt);
    }

    protected void init(IterColumnInfo[] infos) {
        m_infos = infos;

        m_data = new StringBuffer[m_infos.length];
        m_wasNulls = new boolean[m_data.length];
        for (int i = 0; i < m_infos.length; i++) {
            m_data[i] = new StringBuffer(m_infos[i].m_size);
        }
        m_dynColCount = infos.length;
    }

    protected void initWithConsts(IterColumnInfo[] infos, Map<String, String> data) {
        m_infos = new IterColumnInfo[infos.length + data.size()];
        m_data = new StringBuffer[m_infos.length];
        m_wasNulls = new boolean[m_data.length];
        int i = 0;
        for (; i < infos.length; i++) {
            m_infos[i] = infos[i];
            m_data[i] = new StringBuffer(m_infos[i].m_size);
        }
        for (Map.Entry<String, String> e: data.entrySet()) {
            m_infos[i] = new IterColumnInfo(e.getKey());
            m_data[i] = new StringBuffer(e.getValue());
            m_wasNulls[i] = (e.getValue() == null);
            i++;
        }
        m_dynColCount = infos.length;
    }

    protected int getDynColCount() {
        return m_dynColCount;
    }

    public StringBufferedResultSet(Statement stmt, IterColumnInfo[] infos) {
        this(stmt);
        if (infos == null) {
            throw new IllegalArgumentException("Infos cannot be null");
        }
        init(infos);
    }

    protected boolean needToClearNulls() {
        return true;
    }

    protected void beforeNextRecord() throws SQLException {
        super.beforeNextRecord();
        if (needToClearNulls()) {
            for (int i = 0; i < m_wasNulls.length; i++) {
                m_wasNulls[i] = true;
            }
        }
    }

    public void close() throws SQLException {
        m_data = null;
        m_infos = null;
    }

    private String innerGetString(int index) {
        if (m_wasNulls[index]) {
            m_wasNull = WAS_NULL;
            return null;
        }
        m_wasNull = NOT_WAS_NULL;
        return m_data[index].toString();
    }

    public String getString(int columnIndex) throws SQLException {
        if ((columnIndex < 1) || (columnIndex > m_data.length)) {
            throw new SQLException("Invalid column index '" + columnIndex + "'");
        }
        return innerGetString(columnIndex - 1);
    }

    public String getString(String columnName) throws SQLException {
        return innerGetString(innerFindColumn(columnName));
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        if ((columnIndex < 1) || (columnIndex > m_data.length)) {
            throw new SQLException("Invalid column index '" + columnIndex + "'");
        }
        m_wasNulls[columnIndex - 1] = (x == null);
        m_data[columnIndex - 1].setLength(0);
        if (x != null) m_data[columnIndex - 1].append(x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        updateString(findColumn(columnName), x);
    }

    private int innerFindColumn(String columnName) throws SQLException {
        for (int i = 0; i < m_infos.length; i++) {
            if (m_infos[i].m_name.equals(columnName)) return i;
        }
        throw new SQLException(
            "Result set does not contain column '" + columnName + "'");
    }

    public int findColumn(String columnName) throws SQLException {
        return innerFindColumn(columnName) + 1;
    }

    private class MetaData extends EmptyResultSetMetaData {
        public int getColumnCount() throws SQLException {
            return m_infos.length;
        }

        public String getColumnName(int column) throws SQLException {
            return m_infos[column - 1].m_name;
        }

        public int getPrecision(int column) throws SQLException {
            return m_infos[column - 1].m_size;
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

    public ResultSetMetaData getMetaData() throws SQLException {
        return new MetaData();
    }
}
