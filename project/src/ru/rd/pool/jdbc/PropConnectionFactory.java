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
import ru.rd.pool.PoolException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PropConnectionFactory extends ConnectionFactory {
    private Properties m_info = null;
    private final String m_progNameParamName;
    private final String m_progName;
    private int m_conNumber = 0;

    public PropConnectionFactory(
        CourierLogger logger,
        final String driverName, final String url,
        final Properties info, String progNameParamName
    ) throws PoolException {
        super(logger, driverName, url);
        m_info = info;
        m_progNameParamName = progNameParamName;
        if ((m_progNameParamName != null) && m_info.containsKey(m_progNameParamName)) {
            m_progName = m_info.getProperty(m_progNameParamName);
        } else {
            m_progName = null;
        }
    }

    protected Connection getConnection() {
        if (m_progName != null) {
            m_conNumber++;
            m_info.setProperty(m_progNameParamName, m_progName + " " + m_conNumber);
        }

        m_logger.debug("Connecting to " + m_url + " info = " + m_info.toString());
        try {
            return DriverManager.getConnection(m_url, m_info);
        } catch (SQLException e) {
            throw new RuntimeException("Error getting connection to: " + m_url, e);
        }
    }
}
