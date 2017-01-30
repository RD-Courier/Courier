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
import ru.rd.courier.datalinks.JdbcObjectFactory;
import ru.rd.pool.PoolException;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.ObjectPoolIntf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory implements PoolObjectFactory, JdbcObjectFactory {
    protected CourierLogger m_logger;
    protected String m_url = null;

    public ConnectionFactory(CourierLogger logger, String driverName, String url) throws PoolException {
        m_logger = logger;
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            throw new PoolException(e.getMessage(), e);
        }
        m_url = url;
    }

    public final Object getObject(ObjectPoolIntf pool) {
        return getConnection();
    }

    protected Connection getConnection() {
        m_logger.debug("Connecting to " + m_url);
        try {
            return DriverManager.getConnection(m_url);
        } catch (SQLException e) {
            throw new RuntimeException(
                "Error getting connection to: " + m_url, e);
        }
    }

    public final void returnObject(final Object o) {
        try {
            ((PooledConnectionHolder)o).realClose();
        } catch (SQLException e) {
            throw new RuntimeException(
                "Error closing connection to: " + m_url, e);
        }
    }

    public final boolean checkObject(final Object o) {
        return ((PooledConnectionHolder)o).check();
    }

    public String getUrl() {
        return m_url;
    }
}
