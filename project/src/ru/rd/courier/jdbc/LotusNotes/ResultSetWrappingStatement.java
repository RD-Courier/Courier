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

import java.sql.*;

/**
 * User: AStepochkin
 * Date: 05.10.2005
 * Time: 17:16:13
 */
public class ResultSetWrappingStatement implements Statement {
    private final Statement m_stmt;

    public ResultSet executeQuery(String sql) throws SQLException {
        return new StatementStoringResultSetDecorator(m_stmt, m_stmt.executeQuery(sql));
    }

    public int executeUpdate(String sql) throws SQLException {
        return m_stmt.executeUpdate(sql);
    }

    public void close() throws SQLException {
        m_stmt.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return m_stmt.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        m_stmt.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return m_stmt.getMaxRows();
    }

    public void setMaxRows(int max) throws SQLException {
        m_stmt.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        m_stmt.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return m_stmt.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        m_stmt.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        m_stmt.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return m_stmt.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        m_stmt.clearWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        m_stmt.setCursorName(name);
    }

    public boolean execute(String sql) throws SQLException {
        return m_stmt.execute(sql);
    }

    private ResultSet m_rs = null;

    public ResultSet getResultSet() throws SQLException {
        if (m_rs == null) {
            m_rs = new StatementStoringResultSetDecorator(m_stmt, m_stmt.getResultSet());
        }
        return m_rs;
    }

    public int getUpdateCount() throws SQLException {
        return m_stmt.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        m_rs = null;
        return m_stmt.getMoreResults();
    }

    public void setFetchDirection(int direction) throws SQLException {
        m_stmt.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return m_stmt.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        m_stmt.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return m_stmt.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return m_stmt.getResultSetConcurrency();
    }

    public int getResultSetType()  throws SQLException {
        return m_stmt.getResultSetType();
    }

    public void addBatch( String sql ) throws SQLException {
        m_stmt.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        m_stmt.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        return m_stmt.executeBatch();
    }

    public Connection getConnection()  throws SQLException {
        return m_stmt.getConnection();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return m_stmt.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return m_stmt.getGeneratedKeys();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return m_stmt.executeUpdate(sql, autoGeneratedKeys);
    }

    public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
        return m_stmt.executeUpdate(sql, columnIndexes);
    }

    public int executeUpdate(String sql, String columnNames[]) throws SQLException {
        return m_stmt.executeUpdate(sql, columnNames);
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return m_stmt.execute(sql, autoGeneratedKeys);
    }

    public boolean execute(String sql, int columnIndexes[]) throws SQLException {
        return m_stmt.execute(sql, columnIndexes);
    }

    public boolean execute(String sql, String columnNames[]) throws SQLException {
        return m_stmt.execute(sql, columnNames);
    }

    public int getResultSetHoldability() throws SQLException {
        return m_stmt.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setPoolable(boolean poolable) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSetWrappingStatement(Statement stmt) {
        m_stmt = stmt;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    public boolean isCloseOnCompletion() throws SQLException {
        throw new UnsupportedOperationException();        
    }

    public void closeOnCompletion() throws SQLException {
        throw new UnsupportedOperationException();        
    }
}
