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
import ru.rd.courier.scripting.BoolExpression;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptStatement;

public class If implements ScriptStatement {
    private BoolExpression m_boolExp = null;
    private ScriptStatement m_trueStat = null;
    private ScriptStatement m_falseStat = null;

    public If(
        final BoolExpression boolExp,
        final ScriptStatement trueStat, final ScriptStatement falseStat
    ) throws CourierException {
        if (boolExp == null) {
            throw new CourierException("Boolean expression for IF statement not specified");
        }
        m_boolExp = boolExp;
        if (trueStat == null) {
            throw new CourierException("True statement for IF statement not specified");
        }
        m_trueStat = trueStat;
        m_falseStat = falseStat;
    }

    public If(
        final BoolExpression boolExp, final ScriptStatement trueStat
    ) throws CourierException {
        this(boolExp, trueStat, null);
    }

    public void start(final Context ctx) throws CourierException {
        if (m_falseStat != null) m_falseStat.start(ctx);
        m_trueStat.start(ctx);
    }

    public void finish(final ru.rd.courier.scripting.Context ctx) throws CourierException {
        if (m_falseStat != null) m_falseStat.finish(ctx);
        m_trueStat.finish(ctx);
    }

    public void exec(final Context ctx) throws CourierException {
        if (m_boolExp.calculate(ctx)) {
            m_trueStat.exec(ctx);
        } else if (m_falseStat != null) {
            m_falseStat.exec(ctx);
        }
    }
}
