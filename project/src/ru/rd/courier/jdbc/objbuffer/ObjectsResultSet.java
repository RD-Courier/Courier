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
package ru.rd.courier.jdbc.objbuffer;

import ru.rd.courier.jdbc.EmptyResultSetMetaData;
import ru.rd.courier.jdbc.ResultSets.IteratingResultSet;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;

/**
 * User: AStepochkin
 * Date: 22.07.2008
 * Time: 18:14:10
 */
public abstract class ObjectsResultSet extends IteratingResultSet {
    private final ColumnInfo[] m_columns;
    private Object[] m_data = null;
    private boolean m_wasNull;

    public ObjectsResultSet(Statement stmt, ColumnInfo[] columns) {
        super(stmt);
        m_wasNull = false;
        m_columns = columns;
    }

    public int innerFindColumn(String columnName) throws SQLException {
        for (int i = 0; i < m_columns.length; i++) {
            if (m_columns[i].getName().equals(columnName)) return i;
        }
        throw new SQLException("Result set does not contain column '" + columnName + "'");
    }

    public void setData(Object[] data) {
        if (data != null) {
            if (m_columns.length != data.length) {
                throw new RuntimeException(
                    "Invalid data length " + data.length + " (should be " + m_columns.length + ")"
                );
            }
        }
        m_data = data;
    }

    public boolean wasNull() throws SQLException {
        return m_wasNull;
    }

    public ColumnInfo getColumn(int index) throws SQLException {
        checkColumnIndex(index);
        return m_columns[index - 1];
    }

    public int getColumnCount() {
        return m_columns.length;
    }

    private void checkColumnIndex(int index) throws SQLException {
        if ((index < 1) || (index > m_columns.length)) {
            throw new SQLException("Invalid column index '" + index + "'");
        }
    }

    public void updateObject(int columnIndex, Object obj) throws SQLException {
        checkColumnIndex(columnIndex);
        m_data[columnIndex - 1] = obj;
    }

    public void updateObject(String columnName, Object obj) throws SQLException {
        updateObject(findColumn(columnName), obj);
    }

    public Object getObject(int columnIndex) {
        Object obj = m_data[columnIndex - 1];
        if (obj == null) {
            m_wasNull = true;
            return null;
        }
        return obj;
    }

    public void updateNull(String columnName) throws SQLException {
        updateObject(columnName, null);
    }

    public void updateNull(int columnIndex) throws SQLException {
        updateObject(columnIndex, null);
    }

    public String getString(int columnIndex) throws SQLException {
        return getColumn(columnIndex).getString(getObject(columnIndex));
    }

    public String getString(String columnName) throws SQLException {
        return getString(findColumn(columnName));
    }

    public int getInt(int columnIndex) throws SQLException {
        return getColumn(columnIndex).getInt(getObject(columnIndex));
    }

    public int getInt(String columnName) throws SQLException {
        return getInt(findColumn(columnName));
    }

    public long getLong(int columnIndex) throws SQLException {
        return getColumn(columnIndex).getLong(getObject(columnIndex));
    }

    public long getLong(String columnName) throws SQLException {
        return getLong(findColumn(columnName));
    }

    public double getDouble(int columnIndex) throws SQLException {
        return getColumn(columnIndex).getDouble(getObject(columnIndex));
    }

    public double getDouble(String columnName) throws SQLException {
        return getDouble(findColumn(columnName));
    }

    public float getFloat(int columnIndex) throws SQLException {
        return getColumn(columnIndex).getFloat(getObject(columnIndex));
    }

    public float getFloat(String columnName) throws SQLException {
        return getFloat(findColumn(columnName));
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
        return getColumn(columnIndex).getBoolean(getObject(columnIndex));
    }

    public boolean getBoolean(String columnName) throws SQLException {
        return getBoolean(findColumn(columnName));
    }

    public Date getDate(int columnIndex) throws SQLException {
        return getColumn(columnIndex).getDate(getObject(columnIndex));
    }

    public Date getDate(String columnName) throws SQLException {
        return getDate(findColumn(columnName));
    }

    public int findColumn(String columnName) throws SQLException {
        return innerFindColumn(columnName) + 1;
    }

    public void close() throws SQLException {
        super.close();
        setData(null);
    }

    private class MetaData extends EmptyResultSetMetaData {
        private ColumnInfo getCol(int index) {
            return m_columns[index - 1];
        }

        public int getColumnCount() throws SQLException {
            return m_columns.length;
        }

        public String getColumnName(int column) throws SQLException {
            return getCol(column).getName();
        }

        public int getPrecision(int column) throws SQLException {
            return getCol(column).getPrecision();
        }

        public int getScale(int column) throws SQLException {
            return getCol(column).getScale();
        }

        public int getColumnType(int column) throws SQLException {
            return getCol(column).getType();
        }

        public String getColumnTypeName(int column) throws SQLException {
            return getCol(column).getTypeName();
        }
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new MetaData();
    }
}
