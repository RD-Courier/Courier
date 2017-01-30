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
import ru.rd.courier.TargetPipeline;
import ru.rd.courier.logging.data.DataLoggerToOutputStreamAdapter;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.dataaccess.FileSystemSource;
import ru.rd.courier.scripting.dataaccess.OsCommand;

import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 11:06:18
 */
public abstract class ExecProcess implements ScriptStatement {
    private final String m_pipeObjectName;
    private final boolean m_showOutput;
    private final boolean m_logOutput;
    private final boolean m_noErrorStreamException;

    public ExecProcess(
        String pipeObjectName,
        String logOutput, String showOutput, String ignoreErrorStream
    ) {
        m_pipeObjectName = pipeObjectName;
        m_logOutput = logOutput.equalsIgnoreCase("yes");
        m_showOutput = showOutput.equalsIgnoreCase("yes");
        m_noErrorStreamException = ignoreErrorStream.equalsIgnoreCase("yes");
    }

    protected abstract OsCommand createProcess(Context ctx) throws IOException;

    public final void start(Context ctx) throws CourierException {}
    public final void finish(Context ctx) throws CourierException {}

    public final void exec(Context ctx) throws CourierException {
        try {
            TargetPipeline pipe = (TargetPipeline)ctx.getObject(m_pipeObjectName);
            OsCommand proc = createProcess(ctx);
            try {
                FileSystemSource.processCommandOutput(
                    proc, ctx, pipe.getCourier().getThreadPool(),
                    m_logOutput ? new DataLoggerToOutputStreamAdapter(pipe.getDataLogger()) : null,
                    m_showOutput, m_noErrorStreamException
                );
            } finally {
                try { proc.close(); } 
                catch (Exception e) { ctx.warning(e); }
            }
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }
}
