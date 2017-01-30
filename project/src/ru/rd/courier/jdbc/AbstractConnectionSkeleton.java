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
package ru.rd.courier.jdbc;

import ru.rd.courier.LoggerConsumer;
import ru.rd.courier.jdbc.mock.MockDatabaseMetaData;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 17.02.2005
 * Time: 15:20:35
 */
public abstract class AbstractConnectionSkeleton implements Connection, LoggerConsumer {
    protected Logger m_logger;
    protected final int m_conNumber;
    private boolean m_closed = false;
    private boolean m_autoCommit = true;
    private String m_catalog = "default";
    private List<Statement> m_stats = new LinkedList<Statement>();
    public static final String c_loggerName = "LoggerName";

    public AbstractConnectionSkeleton(int conNumber) {
        m_logger = Logger.getLogger("ru.rd.courier");
        m_conNumber = conNumber;
    }

    protected void checkClosed() throws SQLException {
        if(m_closed) throw new SQLException("Statement closed");
    }

    public synchronized void discardStatement(Statement stmt) {
        m_stats.remove(stmt);
    }

    public void setLogger(Logger logger) {
        m_logger = logger;
    }

    public Logger getLogger() {
        return m_logger;
    }

    public boolean getActive(){
        return !m_closed;
    }

    public synchronized Statement createStatement() throws SQLException {
        checkClosed();
        Statement res = innerCreateStatement();
        m_stats.add(res);
        return res;
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

    protected abstract Statement innerCreateStatement();

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkClosed();
        m_autoCommit = autoCommit;
    }

    public boolean getAutoCommit() throws SQLException {
        return m_autoCommit;
    }

    public synchronized void commit() throws SQLException {}
    public synchronized void rollback() throws SQLException {}

    public synchronized void close() throws SQLException {
        if(m_closed) return;

        Exception err = null;
        int errCount = 0;
        Iterator it = m_stats.iterator();
        while (it.hasNext()) {
            Statement stmt = (Statement)it.next();
            it.remove();
            try {
                stmt.close();
            } catch(Exception e) {
                errCount++;
                if (err != null) err = e;
                m_logger.log(Level.WARNING, "Error closing #" + m_conNumber + " connection", e);
            }
        }

        m_stats = null;
        m_closed = true;

        if (errCount > 0) {
            SQLException e = new SQLException(
                errCount + " error(s) in connection #" + m_conNumber +
                " occured (see first as cause)"
            );
            e.initCause(err);
            throw e;
        }
    }

    public boolean isClosed() throws SQLException {
        return m_closed;
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return MockDatabaseMetaData.instance();
    }

    public boolean isReadOnly() throws SQLException {
        return false;
    }

    public void setCatalog(String catalog) throws SQLException {
        m_catalog = catalog;
    }

    public String getCatalog() throws SQLException {
        return m_catalog;
    }

    public PreparedStatement prepareStatement(String sql)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String nativeSQL(String sql) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getTransactionIsolation() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setHoldability(int holdability) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getHoldability() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                     int resultSetHoldability) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[])
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[])
            throws SQLException {
        throw new UnsupportedOperationException();
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

    protected Logger getLoggerFromUrl(Properties props) {
        if (props.containsKey(c_loggerName)) {
            return LogManager.getLogManager().getLogger(
                props.getProperty(c_loggerName)
            );
        }
        return null;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
