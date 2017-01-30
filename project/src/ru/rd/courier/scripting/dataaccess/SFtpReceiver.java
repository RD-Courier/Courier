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

import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.LinkWarning;

import java.io.IOException;
import java.util.List;
import java.util.Date;

import com.trilead.ssh2.*;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 20:09:25
 */
public class SFtpReceiver extends FileBasedAbstractReceiver {
    private final String m_host;
    private final int m_port;
    private final String m_username;
    private final String m_password;
    private Connection m_con = null;
    protected SFTPv3Client m_ftp = null;
    private boolean m_cancelled = false;

    public SFtpReceiver(
        CourierLogger logger, String encoding, boolean overwrite, boolean overwriteWarning,
        String dir, String prefix, String postfix, boolean fileNameAtFirstLine, String dateFormat,
        String host, int port, String username, String password
    ) throws IOException {
        super(
            logger, encoding, overwrite, overwriteWarning,
            prefix, postfix, dir, fileNameAtFirstLine, dateFormat
        );

        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;

        m_con = m_port >= 0 ? new Connection(m_host, m_port) : new Connection(m_host);
        m_con.addConnectionMonitor(new ConnectionMonitor() {
            public void connectionLost(Throwable throwable) {
                cleanup();
            }
        });
        m_con.connect();
        if (!m_con.authenticateWithPassword(m_username, m_password)) {
            throw new RuntimeException("Authentication failed.");
        }
        m_ftp = new SFTPv3Client(m_con);
    }


    private void cleanup() {
        Connection con;
        SFTPv3Client ftp;
        synchronized(this) {
            con = m_con;
            m_con = null;
            ftp = m_ftp;
            m_ftp = null;
        }
        try { if (ftp != null) ftp.close(); }
        catch (Exception e) { m_logger.warning(e); }
        try { if (con != null) con.close(); }
        catch (Exception e) { m_logger.warning(e); }
    }

    protected String getParamFileName() {
        return (
            m_prefix + "-" +
            m_dateFormat.format(new Date()) +
            "-" + ((int)(Math.random() * 100000000)) + m_postfix
        );
    }

    protected void beforeRequest() {
        synchronized(this) {
            if (m_con == null) throw new CourierException("Closed");
            m_cancelled = false;
        }
    }

    protected void storeData(String fileName, String inputData) throws CourierException, IOException {
        beforeRequest();
        try {
            SFTPv3FileHandle h = m_ftp.createFileTruncate(fileName);
            try {
                long fileOffset = 0;
                int pos = 0;
                while (pos < inputData.length()) {
                    synchronized(this) {
                        if (m_cancelled) throw new CourierException("Operation cancelled");
                    }
                    int newPos = Math.min(inputData.length(), pos + 4*1024);
                    String chunk = inputData.substring(pos, newPos);
                    byte[] buf = chunk.getBytes("cp1251");
                    m_ftp.write(h, fileOffset, buf, 0, buf.length);
                    fileOffset += buf.length;
                    pos = newPos;
                }
            } finally {
                try {m_ftp.closeFile(h); }
                catch(Exception e) { m_logger.warning(e); }
            }
        } catch (IOException e) {
            cleanup();
            throw e;
        }
    }

    protected boolean fileExists(String fileName) throws IOException, CourierException {
        try {
            m_ftp.stat(fileName);
            return true;
        } catch(SFTPException e) {
            if (e.getServerErrorCodeSymbol().equals("SSH_FX_NO_SUCH_FILE")) return false;
            throw e;
        }
    }

    protected List<LinkWarning> timedFlush() {
        return null;
    }

    protected void timedClose() throws CourierException {
        cleanup();
    }

    public void setTimeout(int timeout) throws CourierException {}
    public void cancel() throws CourierException {
        synchronized(this) {
            m_cancelled = true;
        }
    }

    public boolean isValid() {
        synchronized(this) {
            return (m_con != null);
        }
    }

    public String toString() {
        return
            "host = '" + m_host + "'" +
            " port = " + m_port +
            " username = '" + m_username + "'" +
            " dir = '" + m_dir + "'" +
            " encoding = '" + m_encoding + "'";
    }
}
