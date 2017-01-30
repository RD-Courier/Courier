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
package ru.rd.courier;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.StatementProvider;
import ru.rd.courier.scripting.statements.ResultSetLoopSkeleton;
import ru.rd.courier.scripting.statements.Nothing;
import ru.rd.courier.scripting.statements.ObjectStatementCaller;

import java.sql.ResultSet;

/**
 * User: AStepochkin
 * Date: 27.05.2005
 * Time: 16:50:48
 */
public class AsyncTransferScriptProcess extends TargetScriptProcess {
    public AsyncTransferScriptProcess(
        CourierLogger msgh, final CourierContext appContext, Integer dbId,
        TransferRequest request,
        Integer failCount,
        Pipeline pipe,
        String pipeValue,
        final ResultSet resultSet
    ) throws CourierException {
        super(
            msgh, "process-log-decorator-template-rt", Pipeline.s_PipeMessageTemplate, dbId, request, pipe, pipeValue,
            new StatementProvider() {
                ScriptStatement m_stmt = new ResultSetLoopSkeleton(
                    new ObjectStatementCaller(
                        appContext.getScriptParam("target-profile-object-name"),
                        appContext.getScriptParam("target-profile-object-data-stmt")
                    ),
                    null, appContext.getScriptParam("rs-counter-var-name"), null
                ) {
                    protected ResultSet getResultSet(Context ctx) {
                        return resultSet;
                    }

                    protected boolean needStandardCleanUp() {
                        return true;
                    }

                    protected void cleanUp(Context ctx) {}
                };

                public void start(Context ctx) throws CourierException {
                    m_stmt.start(ctx);
                }

                public ScriptStatement getStatement(String name) {
                    if ("main".equals(name)) return m_stmt;
                    return new Nothing();
                }

                public void finish(Context ctx) throws CourierException {
                    m_stmt.finish(ctx);
                }
            }, failCount, null
        );
    }
}
