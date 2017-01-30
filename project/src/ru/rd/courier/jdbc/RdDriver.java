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

import ru.rd.courier.utils.StringHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 21.03.2005
 * Time: 12:56:45
 */
public abstract class RdDriver extends AbstractDriver {
    private static final String c_companyName = "rd";
    private final ConnectionFactory m_conFactory;

    public RdDriver(
        String driverName, ConnectionFactory conFactory
    ) {
        super(c_companyName, driverName);
        if (conFactory == null) {
            throw new IllegalArgumentException("ConnectionFactory cannot be null");
        }
        m_conFactory = conFactory;
    }

    protected interface ConnectionFactory {
        Connection create(Properties info) throws SQLException;
    }

    protected void parseProperties(Properties props, String parsUrlPart) {
        StringHelper.parseParams(props, parsUrlPart);
    }
    
    protected boolean checkUrl(Properties props, String url) throws SQLException {
        StringBuffer params = new StringBuffer();
        if (!checkUrlBase(props, url, params)) return false;
        String pars = params.toString().trim();
        try {
            if (props != null) parseProperties(props, pars);
            checkUrl(props);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected abstract boolean checkUrl(Properties props) throws SQLException;

    public Connection connect(String url, Properties info) throws SQLException {
        if (checkUrl(info, url)) {
            return m_conFactory.create(info);
        } else {
            return null;
        }
    }
}
