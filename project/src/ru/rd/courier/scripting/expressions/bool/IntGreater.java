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

import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.expressions.string.*;
import ru.rd.courier.CourierException;

/**
 * User: AStepochkin
 * Date: 27.03.2008
 * Time: 14:27:14
 */
public class IntGreater {
    private final ScriptExpression m_expr1;
    private final ScriptExpression m_expr2;

    public IntGreater(ScriptExpression expr1, ScriptExpression expr2) {
        m_expr1 = expr1;
        m_expr2 = expr2;
    }

    public IntGreater(ScriptExpression expr1, int val2) {
        m_expr1 = expr1;
        m_expr2 = new ru.rd.courier.scripting.expressions.string.Const(Integer.toString(val2));
    }

    public boolean calculate(Context ctx) throws CourierException {
        return Integer.parseInt(m_expr1.calculate(ctx)) > Integer.parseInt(m_expr2.calculate(ctx));
    }
}
