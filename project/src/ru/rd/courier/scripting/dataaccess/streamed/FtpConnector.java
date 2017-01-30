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

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.*;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 31.08.2006
 * Time: 12:05:51
 */
public class FtpConnector extends FtpProperties implements StreamConnector {
    private String m_fileName;

    public FtpConnector(Properties props) {
        super(props);
        m_fileName = StringHelper.stringParam(props, "file", null);
    }

    public void parseProperties(StringSimpleParser p) {
        Properties props = p.getProperties(null, '\'', "|");
        loadProps(props);
        String fileName = StringHelper.stringParam(props, "file", null);
        if (fileName != null) m_fileName = fileName;
        if (m_fileName == null) {
            throw new RuntimeException("File name not specified");
        }
    }

    private FTPClient connect() throws IOException {
        FTPClient ftp = new FTPClient();
        if (m_defaultTimeout > 0) ftp.setDefaultTimeout(m_defaultTimeout);
        if (m_dataTimeout > 0) ftp.setDataTimeout(m_dataTimeout);
        if (m_port < 0) {
            ftp.connect(m_host);
        } else {
            ftp.connect(m_host, m_port);
        }
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            throw new RuntimeException("FTP server refused connection.");
        }

        if (!ftp.login(m_username, m_password)) {
            throw new RuntimeException("Ftp login failed");
        }

        //m_logger.debug("Logged in to ftp remote system is " + m_ftp.getSystemName());

        if (m_isAsciiType) ftp.setFileType(FTP.ASCII_FILE_TYPE);
        else ftp.setFileType(FTP.BINARY_FILE_TYPE);
        
        ftp.enterLocalActiveMode();
        //ftp.enterLocalPassiveMode();
        if (!ftp.isConnected()) {
            throw new RuntimeException("Connect failed");
        }
        return ftp;
    }

    public InputStream createStream() throws IOException {
        FTPClient ftp = connect();

        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        if (!ftp.retrieveFile(m_fileName, os)) {
            throw new RuntimeException(
                "Error " + ftp.getReplyCode() + " " + ftp.getReplyString() +
                 " reading file " + m_fileName
            );
        }
        InputStream is = new ByteArrayInputStream(os.toByteArray());


        //InputStream is = ftp.retrieveFileStream(m_fileName);
        ftp.disconnect();
        return is;
    }

    public void cancel() {}
}
