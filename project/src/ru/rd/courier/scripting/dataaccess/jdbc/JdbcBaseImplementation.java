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
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.StandardOperationSupport;
import ru.rd.courier.scripting.TimedDataReceiver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public abstract class JdbcBaseImplementation
    extends TimedDataReceiver implements JdbcReceiver, StandardOperationSupport
{
    private final Object m_cancelMutex = new Object();
    private boolean m_cancel = false;
    protected CourierLogger m_logger;
    protected Connection m_con;
    protected int m_timeout;
    protected boolean m_autoCommit;
    private boolean m_realAutoCommit;
    private String m_type;

    public JdbcBaseImplementation(
        CourierLogger logger,
        String type, final Connection con, final boolean autoCommit
    ) throws CourierException {
        m_logger = logger;
        m_type = type;
        m_con = con;
        m_autoCommit = autoCommit;
        try {
            m_realAutoCommit = m_con.getAutoCommit();
            synchAutoCommit();
        } catch (SQLException e) { throw new CourierException(e); }
    }

    public void setAutoCommit(boolean autoCommit) {
        m_autoCommit = autoCommit;
        synchAutoCommit();
    }

    protected void synchAutoCommit() {
        if (m_autoCommit == m_realAutoCommit) return;
        try {
            m_con.setAutoCommit(m_autoCommit);
            m_realAutoCommit = m_autoCommit;
        } catch (SQLException e) { throw new CourierException(e); }
    }

    protected boolean isCancelled() {
        synchronized(m_cancelMutex) { return m_cancel; }
    }

    protected void setCancelled(boolean cancelled) {
        synchronized(m_cancelMutex) { m_cancel = cancelled; }
    }

    public List<LinkWarning> timedFlush() {
        return null;
    }

    public synchronized void setTimeout(final int timeout) throws CourierException {
        m_timeout = timeout;
    }

    public void cancel() throws CourierException {
        setCancelled(true);
    }

    public String getType() {
        return m_type;
    }

    public Connection getConnection() {
        return m_con;
    }

    public String toString() {
        return (
            "connection: " + m_con
            + "; cancelled = " + isCancelled()
            + "; timeout = " + m_timeout
            + "; autoCommit = " + m_autoCommit
            + "; type = " + m_type
        );
    }
}