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

import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.AbstractContext;
import ru.rd.courier.scripting.expressions.string.Const;

import java.sql.ResultSet;

public class CtxResultSetLoop extends ResultSetLoopSkeleton {
    private final ScriptExpression m_dbName;
    private final String m_rsName;
    private final ScriptExpression m_sql;

    public CtxResultSetLoop(
        final ScriptExpression dbName, final String rsName,
        final ScriptExpression sql,
        final ScriptStatement stmt, final String label,
        String recCountVarName, String lastRecordVarName,
        boolean errorAsNull, ScriptExpression prefix,
        String xmlvar, String xmlRecordTag, boolean xmlAttributes
    ) {
        this(dbName, rsName, sql, stmt,label, recCountVarName, lastRecordVarName, errorAsNull, prefix);
        setXmlVar(xmlvar);
        setXmlRecordTag(xmlRecordTag);
        setXmlAttributes(xmlAttributes);
    }

    public CtxResultSetLoop(
        final ScriptExpression dbName, final String rsName,
        final ScriptExpression sql,
        final ScriptStatement stmt, final String label,
        String recCountVarName, String lastRecordVarName,
        boolean errorAsNull, ScriptExpression prefix
    ) {
        super(stmt, label, recCountVarName, lastRecordVarName, errorAsNull, prefix);
        m_dbName = dbName;
        m_rsName = rsName;
        m_sql = sql;
    }

    public CtxResultSetLoop(
        final ScriptExpression dbName, final String rsName,
        final ScriptExpression sql,
        final ScriptStatement stmt, final String label,
        String recCountVarName, String lastRecordVarName,
        boolean errorAsNull
    ) {
        this(
            dbName, rsName, sql, stmt,
            label, recCountVarName, lastRecordVarName, errorAsNull, null
        );
    }

    public CtxResultSetLoop(
        final ScriptExpression dbName, final String rsName,
        final ScriptExpression sql,
        final ScriptStatement stmt, final String label,
        String recCountVarName, String lastRecordVarName
    ) {
        this(
            dbName, rsName, sql, stmt,
            label, recCountVarName, lastRecordVarName, false
        );
    }

    public CtxResultSetLoop(
        final String dbName, final String rsName,
        final ScriptExpression sql,
        final ScriptStatement stmt, final String label,
        String recCountVarName, String lastRecordVarName
    ) {
        this(
            new Const(dbName), rsName, sql, stmt,
            label, recCountVarName, lastRecordVarName
        );
    }

    public CtxResultSetLoop(
        final String dbName, final String rsName,
        final ScriptExpression sql,
        final ScriptStatement stmt, final String label, String recCountVarName
    ) {
        this(dbName, rsName, sql, stmt, label, recCountVarName, null);
    }

    public CtxResultSetLoop(
        final String dbName, final String rsName,
        final ScriptExpression sql,
        final ScriptStatement stmt, final String label
    ) {
        this(dbName, rsName, sql, stmt, label, null);
    }

    public CtxResultSetLoop(
        final String dbName, final String rsName,
        final ScriptExpression sql, final ScriptStatement stmt
    ) {
        this(dbName, rsName, sql, stmt, null);
    }

    public CtxResultSetLoop(
        final String dbName, final String rsName,
        final String sql, final ScriptStatement stmt
    ) {
        this(dbName, rsName, new Const(sql), stmt, null);
    }

    protected ResultSet getResultSet(final Context ctx) throws CourierException {
        if (m_rsName == null) {
            return AbstractContext.createResultSet(ctx, m_dbName.calculate(ctx), m_sql.calculate(ctx));
        } else {
            return AbstractContext.createResultSet(
                ctx, m_dbName.calculate(ctx), m_rsName, m_sql.calculate(ctx)
            );
        }
    }

    protected boolean needStandardCleanUp() throws CourierException {
        return m_rsName == null;
    }

    protected void cleanUp(Context ctx) throws CourierException {
        if (m_rsName != null) {
            ctx.removeResultSet(m_rsName);
        }
    }
}
