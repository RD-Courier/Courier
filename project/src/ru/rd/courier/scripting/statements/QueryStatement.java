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
package ru.rd.courier.scripting.statements;

import ru.rd.courier.scripting.*;
import ru.rd.courier.CourierException;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 17.02.2006
 * Time: 14:05:09
 */
public abstract class QueryStatement implements ScriptStatement {
    private final ScriptExpression m_dbName;
    private final ScriptExpression m_sql;

    public QueryStatement(ScriptExpression dbName, ScriptExpression sql) {
        m_dbName = dbName;
        m_sql = sql;
    }

    public void start(Context ctx) throws CourierException {}
    public void finish(Context ctx) throws CourierException {}

    public final void exec(final Context ctx) throws CourierException {
        String sql = m_sql.calculate(ctx);
        if (ctx.isCanceled()) return;
        ResultSet rs = AbstractContext.createResultSet(ctx, m_dbName.calculate(ctx), sql);
        try {
            if (ctx.isCanceled()) return;
            processResultSet(rs, ctx);
        } catch (SQLException e) {
            throw new CourierException(e);
        } finally {
            try { AbstractContext.closeResultSet(ctx, rs); }
            catch(Exception e) { ctx.warning(e); }
        }
    }

    protected abstract void processResultSet(ResultSet rs, Context ctx) throws SQLException;
}
