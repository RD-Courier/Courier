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

import ru.rd.courier.*;
import ru.rd.courier.schedule.TimeRelaunchLimit;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.ScriptStatement;

import java.util.HashMap;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 01.11.2005
 * Time: 16:40:28
 */
public class LaunchProcess implements ScriptStatement {
    private final String m_courierObjectName;
    private final ScriptExpression m_pipeName;
    private final ScriptExpression m_ruleName;
    private final Integer m_ignoreErrorCount;
    private final ScriptStatement m_stmt;
    private final ScriptExpression m_importVars;
    private final SupervisorFactory m_svFactory;

    public LaunchProcess(
        String courierObjectName, ScriptExpression pipeName, ScriptExpression ruleName,
        int ignoreErrorCount, ScriptStatement stmt, ScriptExpression importVars,
        final String errorRelaunchInterval, final String errorRelaunchLimitInterval
    ) {
        m_courierObjectName = courierObjectName;
        m_pipeName = pipeName;
        m_ruleName = ruleName;
        m_ignoreErrorCount = ignoreErrorCount;
        m_stmt = stmt;
        m_importVars = importVars;
        if (errorRelaunchInterval == null) {
            m_svFactory = new SupervisorFactory() {
                public TransferProcessSupervisor create() {
                    return null;
                }
            };
        } else {
            m_svFactory = new SupervisorFactory() {
                private final long m_interval = StringHelper.parseTimeSecondsDef(errorRelaunchInterval);
                private final long m_limitInterval =
                    errorRelaunchLimitInterval == null || errorRelaunchLimitInterval.length() == 0
                        ? -1 : StringHelper.parseTimeSecondsDef(errorRelaunchLimitInterval);

                public TransferProcessSupervisor create() {
                    return new GenericSupervisor(
                        m_interval, new ErrorRelaunchChecker(),
                        m_limitInterval > 0 ? new TimeRelaunchLimit(m_limitInterval) : null
                    );
                }
            };
        }
    }

    // do not start and finish m_stmt because it will be at start of requested process
    public void start(Context ctx) throws CourierException {}
    public void finish(Context ctx) throws CourierException {}

    public void exec(Context ctx) throws CourierException {
        SystemDb sdb = ((CourierContext)ctx.getObject(m_courierObjectName)).getSystemDb();

        Map<String, ScriptExpression> pars = new HashMap<String, ScriptExpression>();
        String importVars = null;
        if (m_importVars != null) importVars = m_importVars.calculate(ctx);
        if (importVars == null) {
            for(String varName: ctx.getVarKeySet()) {
                if (varName.length() == 0 || varName.charAt(0) == '$') continue;
                pars.put(varName, ctx.getVarExpression(varName));
            }
        } else {
            String[] importVarsArr = importVars.split("\\s*,\\s*");
            for(String varName: importVarsArr) {
                if (varName.length() == 0 || varName.charAt(0) == '$') continue;
                pars.put(varName, ctx.getVarExpression(varName));
            }
        }

        sdb.registerProcessRequest(new TransferRequest(
            m_pipeName.calculate(ctx), m_ruleName.calculate(ctx),
            m_ignoreErrorCount, m_stmt,
            m_svFactory.create(),
            new Pipeline.StdProcessFactory(pars)
        ));
    }
}
