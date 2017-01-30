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
package ru.rd.courier.scripting.expressions.bool;

import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.BoolExpression;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptExpression;

public class TestVar implements BoolExpression {
    private String m_varName = null;
    private ScriptExpression m_expr = null;

    public TestVar(final String varName, final ScriptExpression expr) {
        m_varName = varName;
        m_expr = expr;
    }

    public TestVar(final String varName, final String str) {
        this(varName, new ru.rd.courier.scripting.expressions.string.Const(str));
    }

    public boolean calculate(final Context ctx) throws CourierException {
        if (!ctx.hasVar(m_varName)) return false;
        final String varValue = ctx.getVar(m_varName);
        final String exprValue = m_expr.calculate(ctx);
        if (varValue == null) return exprValue == null;
        return varValue.equals(exprValue);
    }
}
