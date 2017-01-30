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
package ru.rd.courier.scripting.dataaccess.streamed;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.OsCommand;
import ru.rd.courier.scripting.dataaccess.OsProcess;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.pool.ObjectPoolIntf;

import java.io.File;

/**
 * User: AStepochkin
 * Date: 10.08.2006
 * Time: 16:07:39
 */
public class ExecStreamConnector extends OsCommandConnector {
    private String m_command;
    private String[] m_env;
    private File m_workDir;

    public ExecStreamConnector(
        CourierLogger logger, ObjectPoolIntf threadPool,
        String command, String[] env, File workDir
    ) {
        super(logger, threadPool);
        m_command = command;
        m_env = env;
        m_workDir = workDir;
    }

    public void parseProperties(StringSimpleParser p) {
        p.skipBlanks();
        if (p.beyondEnd()) {
            if (m_command == null) {
                throw new RuntimeException("Command for process is undefined");
            }
        } else {
            m_command = p.shiftWordOrBracketedString('\'');
        }
    }

    protected OsCommand createCommand() {
        return new OsProcess(m_command, m_env, m_workDir);
    }
}
