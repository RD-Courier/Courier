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

import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.dataaccess.OsCommand;
import ru.rd.courier.scripting.dataaccess.OsProcess;

import java.io.File;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 15.11.2007
 * Time: 18:44:23
 */
public class ExecOsProcess extends ExecProcess {
    private final ScriptExpression m_cmd;
    private final ScriptExpression m_workDir;

    public ExecOsProcess(
        String pipeObjectName, ScriptExpression cmd, ScriptExpression workDir,
        String logOutput, String showOutput, String ignoreErrorStream
    ) {
        super(pipeObjectName, logOutput, showOutput, ignoreErrorStream);
        m_cmd = cmd;
        m_workDir = workDir;
    }

    protected OsCommand createProcess(Context ctx) throws IOException {
        File wd = null;
        if (m_workDir != null) {
            String wds = m_workDir.calculate(ctx);
            if (wds != null) wd = new File(wds);
        }
        return new OsProcess(m_cmd.calculate(ctx), null, wd);
    }
}