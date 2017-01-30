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

import com.trilead.ssh2.SFTPv3DirectoryEntry;
import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3FileHandle;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.dataaccess.SFtpReceiver;
import ru.rd.courier.scripting.dataaccess.streamed.StreamParser;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.courier.utils.Property;
import ru.rd.courier.utils.StreamHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.CourierException;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * User: AStepochkin
 * Date: 11.04.2008
 * Time: 14:43:58
 */
public class SFtpSource extends SFtpReceiver implements DataSource {
    private final char m_bracket = '\'';
    private final StreamParser m_parser;

    public SFtpSource(
        CourierLogger logger,
        String encoding, boolean overwrite, boolean overwriteWarning,
        boolean fileNameAtFirstLine,
        String dir, String prefix, String postfix, String dateFormat,
        String host, int port,
        String username, String password,
        StreamParser parser
    ) throws CourierException, IOException {
        super(
            logger,
            encoding, overwrite, overwriteWarning, dir, prefix, postfix, fileNameAtFirstLine, dateFormat,
            host, port, username, password
        );
        m_parser = parser;
    }

    public ResultSet request(String query) throws CourierException {
        beforeRequest();
        try {
            return innerRequest(query);
        } catch (Throwable e) {
            throw new CourierException(e);
        }
    }

    private ResultSet innerRequest(String query) throws IOException, SQLException, CourierException {
        StringSimpleParser p = new StringSimpleParser(query);
        final String command = p.shiftWord().toLowerCase();
        p.skipBlanks();

        DataBufferResultSet rs = new DataBufferResultSet();

        if (command.equals("list")) {
            String path = p.shiftWordOrBracketedString(m_bracket);
            p.skipBlanks();
            final Pattern re;
            if (p.beyondEnd()) {
                re = null;
            } else {
                final String nameRegExp = p.shiftWordOrBracketedString(m_bracket);
                re = Pattern.compile(nameRegExp);
            }
            rs.addColumn(new StringColumnInfo("File", 1024));
            rs.addColumn(new StringColumnInfo("FileName", 1024));

            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            Vector ls = m_ftp.ls(path);
            for (Object fo: ls) {
                SFTPv3DirectoryEntry fe = (SFTPv3DirectoryEntry)fo;
                String name = fe.filename;
                if (!name.equals(".") && !name.equals("..") && (re == null || re.matcher(name).matches())) {
                    rs.addRecord();
                    String fullname = path + '/' + name;
                    rs.updateString(1, fullname);
                    rs.updateString(2, name);
                }
            }
        } else if (command.equals("get")) {
            getCommand(p, rs);
        } else if (command.equals("put")) {
            putCommand(p, rs);
        } else if (command.equals("remove")) {
            final String name = p.shiftWordOrBracketedString(m_bracket);
            m_ftp.rm(name);
        } else if (command.equals("get-parse")) {
            final String name = p.shiftWordOrBracketedString(m_bracket);
            return m_parser.parse(new BufferedInputStream(new SFileInputStream(m_ftp, name)));
        } else {
            throw new RuntimeException("Invalid command '" + command + "'");
        }
        rs.beforeFirst();
        return rs;
    }

    private void putCommand(StringSimpleParser p, DataBufferResultSet rs)
    throws IOException {
        Properties props = p.getProperties(null, '\'', null);
        String fromfile = StringHelper.stringParam(props, "from-file");
        String tofile = StringHelper.stringParam(props, "to-file");
        StreamHelper.transfer(new FileInputStream(fromfile), new SFileOutputStream(m_ftp, tofile), 4*1024);
    }

    private void getCommand(StringSimpleParser p, DataBufferResultSet rs)
    throws SQLException, IOException {
        final String name = p.shiftWordOrBracketedString(m_bracket);
        Property storeParam = p.getProperty(m_bracket);
        final String storeType = storeParam.name.toLowerCase();
        final String storeSpec = storeParam.value;
        OutputStream os = null;
        final String field;
        final String value;
        try {
            if (storeType.equals("file")) {
                File storeFile = new File(storeSpec);
                File parentFile = storeFile.getParentFile();
                if (parentFile != null && !parentFile.exists()) {
                    if(!parentFile.mkdirs()) {
                        throw new CourierException("Failed creating dir: " + parentFile.getPath());
                    }
                }
                os = new FileOutputStream(storeSpec);
                StreamHelper.transfer(new SFileInputStream(m_ftp, name), os, 4*1024);
                field = "file";
                value = storeSpec;
            } else if (storeType.equals("var-name")) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                os = bos;
                StreamHelper.transfer(new SFileInputStream(m_ftp, name), os, 4*1024);
                String encoding = p.thisParamThenMove("encoding", true, m_bracket);
                if (encoding == null) encoding = "cp1251";
                value = bos.toString(encoding);
                field = storeSpec;
            } else {
                throw new RuntimeException("Invalid store type '" + storeType + "'");
            }
            rs.addColumn(new StringColumnInfo(field, value.length()));
            rs.addRecord();
            rs.updateString(1, value);
        } finally {
            try { if (os != null) os.close(); }
            catch(Throwable e) { m_logger.warning(e); }
        }
    }

    private static class SFileInputStream extends InputStream {
        private final SFTPv3Client m_ftp;
        private final SFTPv3FileHandle m_handle;
        private long m_pos;

        public SFileInputStream(SFTPv3Client ftp, String file) throws IOException {
            m_ftp = ftp;
            m_handle = m_ftp.openFileRO(file);
            m_pos = 0;
        }

        public int read() throws IOException {
            byte[] buf = new byte[1];
            int rlen = read(buf, 0, 1);
            if (rlen == -1) return -1;
            return buf[0];
        }

        public int read(byte b[], int off, int len) throws IOException {
            int rlen = m_ftp.read(m_handle, m_pos, b, off, len);
            if (rlen != -1) m_pos += rlen;
            return rlen;
        }

        public void close() throws IOException {
            if (!m_handle.isClosed()) m_ftp.closeFile(m_handle);
        }
    }

    private static class SFileOutputStream extends OutputStream {
        private final SFTPv3Client m_ftp;
        private final SFTPv3FileHandle m_handle;
        private long m_pos;

        public SFileOutputStream(SFTPv3Client ftp, String file) throws IOException {
            m_ftp = ftp;
            m_handle = m_ftp.openFileRO(file);
            m_pos = 0;
        }

        public void write(int b) throws IOException {
            byte[] buf = new byte[1];
            write(buf, 0, 1);
        }

        public void write(byte b[], int off, int len) throws IOException {
            m_ftp.write(m_handle, m_pos, b, off, len);
            m_pos += len;
        }

        public void close() throws IOException {
            if (!m_handle.isClosed()) m_ftp.closeFile(m_handle);
        }
    }
}
