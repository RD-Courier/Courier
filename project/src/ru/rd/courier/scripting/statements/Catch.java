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
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.expressions.bool.Const;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.ErrorHelper;

/**
 * User: AStepochkin
 * Date: 12.07.2005
 * Time: 14:07:22
 */
public class Catch implements ScriptStatement {
    private final ScriptStatement m_stmt;
    private final ScriptStatement m_catchStmt;
    private final BoolExpression m_rethrow;
    private final BoolExpression m_supressLogging;
    private final String m_varName;

    public Catch(
        final ScriptStatement stmt, ScriptStatement finallyStmt,
        BoolExpression rethrow, BoolExpression supressLogging, String varName
    ) throws CourierException {
        if (stmt == null) {
            throw new CourierException("Executable statement not specified");
        }
        if (finallyStmt == null) {
            throw new CourierException("Finally statement not specified");
        }
        m_stmt = stmt;
        m_catchStmt = finallyStmt;
        m_rethrow = rethrow;
        m_supressLogging = supressLogging;
        m_varName = varName;
    }

    public Catch(
        final ScriptStatement stmt, ScriptStatement finallyStmt,
        BoolExpression rethrow
    ) throws CourierException {
        this(stmt, finallyStmt, rethrow, new Const(false), null);
    }

    public void start(Context ctx) throws CourierException {
        m_stmt.start(ctx);
        m_catchStmt.start(ctx);
    }

    public void exec(Context ctx) throws CourierException {
        try {
            ctx.execInnerStmt(m_stmt);
        } catch (Exception e) {
            if (m_varName != null) {
                ctx.setVar(m_varName, ErrorHelper.getOriginalCause(e).getMessage());
            }
            boolean rethrow = true;
            rethrow = m_rethrow.calculate(ctx);
            boolean supressLogging = m_supressLogging.calculate(ctx);
            if (!supressLogging && !rethrow) ctx.error(e);
            try { ctx.execInnerStmt(m_catchStmt); }
            catch(Exception e1) { ctx.error(e1); }
            if (rethrow) throw new CourierException(e);
        }
    }

    public void finish(Context ctx) throws CourierException {
        m_stmt.finish(ctx);
        m_catchStmt.finish(ctx);
    }
}
