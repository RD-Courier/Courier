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
package ru.rd.courier.scripting.expressions.string;

import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.AbstractContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FromDb implements ScriptExpression {
    private ScriptExpression m_dbName;
    private String m_rsName;
    private ScriptExpression m_sql;

    public FromDb(
        final ScriptExpression dbName, final String rsName,
        final ScriptExpression sql
    ) {
        m_dbName = dbName;
        m_rsName = rsName;
        m_sql = sql;
    }

    public FromDb(
        final String dbName, final String rsName,
        final ScriptExpression sql
    ) {
        this(new Const(dbName), rsName, sql);
    }

    public String calculate(Context ctx) throws CourierException {
        String sql = m_sql.calculate(ctx);
        if (ctx.isCanceled()) return null;
        ResultSet rs = AbstractContext.createResultSet(ctx, m_dbName.calculate(ctx), sql);
        try {
            if (ctx.isCanceled()) return null;

            long begTime = System.currentTimeMillis();
            String ret = null;
            if (rs.next()) {
                ret = rs.getString(1);
            }
            ctx.addSourceTime(System.currentTimeMillis() - begTime);
            return ret;
        } catch (SQLException e) {
            throw new CourierException(e);
        } finally {
            try {
                AbstractContext.closeResultSet(ctx, rs);
            } catch(Exception e) {
                ctx.warning(e);
            }
        }
    }
}
