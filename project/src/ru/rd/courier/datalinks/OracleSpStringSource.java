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
package ru.rd.courier.datalinks;

import ru.rd.courier.scripting.dataaccess.jdbc.JdbcStringSource;
import ru.rd.courier.scripting.dataaccess.jdbc.JdbcStringReceiver;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringSimpleParser;

import java.sql.*;

import oracle.jdbc.OracleTypes;

/**
 * User: AStepochkin
 * Date: 20.07.2009
 * Time: 17:04:43
 */
public class OracleSpStringSource extends JdbcStringSource {
    public OracleSpStringSource(
        CourierLogger logger,  Connection con, String type, boolean autoCommit
    ) throws CourierException {
        super(logger, con, type, autoCommit);
    }

    public synchronized ResultSet request(String query) throws CourierException {
        StringSimpleParser p = new StringSimpleParser(query);
        p.skipBlanks();
        if (!p.shiftWord().equalsIgnoreCase("call")) {
            return super.request(query);
        }
        setCancelled(false);
        CallableStatement stmt;
        try {
            stmt = m_con.prepareCall(query);
            if (m_timeout > 0) {
                stmt.setQueryTimeout(m_timeout);
            }
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            if (isCancelled()) throw new CourierException("Query cancelled");
            stmt.execute();
            return (ResultSet)stmt.getObject(1);
        } catch(SQLException e) {
            m_errorsCount++;
            throw new CourierException(e);
        }
    }
}
