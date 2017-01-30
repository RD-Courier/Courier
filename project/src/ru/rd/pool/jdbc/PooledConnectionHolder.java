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
package ru.rd.pool.jdbc;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.PooledObjectHolder;

import java.sql.*;
import java.util.concurrent.Executor;
import java.util.Properties;

public class PooledConnectionHolder implements Connection, PooledObjectHolder {
    private final CourierLogger m_logger;
    private ObjectPoolIntf m_pool = null;
    private Connection m_con = null;
    private final int m_checkWaitTimeout;
    private final String m_testSql;
    private int m_errorsCount = 0;
    private final int m_maxErrorsCount;
    private boolean m_stale = false;

    public PooledConnectionHolder(
        CourierLogger logger, ObjectPoolIntf pool, Connection con,
        String testSql, int checkWaitTimeout, int maxErrorsCount
    ) {
        m_logger = logger;
        m_pool = pool;
        m_con = con;
        m_testSql = testSql;
        m_checkWaitTimeout = checkWaitTimeout;
        m_maxErrorsCount = maxErrorsCount;
    }

    public Statement createStatement() throws SQLException {
        return m_con.createStatement();
    }

    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        return m_con.prepareStatement(sql);
    }

    public CallableStatement prepareCall(final String sql) throws SQLException {
        return m_con.prepareCall(sql);
    }

    public String nativeSQL(final String sql) throws SQLException {
        return m_con.nativeSQL(sql);
    }

    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        m_con.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return m_con.getAutoCommit();
    }

    public void commit() throws SQLException {
        m_con.commit();
    }

    public void rollback() throws SQLException {
        m_con.rollback();
    }

    public void close() throws SQLException {
        if (isConnectionBroken()) {
            m_pool.releaseAndRemoveObject(this);
        } else {
            m_pool.releaseObject(this);
        }
    }

    public void realClose() throws SQLException {
        m_con.close();
    }

    public boolean isClosed() throws SQLException {
        return m_con.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return m_con.getMetaData();
    }

    public void setReadOnly(final boolean readOnly) throws SQLException {
        m_con.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return m_con.isReadOnly();
    }

    public void setCatalog(final String catalog) throws SQLException {
        m_con.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return m_con.getCatalog();
    }

    public void setTransactionIsolation(final int level) throws SQLException {
        m_con.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return m_con.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return m_con.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        m_con.clearWarnings();
    }

    public Statement createStatement(final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        return m_con.createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(final String sql, final int resultSetType,
                                              final int resultSetConcurrency)
            throws SQLException {
        return m_con.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(final String sql, final int resultSetType,
                                         final int resultSetConcurrency) throws SQLException {
        return m_con.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public java.util.Map<String,Class<?>> getTypeMap() throws SQLException {
        return m_con.getTypeMap();
    }

    public void setTypeMap(java.util.Map<String,Class<?>> map) throws SQLException {
        m_con.setTypeMap(map);
    }

    public void setHoldability(final int holdability) throws SQLException {
        m_con.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return m_con.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return m_con.setSavepoint();
    }

    public Savepoint setSavepoint(final String name) throws SQLException {
        return m_con.setSavepoint(name);
    }

    public void rollback(final Savepoint savepoint) throws SQLException {
        m_con.rollback(savepoint);
    }

    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        m_con.releaseSavepoint(savepoint);
    }

    public Statement createStatement(
        final int resultSetType, final int resultSetConcurrency,
        final int resultSetHoldability
    ) throws SQLException {
        return m_con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(
        final String sql, final int resultSetType,
        final int resultSetConcurrency, final int resultSetHoldability
    ) throws SQLException {
        return m_con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(
        final String sql, final int resultSetType,
        final int resultSetConcurrency,
        final int resultSetHoldability
    ) throws SQLException {
        return m_con.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys)
    throws SQLException {
        return m_con.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes)
    throws SQLException {
        return m_con.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(final String sql, final String[] columnNames)
    throws SQLException {
        return m_con.prepareStatement(sql, columnNames);
    }

    public Clob createClob() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Blob createBlob() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public NClob createNClob() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isValid(int timeout) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new UnsupportedOperationException();
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new UnsupportedOperationException();
    }

    public String getClientInfo(String name) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void notifyErrors(int count) {
        if (m_maxErrorsCount > 0) {
            if (count > 0) m_errorsCount += count;
            else m_errorsCount = 0;
        }
    }

    public int getErrorsCount() {
        return m_errorsCount;
    }

    private boolean isConnectionBroken() {
        return
            m_stale || ((m_maxErrorsCount > 0) && (m_errorsCount > m_maxErrorsCount));
    }

    public boolean check() {
        if (isConnectionBroken()) return false;
        boolean ret = false;
        try {
            Statement stmt = m_con.createStatement();
            try {
                if (m_checkWaitTimeout > 0) stmt.setQueryTimeout(m_checkWaitTimeout);
                stmt.execute(m_testSql);
                ret = true;
            } finally {
                try { if (stmt != null) stmt.close(); }
                catch (Exception e) { m_logger.warning(e); }
            }
        } catch (Exception e) {
            m_logger.error(e);
        }
        return ret;
    }

    public String toString() {
        return (
            "Pooled connection = " + m_pool.getDesc()
            + "; stale = " + m_stale
            + "; con = " + m_con.toString()
            + "; checkWaitTimeout = " + m_checkWaitTimeout
            + "; errorsCount = " + m_errorsCount
            + "; maxErrorsCount = " + m_maxErrorsCount
            + "; testSql" + m_testSql
        );
    }

    public ObjectPoolIntf getPool() {
        return m_pool;
    }

    public PoolObjectFactory getFactory() {
        return getPool().getObjectFactory();
    }

    public Object getObject() {
        return m_con;
    }

    public boolean hasObject() {
        return m_con != null;
    }

    public void release() {
        try {
            close();
        } catch (SQLException e) {
            m_logger.warning(e);
        }
    }

    public void markStale() {
        m_stale = true;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    public int getNetworkTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void abort(Executor executor) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public String getSchema() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setSchema(String schema) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
}
