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
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.ScriptExpression;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 24.06.2005
 * Time: 18:41:25
 */
public abstract class ResultSetLoopSkeleton implements ScriptStatement {
    protected ScriptStatement m_stmt = null;
    protected String m_breakLabel = null;
    private final String m_recCountVarName;
    private final String m_lastRecordVarName;
    private final boolean m_errorAsNull;
    private final ScriptExpression m_prefix;
    private String m_xmlvar = null;
    private String m_xmlRecordTag = null;
    private boolean m_xmlAttributes = false;

    protected ResultSetLoopSkeleton(
        ScriptStatement stmt, String breakLabel,
        String recCountVarName, String lastRecordVarName,
        boolean errorAsNull, ScriptExpression prefix
    ) {
        m_stmt = stmt;
        m_breakLabel = (breakLabel == null) || (breakLabel.length() == 0) ? null : breakLabel;
        m_recCountVarName = (recCountVarName == null) || (recCountVarName.length() == 0) ?
            null : recCountVarName;
        m_lastRecordVarName = (lastRecordVarName == null) || (lastRecordVarName.length() == 0) ?
            null : lastRecordVarName;
        m_errorAsNull = errorAsNull;
        m_prefix = prefix;
    }

    protected ResultSetLoopSkeleton(
        ScriptStatement stmt, String breakLabel,
        String recCountVarName, String lastRecordVarName,
        boolean errorAsNull
    ) {
        this(stmt, breakLabel, recCountVarName, lastRecordVarName, errorAsNull, null);
    }

    protected ResultSetLoopSkeleton(
        ScriptStatement stmt, String breakLabel,
        String recCountVarName, String lastRecordVarName
    ) {
        this(stmt, breakLabel, recCountVarName, lastRecordVarName, false);
    }

    public void setXmlVar(String xmlvar) {
        m_xmlvar = xmlvar;
    }

    public void setXmlRecordTag(String xmlRecordTag) {
        m_xmlRecordTag = xmlRecordTag;
    }

    public void setXmlAttributes(boolean xmlAttributes) {
        m_xmlAttributes = xmlAttributes;
    }

    public final void start(
        final ru.rd.courier.scripting.Context ctx
    ) throws CourierException {
        m_stmt.start(ctx);
    }

    public final void finish(
        final ru.rd.courier.scripting.Context ctx
    ) throws CourierException {
        m_stmt.finish(ctx);
    }

    protected abstract ResultSet getResultSet(final Context ctx) throws CourierException;
    protected abstract boolean needStandardCleanUp() throws CourierException;
    protected abstract void cleanUp(final Context ctx) throws CourierException;

    // ***************** ScriptStatement implementation ********
    public void exec(final Context ctx) throws CourierException {
        final ResultSet rs = getResultSet(ctx);
        try {
            if (ctx.isCanceled()) return;

            int recCount = 0;
            if (m_recCountVarName != null) {
                ctx.setVar(m_recCountVarName, Integer.toString(recCount));
            }
            ScriptStatement fillVarsStat = new GetVarsFromDirectRs(
                rs, m_errorAsNull, m_prefix, ctx, m_xmlvar, m_xmlRecordTag, m_xmlAttributes
            );
            long begTime = System.currentTimeMillis();
            boolean isLast = !rs.next();
            ctx.addSourceTime(System.currentTimeMillis() - begTime);
            while (!ctx.isCanceled()) {
                if (isLast) break;

                fillVarsStat.exec(ctx);

                begTime = System.currentTimeMillis();
                isLast = !rs.next();
                ctx.addSourceTime(System.currentTimeMillis() - begTime);

                if (m_lastRecordVarName != null) {
                    ctx.setVar(m_lastRecordVarName, isLast ? "1" : "0");
                }

                recCount++;
                if (m_recCountVarName != null) {
                    ctx.setVar(m_recCountVarName, Integer.toString(recCount));
                }

                ctx.execInnerStmt(m_stmt);

                if (ctx.getBreakLabel() != null ) {
                    if(ctx.getBreakLabel().equals(m_breakLabel)) ctx.setBreak(null);
                    break;
                }
            }
        } catch (SQLException e) {
            throw new CourierException(e);
        } finally {
            if (needStandardCleanUp()) {
                try {
                    if (rs.getStatement() != null) {
                        rs.getStatement().close();
                    } else {
                        rs.close();
                    }
                } catch (Exception e) { ctx.error(e); }
            }

            try { cleanUp(ctx); }
            catch (Exception e) { ctx.error(e); }
        }
    }
}
