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

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

public class FtpReceiver extends FileBasedAbstractReceiver {
    private final String m_host;
    private final int m_port;
    private final String m_username;
    private final String m_password;
    private final String m_localHostName;
    private FTPClient m_ftp;

    public FtpReceiver(
        CourierLogger logger,
        int defaultTimeout, int dataTimeout,
        String host, int port, String username, String password, String dir, String encoding,
        boolean overwrite, boolean overwriteWarning,
        boolean fileNameAtFirstLine, String prefix, String postfix, String dateFormat,
        boolean asciiMode, boolean passiveMode
    ) throws CourierException {
        super(
            logger, encoding, overwrite, overwriteWarning, prefix,
            postfix, dir, fileNameAtFirstLine, dateFormat
        );
        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;

        try {
            m_localHostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new CourierException(toString() + ": error getting local host name", e);
        }

        m_ftp = new FTPClient();
        try {
            if (defaultTimeout > 0) m_ftp.setDefaultTimeout(defaultTimeout);
            if (dataTimeout > 0) m_ftp.setDataTimeout(dataTimeout);
            if (m_port > 0) {
                m_ftp.connect(m_host, m_port);
            } else {
                m_ftp.connect(m_host);                
            }
            int reply = m_ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new CourierException("FTP server refused connection.");
            }

            if (!m_ftp.login(m_username, m_password)) {
                throw new CourierException("Ftp login failed");
            }

            m_logger.debug("Logged in to ftp remote system is " + m_ftp.getSystemName());

            if (asciiMode) {
                m_ftp.setFileType(FTP.ASCII_FILE_TYPE);
            } else {
                m_ftp.setFileType(FTP.BINARY_FILE_TYPE);
            }
            if (passiveMode) {
                m_ftp.enterLocalPassiveMode();
            } else {
                m_ftp.enterLocalActiveMode();
            }
            if (needCheckFileExistance()) {
                if (!ftpSupportsList()) {
                    m_logger.error(
                        toString() + ": invalid overwrite settings" +
                        " as ftp server does not support list command." +
                        " Receiver will work in standard mode"
                    );
                    m_overwrite = true;
                    m_overwriteWarning = false;
                }
            }
        } catch (Exception e) {
            if ((m_ftp != null) && m_ftp.isConnected()) {
                try {
                    m_ftp.disconnect();
                } catch (Exception f) {
                    m_logger.warning(f);
                }
            }
            m_ftp = null;
            throw new CourierException(toString() + ": connection error", e);
        }
    }

    private boolean ftpSupportsList() throws IOException {
        return m_ftp.listNames(m_dir) != null;
    }

    protected boolean fileExists(String fileName) throws IOException, CourierException {
        String[] fileList = m_ftp.listNames(m_dir);

        if (fileList == null) {
            throw new CourierException("Ftp server does not support list");
        }

        for (String fn : fileList) {
            if (fn.equals(fileName)) return true;
        }

        return false;
    }

    protected String getParamFileName() {
        return (
            m_prefix + m_localHostName + "-" +
            m_dateFormat.format(new Date()) +
            "-" + ((int)(Math.random() * 100000000)) + m_postfix
        );
    }

    protected void storeData(String fileName, String inputData) throws CourierException, IOException {
        if (!m_ftp.storeFile(fileName, new ByteArrayInputStream(inputData.getBytes(m_encoding)))) {
            throw new CourierException(
                "Storing file through ftp (" + toString() +
                ") failed: " + m_ftp.getReplyString()
            );
        }
    }

    public List timedFlush() {
        return null;
    }

    public void setTimeout(int timeout) throws CourierException {
        m_ftp.setDataTimeout(timeout);
    }

    public void timedClose() {
        if (m_ftp.isConnected()) {
            try {
                m_ftp.disconnect();
            } catch (Exception f) {
                m_logger.warning(f);
            }
        }
    }

    public void cancel() throws CourierException {
        try {
            m_ftp.abort();
        } catch (IOException e) {
            throw new CourierException(toString(), e);
        }
    }

    public boolean check() throws CourierException {
        try {
            return m_ftp.sendNoOp();
        } catch (IOException e) {
            throw new CourierException(toString(), e);
        }
    }

    public String toString() {
        return
            "host = '" + m_host + "'" +
            " username = '" + m_username + "'" +
            " dir = '" + m_dir + "'" +
            " encoding = '" + m_encoding + "'";
    }

    protected FTPClient getFtp() {
        return m_ftp;
    }
}
