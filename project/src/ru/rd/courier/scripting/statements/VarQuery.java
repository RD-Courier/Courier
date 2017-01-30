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

import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.expressions.string.Const;
import ru.rd.courier.CourierException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

/**
 * User: AStepochkin
 * Date: 11.02.2008
 * Time: 16:30:45
 */
public class VarQuery extends QueryStatement {
    private final ScriptExpression m_prefix;

    public VarQuery(ScriptExpression dbName, ScriptExpression sql, ScriptExpression prefix) {
        super(dbName, sql);
        m_prefix = prefix;
    }

    public VarQuery(ScriptExpression dbName, ScriptExpression sql) {
        this(dbName, sql, null);
    }

    public VarQuery(String dbName, ScriptExpression sql) {
        this(new Const(dbName), sql);
    }

    public VarQuery(String dbName, String sql) {
        this(new Const(dbName), new Const(sql));
    }

    protected void processResultSet(ResultSet rs, Context ctx) throws SQLException {
        resultSetToVars(rs, ctx, m_prefix);
    }

    public static void resultSetToVars(final ResultSet rs, final Context ctx) throws SQLException, CourierException {
        resultSetToVars(rs, ctx, null);
    }

    public static void resultSetToVars(final ResultSet rs, final Context ctx, ScriptExpression prefixTmpl) throws SQLException, CourierException {
        long begTime = System.currentTimeMillis();
        boolean hasNext = rs.next();
        ctx.addSourceTime(System.currentTimeMillis() - begTime);
        if (hasNext) {
            GetVarsFromDirectRs mover = new GetVarsFromDirectRs(rs);
            mover.setErrorAsNull(false);
            mover.setPrefix(prefixTmpl, ctx);
            mover.exec(ctx);
        } else {
            String prefix = prefixTmpl == null ? null : prefixTmpl.calculate(ctx);
            final ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                if (ctx.isCanceled()) return;
                String fieldName = md.getColumnName(i);
                if (prefix != null) fieldName = prefix + fieldName;
                ctx.setVar(fieldName, (String)null);
            }
        }
    }
}
