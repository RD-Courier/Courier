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

import ru.rd.courier.scripting.dataaccess.OsCommand;
import ru.rd.courier.logging.CourierLogger;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.ChannelCondition;

import java.io.InputStream;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 14:24:13
 */
public class SshProcess implements OsCommand {
    private final CourierLogger m_logger;
    private final String m_host;
    private final int m_port;
    private final String m_username;
    private final String m_password;
    private final String m_cmd;
    private Connection m_conn = null;
    private Session m_sess = null;

    public SshProcess(CourierLogger logger, String host, int port, String username, String password, String cmd) {
        m_logger = logger;
        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;
        m_cmd = cmd;
    }

    private synchronized Session getSession() {
        return m_sess;
    }

    public void start() {
        Connection conn = m_port >= 0 ? new Connection(m_host, m_port) : new Connection(m_host);
        synchronized(this) {
            m_conn = conn;
        }
        try {
            m_conn.connect();
            if (!m_conn.authenticateWithPassword(m_username, m_password)) {
                throw new RuntimeException("Authentication failed.");
            }
            m_sess = m_conn.openSession();
            m_sess.execCommand(m_cmd);
        } catch (Exception e) {
            close();
        }
    }

    public InputStream getInputStream() {
        return m_sess.getStdout();
    }

    public InputStream getErrorStream() {
        return m_sess.getStderr();
    }

    public int waitFor() throws InterruptedException {
        if (getSession() == null) return 0;
        int status = 0;
        try {
            int ret = m_sess.waitForCondition(ChannelCondition.EXIT_STATUS & ChannelCondition.EOF, 0);
            if ((ret & ChannelCondition.EXIT_STATUS) == 0) {
                m_logger.warning("SSH command has not returned exit status");
            } else {
                status = m_sess.getExitStatus();
            }
            return status;
        } finally {
            close();
        }
    }

    public void close() {
        Session sess;
        Connection conn;
        synchronized(this) {
            sess = m_sess;
            m_sess = null;
            conn = m_conn;
            m_conn = null;
        }
        try { if (sess != null) sess.close(); }
        catch (Exception e) { m_logger.warning(e); }
        try { if (conn != null) conn.close(); }
        catch (Exception e) { m_logger.warning(e); }
    }

    public void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
