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
package ru.rd.courier.jdbc.databuffer;

import ru.rd.courier.jdbc.EmptyResultSetMetaData;
import ru.rd.courier.jdbc.GenericSqlException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.Time;

/**
 * User: AStepochkin
 * Date: 14.12.2006
 * Time: 12:55:36
 */
public class RecordBuffer {
    protected List<ColumnInfo> m_columns = new ArrayList<ColumnInfo>();
    protected int m_recordSize;
    private byte[] m_curBuffer = null;
    private boolean m_wasNull;

    public int getRecordSize() {
        return m_recordSize;
    }

    private void adjustDataPositions(int index) {
        int colPos = 0;
        ColumnInfo ci;
        if (index > 0) {
            ci = m_columns.get(index - 1);
            colPos = ci.getDataPosition() + ci.getByteSize();
        }

        for (Iterator it = m_columns.listIterator(index) ; it.hasNext(); ) {
            ci = (ColumnInfo)it.next();
            ci.setDataPosition(colPos);
            colPos += ci.getByteSize();
        }
        m_recordSize = colPos;
    }

    public void addColumn(ColumnInfo ci, int index) throws SQLException {
        if (index > m_columns.size()) {
            throw new RuntimeException("Invalid column position");
        }
        m_columns.add(index, ci);
        adjustDataPositions(index);
    }

    public void addColumn(ColumnInfo ci) throws SQLException {
        addColumn(ci, m_columns.size());
    }

    public int innerFindColumn(String columnName) throws SQLException {
        int i = 0;
        for (ColumnInfo ci : m_columns) {
            if (ci.m_name.equals(columnName)) return i;
            i++;
        }
        throw new SQLException("Result set does not contain column '" + columnName + "'");
    }

    public byte[] createBuffer() {
        return new byte[m_recordSize];
    }

    public void init() throws SQLException {
        for (ColumnInfo ci : m_columns) {
            ci.initCustomValue(m_curBuffer);
        }
    }

    public void setCurBuffer(byte[] buffer) {
        m_curBuffer = buffer;
    }

    public byte[] getCurBuffer() {
        return m_curBuffer;
    }

    public void initMetaInfo() {
        m_columns.clear();
        m_recordSize = 0;
    }

    public void importMetaInfo(RecordBuffer r) {
        initMetaInfo();
        for (ColumnInfo ci : r.m_columns) {
            m_columns.add((ColumnInfo) ci.clone());
        }
        m_recordSize = r.m_recordSize;
    }

    public Object clone() throws CloneNotSupportedException {
        super.clone();
        RecordBuffer ret = new RecordBuffer();
        ret.importMetaInfo(this);
        ret.m_curBuffer = m_curBuffer;
        ret.m_wasNull = m_wasNull;
        return ret;
    }

    public void importRecord(DataBuffer buf) throws SQLException {
        for (ColumnInfo ci : m_columns) {
            ci.setString(m_curBuffer, buf.getString(ci.getName()));
        }
    }

    public boolean wasNull() throws SQLException {
        return m_wasNull;
    }

    public ColumnInfo getColumn(int index) {
        return m_columns.get(index - 1);
    }

    public int getColumnCount() {
        return m_columns.size();
    }

    private void checkColumnIndex(int index) throws SQLException {
        if ((index < 1) || (index > m_columns.size())) {
            throw new SQLException("Invalid column index '" + index + "'");
        }
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        checkColumnIndex(columnIndex);
        ColumnInfo ci = getColumn(columnIndex);
        ci.setString(m_curBuffer, x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        updateString(innerFindColumn(columnName) + 1, x);
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        checkColumnIndex(columnIndex);
        ColumnInfo ci = getColumn(columnIndex);
        ci.setInteger(m_curBuffer, x);
    }

    public void updateInt(String columnName, int x) throws SQLException {
        updateInt(innerFindColumn(columnName) + 1, x);
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        checkColumnIndex(columnIndex);
        ColumnInfo ci = getColumn(columnIndex);
        ci.setLong(m_curBuffer, x);
    }

    public void updateLong(String columnName, long x) throws SQLException {
        updateLong(innerFindColumn(columnName) + 1, x);
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        checkColumnIndex(columnIndex);
        ColumnInfo ci = getColumn(columnIndex);
        ci.setDate(m_curBuffer, x);
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        updateDate(innerFindColumn(columnName) + 1, x);
    }

    private void setNull(int columnIndex) throws GenericSqlException {
        ColumnInfo ci = getColumn(columnIndex);
        ci.setNull(m_curBuffer);
    }

    public void updateNull(int columnIndex) throws SQLException {
        setNull(columnIndex);
    }

    public void updateNull(String columnName) throws SQLException {
        setNull(innerFindColumn(columnName) + 1);
    }

    public java.sql.Date getDate(int columnIndex) throws SQLException {
        ColumnInfo ci = getColumn(columnIndex);
        Date ret = ci.getDate(m_curBuffer);
        m_wasNull = ci.wasNull();
        return ret;
    }

    public Date getDate(String columnName) throws SQLException {
        return getDate(innerFindColumn(columnName) + 1);
    }

    public float getFloat(int columnIndex) throws SQLException {
        ColumnInfo ci = getColumn(columnIndex);
        float ret = ci.getFloat(m_curBuffer);
        m_wasNull = ci.wasNull();
        return ret;
    }

    public float getFloat(String columnName) throws SQLException {
        return getFloat(innerFindColumn(columnName) + 1);
    }

    public int getInt(int columnIndex) throws SQLException {
        ColumnInfo ci = getColumn(columnIndex);
        int ret = ci.getInteger(m_curBuffer);
        m_wasNull = ci.wasNull();
        return ret;
    }

    public int getInt(String columnName) throws SQLException {
        return getInt(innerFindColumn(columnName));
    }

    public long getLong(int columnIndex) throws SQLException {
        ColumnInfo ci = getColumn(columnIndex);
        long ret = ci.getLong(m_curBuffer);
        m_wasNull = ci.wasNull();
        return ret;
    }

    public long getLong(String columnName) throws SQLException {
        return getLong(innerFindColumn(columnName) + 1);
    }

    public String getString(int columnIndex) throws SQLException {
        ColumnInfo ci = getColumn(columnIndex);
        String ret = ci.getString(m_curBuffer);
        m_wasNull = ci.wasNull();
        return ret;
    }

    public String getString(String columnName) throws SQLException {
        return getString(innerFindColumn(columnName) + 1);
    }

    public Time getTime(int columnIndex) throws SQLException {
        m_wasNull = true;
        return null;
    }

    public int findColumn(String columnName) throws SQLException {
        return (innerFindColumn(columnName) + 1);
    }

    private class MetaData extends EmptyResultSetMetaData {
        public int getColumnCount() throws SQLException {
            return m_columns.size();
        }

        public String getColumnName(int column) throws SQLException {
            ColumnInfo ci = m_columns.get(column - 1);
            return ci.m_name;
        }

        public int getPrecision(int column) throws SQLException {
            ColumnInfo ci = m_columns.get(column - 1);
            return ci.m_precision;
        }

        public int getScale(int column) throws SQLException {
            ColumnInfo ci = m_columns.get(column - 1);
            return ci.m_scale;
        }

        public int getColumnType(int column) throws SQLException {
            ColumnInfo ci = m_columns.get(column - 1);
            return ci.getType();
        }

        public String getColumnTypeName(int column) throws SQLException {
            ColumnInfo ci = m_columns.get(column - 1);
            return ci.getTypeName();
        }
    }

    public ResultSetMetaData createMetaData() {
        return new MetaData();
    }
}
