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

import ru.rd.courier.jdbc.databuffer.RecordBuffer;
import ru.rd.courier.jdbc.databuffer.ColumnInfo;

import java.sql.*;

/**
 * User: AStepochkin
 * Date: 14.12.2006
 * Time: 15:32:43
 */
public abstract class TypedIteratingResultSet extends IteratingResultSet {
    private RecordBuffer m_record;

    public TypedIteratingResultSet(Statement stmt) {
        super(stmt);
        m_record = new RecordBuffer();
    }

    public void addColumn(ColumnInfo ci, int index) throws SQLException {
        m_record.addColumn(ci, index);
    }

    public void addColumn(ColumnInfo ci) throws SQLException {
        m_record.addColumn(ci);
    }
    
    public void initialized() {
        m_record.setCurBuffer(m_record.createBuffer());
    }

    protected void beforeNextRecord() throws SQLException {
        super.beforeNextRecord();
        if (needToInitRecord()) m_record.init();
    }

    protected boolean needToInitRecord() {
        return true;
    }

    public int findColumn(String columnName) throws SQLException {
        return m_record.findColumn(columnName);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return m_record.createMetaData();
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        m_record.updateString(columnIndex, x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        m_record.updateString(columnName, x);
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        m_record.updateInt(columnIndex, x);
    }

    public void updateInt(String columnName, int x) throws SQLException {
        m_record.updateInt(columnName, x);
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        m_record.updateLong(columnIndex, x);
    }

    public void updateLong(String columnName, long x) throws SQLException {
        m_record.updateLong(columnName, x);
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        m_record.updateDate(columnIndex, x);
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        m_record.updateDate(columnName, x);
    }

    public void updateNull(int columnIndex) throws SQLException {
        m_record.updateNull(columnIndex);
    }

    public void updateNull(String columnName) throws SQLException {
        m_record.updateNull(columnName);
    }

    public boolean wasNull() throws SQLException {
        return m_record.wasNull();
    }

    public java.sql.Date getDate(int columnIndex) throws SQLException {
        return m_record.getDate(columnIndex);
    }

    public Date getDate(String columnName) throws SQLException {
        return m_record.getDate(columnName);
    }

    public float getFloat(int columnIndex) throws SQLException {
        return m_record.getFloat(columnIndex);
    }

    public float getFloat(String columnName) throws SQLException {
        return m_record.getFloat(columnName);
    }

    public int getInt(int columnIndex) throws SQLException {
        return m_record.getInt(columnIndex);
    }

    public int getInt(String columnName) throws SQLException {
        return m_record.getInt(columnName);
    }

    public long getLong(int columnIndex) throws SQLException {
        return m_record.getLong(columnIndex);
    }

    public long getLong(String columnName) throws SQLException {
        return m_record.getLong(columnName);
    }

    public String getString(int columnIndex) throws SQLException {
        return m_record.getString(columnIndex);
    }

    public String getString(String columnName) throws SQLException {
        return m_record.getString(columnName);
    }

    public Time getTime(int columnIndex) throws SQLException {
        return m_record.getTime(columnIndex);
    }

}
