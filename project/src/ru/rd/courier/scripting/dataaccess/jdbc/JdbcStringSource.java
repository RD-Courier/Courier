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
import ru.rd.courier.jdbc.EmptyResultSet;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

public class JdbcStringSource extends JdbcStringReceiver implements DataSource {
    public JdbcStringSource(
        final CourierLogger logger,  final Connection con, String type, boolean autoCommit
    ) throws CourierException {
        super(logger, type, con, true, autoCommit);
    }

    private ResultSet findFirstRs(boolean res, Statement stmt) throws SQLException {

        while (true) {
            if (res) if (res) return stmt.getResultSet();
            if (stmt.getUpdateCount() == -1) return new EmptyResultSet();
            res = stmt.getMoreResults();
        }
    }

    public synchronized ResultSet request(String query) throws CourierException {
        setCancelled(false);
        Statement stmt;
        try {
            stmt = m_con.createStatement();
            if (m_timeout > 0) {
                stmt.setQueryTimeout(m_timeout);
            }
            if (isCancelled()) throw new CourierException("Query cancelled");
            return findFirstRs(stmt.execute(query), stmt);
            //return stmt.executeQuery(query);
        } catch(SQLException e) {
            m_errorsCount++;
            throw new CourierException(e);
        }
    }
}