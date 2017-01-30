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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 17.02.2005
 * Time: 15:10:49
 */
public abstract class AbstractJdbcStatement extends EmptyStatement {
private Logger m_logger;
    protected AbstractConnectionSkeleton m_con;
    private boolean m_closed = false;
    private int m_pos;
    private List m_results;
    private int m_queryTimeout = Integer.MAX_VALUE;
    private SQLWarning m_warning, m_lastWarning;

    public synchronized void addWarning(SQLWarning warning) {
        if (m_warning == null) {
            m_warning = warning;
            m_lastWarning = warning;
            return;
        }
        m_lastWarning.setNextWarning(warning);
    }

    public synchronized void clearWarnings() {
        m_warning = null;
    }

    private void checkClosed() throws SQLException {
        if(m_closed) throw new SQLException("Statement closed");
    }

    private void cleanUpResultSet(ResultSet rs) {
        try {
            rs.close();
        } catch(Exception e) {
            getLogger().log(
                Level.SEVERE, "Error cleanining statement results", e
            );
        }
    }

    private void cleanUpResults() {
        if (m_results != null) {
            for (ListIterator it = m_results.listIterator(m_results.size()); it.hasPrevious(); ) {
                Object o = it.previous();
                if(o instanceof ResultSet) {
                    cleanUpResultSet((ResultSet)o);
                }
            }
        }
        m_results = null;
    }

    public AbstractJdbcStatement(Logger logger, AbstractConnectionSkeleton con) {
        m_logger = logger;
        if(con == null) throw new IllegalArgumentException("Connection is null");
        m_con = con;
    }

    protected Logger getLogger() {
        return m_logger;
    }

    private synchronized void setResults(List results) {
        cleanUpResults();
        m_results = results;
    }

    private synchronized List getResults() {
        return m_results;
    }

    protected SQLException getException(String mes) {
        return new SQLException(
            "Statement of connection '" + m_con.toString() + "' error: " + mes
        );
    }

    protected abstract List innerProcessRequest(String sql) throws SQLException;

    public boolean execute(String sql) throws SQLException {
        clearWarnings();
        setResults(innerProcessRequest(sql));
        m_pos = 0;
        return (
            (m_results != null) && (m_results.size() > 0)
            && (m_results.get(0) instanceof ResultSet)
        );
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        if (!execute(sql)) {
            cleanUpResults();
            throw getException(
                "Sql:\n" + sql + "\nhas not produced result set"
            );
        }
        if ((m_results.size() != 1)) {
            int resListSize = m_results.size();
            cleanUpResults();
            throw getException(
                "Sql:\n" + sql +
                "\nhas not produced one result set: result list size = " + resListSize
            );
        }
        return (ResultSet)m_results.get(0);
    }

    public int executeUpdate(String sql) throws SQLException {
        execute(sql);
        int res = -1;
        try {
            for (ListIterator it = m_results.listIterator(m_results.size()); it.hasPrevious(); ) {
                Object o = it.previous();
                if(o instanceof UpdateResult) {
                    res = ((UpdateResult)o).getResult();
                    break;
                }
            }
        } finally {
            cleanUpResults();
        }
        if (res < 0) throw getException("sql has not produced update results");
        return res;
    }

    public synchronized ResultSet getResultSet() throws SQLException {
        checkClosed();
        return (
            (m_pos < m_results.size()) && (m_results.get(m_pos) instanceof ResultSet)
            ? (ResultSet)m_results.get(m_pos) : null
        );
    }

    public synchronized int getUpdateCount() throws SQLException {
        checkClosed();
        return (
            (m_pos < m_results.size()) && (m_results.get(m_pos) instanceof UpdateResult)
            ? ((UpdateResult)m_results.get(m_pos)).getResult() : -1
        );
    }

    public synchronized boolean getMoreResults() throws SQLException {
        checkClosed();
        if(m_pos < m_results.size()) {
            try {
                Object o = m_results.get(m_pos);
                if(o instanceof ResultSet) ((ResultSet)o).close();
            } finally {
                m_pos++;
            }
        }
        return (
            (m_pos < m_results.size()) && (m_results.get(m_pos) instanceof ResultSet)
        );
    }

    public final void close() throws SQLException {
        //m_logger.fine("Enterring " + getClass().getName() + ".Close");
        synchronized (this) {
            if(isClosed()) return;
            m_closed = true;
        }
        cleanUpResults();
        getConnection().discardStatement(this);
        customClose();
    }

    protected void customClose() {}

    public synchronized boolean isClosed() {
        return m_closed;
    }

    public int getMaxFieldSize() throws SQLException {
        return Integer.MAX_VALUE;
    }

    public int getMaxRows() throws SQLException {
        return Integer.MAX_VALUE;
    }

    public synchronized int getQueryTimeout() throws SQLException {
        return m_queryTimeout;
    }

    public synchronized void setQueryTimeout(int seconds) throws SQLException {
        m_queryTimeout = seconds;
    }

    public void cancel() throws SQLException {
        checkClosed();
        // !!! maybe implement
    }

    public synchronized SQLWarning getWarnings() throws SQLException {
        return m_warning;
    }

    public AbstractConnectionSkeleton getConnection() throws SQLException {
        return m_con;
    }
}
