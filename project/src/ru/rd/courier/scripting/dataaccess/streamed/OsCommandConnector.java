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
import ru.rd.courier.utils.DelegateInputStream;
import ru.rd.courier.utils.LinesLimitedOutputStream;
import ru.rd.courier.utils.NullOutputStream;
import ru.rd.courier.utils.StreamTransferring;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.ThreadHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 16:35:19
 */
public abstract class OsCommandConnector implements StreamConnector {
    protected final CourierLogger m_logger;
    private final ObjectPoolIntf m_threadPool;
    private OsCommand m_proc = null;

    private static class OsCommandInputStream extends DelegateInputStream {
        private final CourierLogger m_logger;
        private final OsCommand m_proc;
        private final ByteArrayOutputStream m_stderr;

        public OsCommandInputStream(CourierLogger logger, ObjectPoolIntf threadPool, OsCommand proc) {
            super(proc.getInputStream());
            m_logger = logger;
            m_proc = proc;
            m_stderr = new ByteArrayOutputStream();
            ThreadHelper.exec(
                threadPool,
                new StreamTransferring(proc.getErrorStream(), new LinesLimitedOutputStream(m_stderr, 100))
            );
        }

        public void close() throws IOException {
            try {
                int es = m_proc.waitFor();
                if (es != 0) {
                    String stderr;
                    if (m_stderr.size() > 0) {
                        stderr = " errors: " + new String(m_stderr.toByteArray());
                    } else {
                        stderr = "";
                    }
                    throw new RuntimeException("Process exit status = " + es + stderr);
                }
            }
            catch (Exception e) { m_logger.warning(e); }
            super.close();
        }
    }

    public OsCommandConnector(CourierLogger logger, ObjectPoolIntf threadPool) {
        m_logger = logger;
        m_threadPool = threadPool;
    }

    protected abstract OsCommand createCommand();

    public final InputStream createStream() throws IOException {
        OsCommand proc = createCommand();
        synchronized(this) {
            m_proc = proc;
        }
        try {
            m_proc.start();
            synchronized(this) {
                if (m_proc == null) throw new RuntimeException("Process cancelled");
                proc = m_proc;
                m_proc = null;
                ThreadHelper.exec(m_threadPool, new StreamTransferring(proc.getErrorStream(), new NullOutputStream()));
                return new OsCommandInputStream(m_logger, m_threadPool, proc);
            }
        } finally {
            synchronized(this) {
                m_proc = null;
            }
        }
    }

    public final void cancel() {
        OsCommand proc;
        synchronized(this) {
            if (m_proc == null) return;
            proc = m_proc;
        }
        proc.close();
    }
}