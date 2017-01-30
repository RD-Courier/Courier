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
package ru.rd.courier.scripting.dataaccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 14:22:47
 */
public class OsProcess implements OsCommand {
    private final String m_cmd;
    private final String[] m_env;
    private final File m_workDir;

    private Process m_proc;

    public OsProcess(String cmd, String[] env, File workDir) {
        m_cmd = cmd;
        m_env = env;
        m_workDir = workDir;
    }

    public InputStream getInputStream() {
        return m_proc.getInputStream();
    }

    public InputStream getErrorStream() {
        return m_proc.getErrorStream();
    }

    public void start() throws IOException {
        m_proc = Runtime.getRuntime().exec(m_cmd, m_env, m_workDir);
    }

    public void close() {
        synchronized(this) {
            if (m_proc == null) return;
            m_proc = null;
        }
    }

    public int waitFor() throws InterruptedException {
        return m_proc.waitFor();
    }
}
