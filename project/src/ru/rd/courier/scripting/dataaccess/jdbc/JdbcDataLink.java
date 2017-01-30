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
package ru.rd.courier.scripting.dataaccess.jdbc;

import ru.rd.courier.CourierException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class JdbcDataLink {
    protected Connection m_con = null;
    protected Statement m_activeStmt = null;
    private Object m_cancelMutex = new Object();
    private boolean m_cancel = false;

    public JdbcDataLink(
        final Connection con
    ) {
        if (con == null) throw new IllegalArgumentException("Connection is null");
        m_con = con;
    }

    protected abstract Statement getStmt();
    protected abstract void innerSetTimeout();
    protected abstract void innerExecute(String operation);
    protected abstract void innerClose();

    protected void doStmt(final String operation) {
        synchronized(m_cancelMutex) { m_cancel = false; }
        Statement stmt = getStmt();
        synchronized(m_cancelMutex) {
            if (m_cancel) return;
            m_activeStmt = stmt;
        }
        try {
            innerSetTimeout();
            synchronized(m_cancelMutex) { if (m_cancel) return; }
            innerExecute(operation);
        } finally {
            m_activeStmt = null;
        }
    }

    public void cancel() throws CourierException {
        Statement stmt = null;
        synchronized(m_cancelMutex) {
            m_cancel = true;
            stmt = m_activeStmt;
        }
        try {
            if (stmt != null) stmt.cancel();
        } catch (SQLException e) {
            throw new CourierException(e.getMessage(), e);
        }
    }

    public void start() {
    }

    public void close() throws CourierException {
        try {
            innerClose();
            m_con.close();
        } catch (SQLException e) {
            throw new CourierException(e);
        }
    }

    public Connection getConnection() {
        return m_con;
    }
}
