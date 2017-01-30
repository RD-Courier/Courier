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

import java.util.Map;

public class Case implements ScriptStatement {
    private ScriptExpression m_switchExp = null;
    private Map<String, ScriptStatement> m_caseStats = null;
    private ScriptStatement m_elseStat = null;

    public Case(
        final ScriptExpression switchExp, final Map caseStats, final ScriptStatement elseStat
    ) {
        if (switchExp == null) throw new IllegalArgumentException("Switch expression is not specified");
        m_switchExp = switchExp;
        if (caseStats == null) throw new IllegalArgumentException("Case statements are not specified");
        m_caseStats = caseStats;
        m_elseStat = elseStat;
    }

    public Case(final ScriptExpression switchExp, final Map caseStats) {
        this(switchExp, caseStats, null);
    }

    public void addCase(final String caseConst, final ScriptStatement stmt) {
        m_caseStats.put(caseConst, stmt);
    }

    public void start(final Context ctx) throws CourierException {
        for (ScriptStatement scriptStatement : m_caseStats.values()) {
            scriptStatement.start(ctx);
        }
    }

    public void finish(final Context ctx) throws CourierException {
        for (ScriptStatement scriptStatement : m_caseStats.values()) {
            scriptStatement.finish(ctx);
        }
    }

    public void exec(final Context ctx) throws CourierException {
        final String switchExp = m_switchExp.calculate(ctx);
        if (m_caseStats.containsKey(switchExp)) {
            ctx.execInnerStmt(m_caseStats.get(switchExp));
        } else if (m_elseStat != null) ctx.execInnerStmt(m_elseStat);
    }
}
