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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class AbstractDriver implements Driver {
    private final String m_companyName;
    private final String m_driverName;

    public abstract int getMajorVersion();
    public abstract int getMinorVersion();

    protected AbstractDriver(String companyName, String driverName) {
        if (companyName == null) throw new IllegalArgumentException("Unspecified company name");
        if (driverName == null) throw new IllegalArgumentException("Unspecified driver name");
        m_companyName = companyName;
        m_driverName = driverName;
    }

    protected abstract boolean checkUrl(Properties props, String url) throws SQLException;
    public abstract Connection connect(String url, Properties info) throws SQLException;

    protected boolean checkUrlBase(Properties props, String url, StringBuffer params) throws SQLException {
        String[] urlParts = url.split(":", 4);
        if(urlParts.length < 3) return false;
        if(!urlParts[0].equals("jdbc")) return false;
        if(!urlParts[1].equals(m_companyName)) return false;
        if(!urlParts[2].equals(m_driverName)) return false;
        params.setLength(0);
        if (urlParts.length > 3) {
            params.append(urlParts[3]);
        }
        return true;
    }

    public boolean jdbcCompliant() {
        return true;
    }

    public boolean acceptsURL(String url) throws SQLException {
        boolean res = true;
        try {
            checkUrl(new Properties(), url);
        } catch(SQLException e) {
            res = false;
        }
        return res;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
