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
import ru.rd.courier.scripting.dataaccess.SshProcess;

import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 09.04.2008
 * Time: 20:30:13
 */
public class ExecSshProcess extends ExecProcess {
    private final ScriptExpression m_cmd;
    private final ScriptExpression m_host;
    private final ScriptExpression m_port;
    private final ScriptExpression m_username;
    private final ScriptExpression m_password;

    public ExecSshProcess(
        String pipeObjectName,
        ScriptExpression host, ScriptExpression port, ScriptExpression username, ScriptExpression password, ScriptExpression cmd,
        String logOutput, String showOutput, String ignoreErrorStream
    ) {
        super(pipeObjectName, logOutput, showOutput, ignoreErrorStream);

        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;
        m_cmd = cmd;
    }

    protected OsCommand createProcess(Context ctx) throws IOException {
        return new SshProcess(
            ctx,
            m_host.calculate(ctx), Integer.parseInt(m_port.calculate(ctx)), 
            m_username.calculate(ctx), m_password.calculate(ctx),
            m_cmd.calculate(ctx)
        );
    }
}
