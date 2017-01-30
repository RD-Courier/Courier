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
package ru.rd.courier.jdbc.LotusNotes;

import ru.rd.courier.jdbc.EmptyResultSet;

import java.sql.*;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Calendar;
import java.net.URL;

/**
 * User: AStepochkin
 * Date: 05.10.2005
 * Time: 16:57:25
 */
public class StatementStoringResultSetDecorator extends EmptyResultSet {
    private final ResultSet m_rs;
    private final Statement m_stmt;

    public boolean next() throws SQLException {
        return m_rs.next();
    }

    public void close() throws SQLException {
        m_rs.close();
    }

    public boolean wasNull() throws SQLException {
        return m_rs.wasNull();
    }

    public String getString(int columnIndex) throws SQLException {
        return m_rs.getString(columnIndex);
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
        return m_rs.getBoolean(columnIndex);
    }

    public byte getByte(int columnIndex) throws SQLException {
        return m_rs.getByte(columnIndex);
    }

    public short getShort(int columnIndex) throws SQLException {
        return m_rs.getShort(columnIndex);
    }

    public int getInt(int columnIndex) throws SQLException {
        return m_rs.getInt(columnIndex);
    }

    public long getLong(int columnIndex) throws SQLException {
        return m_rs.getLong(columnIndex);
    }

    public float getFloat(int columnIndex) throws SQLException {
        return m_rs.getFloat(columnIndex);
    }

    public double getDouble(int columnIndex) throws SQLException {
        return m_rs.getDouble(columnIndex);
    }

    @Deprecated public
    BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return m_rs.getBigDecimal(columnIndex, scale);
    }

    public byte[] getBytes(int columnIndex) throws SQLException {
        return m_rs.getBytes(columnIndex);
    }

    public Date getDate(int columnIndex) throws SQLException {
        return m_rs.getDate(columnIndex);
    }

    public Time getTime(int columnIndex) throws SQLException {
        return m_rs.getTime(columnIndex);
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return m_rs.getTimestamp(columnIndex);
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return m_rs.getAsciiStream(columnIndex);
    }

    @Deprecated public
    InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return m_rs.getUnicodeStream(columnIndex);
    }

    public InputStream getBinaryStream(int columnIndex)
        throws SQLException {
        return m_rs.getBinaryStream(columnIndex);
    }

    public String getString(String columnName) throws SQLException {
        return m_rs.getString(columnName);
    }

    public boolean getBoolean(String columnName) throws SQLException {
        return m_rs.getBoolean(columnName);
    }

    public byte getByte(String columnName) throws SQLException {
        return m_rs.getByte(columnName);
    }

    public short getShort(String columnName) throws SQLException {
        return m_rs.getShort(columnName);
    }

    public int getInt(String columnName) throws SQLException {
        return m_rs.getInt(columnName);
    }

    public long getLong(String columnName) throws SQLException {
        return m_rs.getLong(columnName);
    }

    public float getFloat(String columnName) throws SQLException {
        return m_rs.getFloat(columnName);
    }

    public double getDouble(String columnName) throws SQLException {
        return m_rs.getDouble(columnName);
    }

    @Deprecated public
    BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
        return m_rs.getBigDecimal(columnName, scale);
    }

    public byte[] getBytes(String columnName) throws SQLException {
        return m_rs.getBytes(columnName);
    }

    public Date getDate(String columnName) throws SQLException {
        return m_rs.getDate(columnName);
    }

    public Time getTime(String columnName) throws SQLException {
        return m_rs.getTime(columnName);
    }

    public Timestamp getTimestamp(String columnName) throws SQLException {
        return m_rs.getTimestamp(columnName);
    }

    public InputStream getAsciiStream(String columnName) throws SQLException {
        return m_rs.getAsciiStream(columnName);
    }

    @Deprecated public
    InputStream getUnicodeStream(String columnName) throws SQLException {
        return m_rs.getUnicodeStream(columnName);
    }

    public InputStream getBinaryStream(String columnName)
        throws SQLException {
        return m_rs.getBinaryStream(columnName);
    }

    public SQLWarning getWarnings() throws SQLException {
        return m_rs.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        m_rs.clearWarnings();
    }

    public String getCursorName() throws SQLException {
        return m_rs.getCursorName();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return m_rs.getMetaData();
    }

    public Object getObject(int columnIndex) throws SQLException {
        return m_rs.getObject(columnIndex);
    }

    public Object getObject(String columnName) throws SQLException {
        return m_rs.getObject(columnName);
    }

    public int findColumn(String columnName) throws SQLException {
        return m_rs.findColumn(columnName);
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return m_rs.getCharacterStream(columnIndex);
    }

    public Reader getCharacterStream(String columnName) throws SQLException {
        return m_rs.getCharacterStream(columnName);
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return m_rs.getBigDecimal(columnIndex);
    }

    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        return m_rs.getBigDecimal(columnName);
    }

    public boolean isBeforeFirst() throws SQLException {
        return m_rs.isBeforeFirst();
    }

    public boolean isAfterLast() throws SQLException {
        return m_rs.isAfterLast();
    }

    public boolean isFirst() throws SQLException {
        return m_rs.isFirst();
    }

    public boolean isLast() throws SQLException {
        return m_rs.isLast();
    }

    public void beforeFirst() throws SQLException {
        m_rs.beforeFirst();
    }

    public void afterLast() throws SQLException {
        m_rs.afterLast();
    }

    public boolean first() throws SQLException {
        return m_rs.first();
    }

    public boolean last() throws SQLException {
        return m_rs.last();
    }

    public int getRow() throws SQLException {
        return m_rs.getRow();
    }

    public boolean absolute( int row ) throws SQLException {
        return m_rs.absolute(row);
    }

    public boolean relative( int rows ) throws SQLException {
        return m_rs.relative(rows);
    }

    public boolean previous() throws SQLException {
        return m_rs.previous();
    }

    public void setFetchDirection(int direction) throws SQLException {
        m_rs.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return m_rs.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        m_rs.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return m_rs.getFetchSize();
    }

    public int getType() throws SQLException {
        return m_rs.getType();
    }

    public int getConcurrency() throws SQLException {
        return m_rs.getConcurrency();
    }

    public boolean rowUpdated() throws SQLException {
        return m_rs.rowUpdated();
    }

    public boolean rowInserted() throws SQLException {
        return m_rs.rowInserted();
    }

    public boolean rowDeleted() throws SQLException {
        return m_rs.rowDeleted();
    }

    public void updateNull(int columnIndex) throws SQLException {
        m_rs.updateNull(columnIndex);
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        m_rs.updateBoolean(columnIndex, x);
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
        m_rs.updateByte(columnIndex, x);
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
        m_rs.updateShort(columnIndex, x);
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        m_rs.updateInt(columnIndex, x);
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        m_rs.updateLong(columnIndex, x);
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
        m_rs.updateFloat(columnIndex, x);
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
        m_rs.updateDouble(columnIndex, x);
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        m_rs.updateBigDecimal(columnIndex, x);
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        m_rs.updateString(columnIndex, x);
    }

    public void updateBytes(int columnIndex, byte x[]) throws SQLException {
        m_rs.updateBytes(columnIndex, x);
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        m_rs.updateDate(columnIndex, x);
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
        m_rs.updateTime(columnIndex, x);
    }

    public void updateTimestamp(int columnIndex, Timestamp x)
      throws SQLException {
        m_rs.updateTimestamp(columnIndex, x);
    }

    public void updateAsciiStream(int columnIndex,
			   InputStream x,
			   int length) throws SQLException {
        m_rs.updateAsciiStream(columnIndex, x, length);
    }

    public void updateBinaryStream(int columnIndex,
			    InputStream x,
			    int length) throws SQLException {
        m_rs.updateBinaryStream(columnIndex, x, length);
    }

    public void updateCharacterStream(int columnIndex,
			     Reader x,
			     int length) throws SQLException {
        m_rs.updateCharacterStream(columnIndex, x, length);
    }

    public void updateObject(int columnIndex, Object x, int scale)
      throws SQLException {
        m_rs.updateObject(columnIndex, x, scale);
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        m_rs.updateObject(columnIndex, x);
    }

    public void updateNull(String columnName) throws SQLException {
        m_rs.updateNull(columnName);
    }

    public void updateBoolean(String columnName, boolean x) throws SQLException {
        m_rs.updateBoolean(columnName, x);
    }

    public void updateByte(String columnName, byte x) throws SQLException {
        m_rs.updateByte(columnName, x);
    }

    public void updateShort(String columnName, short x) throws SQLException {
        m_rs.updateShort(columnName, x);
    }

    public void updateInt(String columnName, int x) throws SQLException {
        m_rs.updateInt(columnName, x);
    }

    public void updateLong(String columnName, long x) throws SQLException {
        m_rs.updateLong(columnName, x);
    }

    public void updateFloat(String columnName, float x) throws SQLException {
        m_rs.updateFloat(columnName, x);
    }

    public void updateDouble(String columnName, double x) throws SQLException {
        m_rs.updateDouble(columnName, x);
    }

    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
        m_rs.updateBigDecimal(columnName, x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        m_rs.updateString(columnName, x);
    }

    public void updateBytes(String columnName, byte x[]) throws SQLException {
        m_rs.updateBytes(columnName, x);
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        m_rs.updateDate(columnName, x);
    }

    public void updateTime(String columnName, Time x) throws SQLException {
        m_rs.updateTime(columnName, x);
    }

    public void updateTimestamp(String columnName, Timestamp x)
      throws SQLException {
        m_rs.updateTimestamp(columnName, x);
    }

    public void updateAsciiStream(String columnName,
			   InputStream x,
			   int length) throws SQLException {
        m_rs.updateAsciiStream(columnName, x, length);
    }

    public void updateBinaryStream(String columnName,
			    InputStream x,
			    int length) throws SQLException {
        m_rs.updateBinaryStream(columnName, x, length);
    }

    public void updateCharacterStream(String columnName,
			     Reader reader,
			     int length) throws SQLException {
        m_rs.updateCharacterStream(columnName, reader, length);
    }

    public void updateObject(String columnName, Object x, int scale)
      throws SQLException {
        m_rs.updateObject(columnName, x, scale);
    }

    public void updateObject(String columnName, Object x) throws SQLException {
        m_rs.updateObject(columnName, x);
    }

    public void insertRow() throws SQLException {
        m_rs.insertRow();
    }

    public void updateRow() throws SQLException {
        m_rs.updateRow();
    }

    public void deleteRow() throws SQLException {
        m_rs.deleteRow();
    }

    public void refreshRow() throws SQLException {
        m_rs.refreshRow();
    }

    public void cancelRowUpdates() throws SQLException {
        m_rs.cancelRowUpdates();
    }

    public void moveToInsertRow() throws SQLException {
        m_rs.moveToInsertRow();
    }

    public void moveToCurrentRow() throws SQLException {
        m_rs.moveToCurrentRow();
    }

    public Statement getStatement() throws SQLException {
        return m_stmt;
    }

    public Object getObject(int i, Map<String,Class<?>> map)
	throws SQLException {
        return m_rs.getObject(i, map);
    }

    public Ref getRef(int i) throws SQLException {
        return m_rs.getRef(i);
    }

    public Blob getBlob(int i) throws SQLException {
        return m_rs.getBlob(i);
    }

    public Clob getClob(int i) throws SQLException {
        return m_rs.getClob(i);
    }

    public Array getArray(int i) throws SQLException {
        return m_rs.getArray(i);
    }

    public Object getObject(String colName, Map<String,Class<?>> map)
	throws SQLException {
        return m_rs.getObject(colName, map);
    }

    public Ref getRef(String colName) throws SQLException {
        return m_rs.getRef(colName);
    }

    public Blob getBlob(String colName) throws SQLException {
        return m_rs.getBlob(colName);
    }

    public Clob getClob(String colName) throws SQLException {
        return m_rs.getClob(colName);
    }

    public Array getArray(String colName) throws SQLException {
        return m_rs.getArray(colName);
    }

    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return m_rs.getDate(columnIndex, cal);
    }

    public Date getDate(String columnName, Calendar cal) throws SQLException {
        return m_rs.getDate(columnName, cal);
    }

    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return m_rs.getTime(columnIndex, cal);
    }

    public Time getTime(String columnName, Calendar cal) throws SQLException {
        return m_rs.getTime(columnName, cal);
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal)
      throws SQLException {
        return m_rs.getTimestamp(columnIndex, cal);
    }

    public Timestamp getTimestamp(String columnName, Calendar cal)
      throws SQLException {
        return m_rs.getTimestamp(columnName, cal);
    }

    public URL getURL(int columnIndex) throws SQLException {
        return m_rs.getURL(columnIndex);
    }

    public URL getURL(String columnName) throws SQLException {
        return m_rs.getURL(columnName);
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        m_rs.updateRef(columnIndex, x);
    }

    public void updateRef(String columnName, Ref x) throws SQLException {
        m_rs.updateRef(columnName, x);
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        m_rs.updateBlob(columnIndex, x);
    }

    public void updateBlob(String columnName, Blob x) throws SQLException {
        m_rs.updateBlob(columnName, x);
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        m_rs.updateClob(columnIndex, x);
    }

    public void updateClob(String columnName, Clob x) throws SQLException {
        m_rs.updateClob(columnName, x);
    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
        m_rs.updateArray(columnIndex, x);
    }

    public void updateArray(String columnName, Array x) throws SQLException {
        m_rs.updateArray(columnName, x);
    }

    public StatementStoringResultSetDecorator(Statement stmt, ResultSet rs) {
        m_stmt = stmt;
        m_rs = rs;
    }
}
