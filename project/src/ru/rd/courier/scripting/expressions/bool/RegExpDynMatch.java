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

import ru.rd.courier.scripting.BoolExpression;
import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.CourierException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * User: AStepochkin
 * Date: 17.05.2006
 * Time: 14:54:30
 */
public class RegExpDynMatch implements BoolExpression {
    private final ScriptExpression m_expr;
    private final ScriptExpression m_patternExpr;
    private final String m_prefix;

    public RegExpDynMatch(
        ScriptExpression expr, ScriptExpression patternExpr, String prefix) {
        m_expr = expr;
        m_patternExpr = patternExpr;
        m_prefix = prefix;
    }

    public boolean calculate(Context ctx) throws CourierException {
        Pattern pattern = Pattern.compile(m_patternExpr.calculate(ctx));
        Matcher m = pattern.matcher(m_expr.calculate(ctx));

        if (m.matches()) {
            if (m_prefix != null) {
                for (int i = 0; i <= m.groupCount(); i++) {
                    ctx.setVar(m_prefix + i, m.group(i));
                }
            }
            return true;
        }
        return false;
    }
}
