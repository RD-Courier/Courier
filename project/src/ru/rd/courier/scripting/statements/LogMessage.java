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
import ru.rd.courier.scripting.expressions.string.PreparedTemplateExpr;

public class LogMessage implements ScriptStatement {
    public static final String cDEBUG = "debug";
    public static final String cINFO = "info";
    public static final String cWARNING = "warning";
    public static final String cSEVERE = "severe";

    private final ScriptExpression m_message;
    private final String m_severity;

    public LogMessage(
        String severity, ScriptExpression message
    ) throws CourierException {
        m_message = message;
        m_severity = severity.toLowerCase();
        if (
            !(
                m_severity.equals(cDEBUG) ||
                m_severity.equals(cINFO) ||
                m_severity.equals(cWARNING) ||
                m_severity.equals(cSEVERE)
            )
        ) {
            throw new CourierException("Unknown log severity '" + m_severity + "'");
        }
    }

    public LogMessage(String severity, String message) throws CourierException {
        this(severity, new PreparedTemplateExpr(message));
    }

    public void start(Context ctx) throws CourierException {}
    public void finish(Context ctx) throws CourierException {}

    public void exec(Context ctx) throws CourierException {
        String message = m_message.calculate(ctx);
        if (m_severity.equals(cDEBUG)) {
            ctx.debug(message);
        } else if (m_severity.equals(cINFO)) {
            ctx.info(message);
        } else if (m_severity.equals(cWARNING)) {
            ctx.warning(message);
        } else if (m_severity.equals(cSEVERE)) {
            ctx.error(message);
        }
    }
}
