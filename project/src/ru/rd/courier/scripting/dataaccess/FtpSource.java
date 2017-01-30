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

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileListParser;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.databuffer.BigIntColumnInfo;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.DateColumnInfo;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.dataaccess.streamed.StreamParser;
import ru.rd.courier.utils.Property;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.*;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * User: AStepochkin
 * Date: 08.08.2006
 * Time: 12:32:57
 */
public class FtpSource extends FtpReceiver implements DataSource {
    private final char m_bracket = '\'';
    private final StreamParser m_parser;
    private FTPFileListParser m_listParser;

    public FtpSource(
        CourierLogger logger,
        int defaultTimeout, int dataTimeout, String host, int port,
        String username, String password,
        String dir, String encoding, boolean overwrite, boolean overwriteWarning,
        boolean fileNameAtFirstLine,
        String prefix, String postfix, String dateFormat,
        boolean asciiMode, boolean passiveMode,
        StreamParser parser
    ) throws CourierException {
        super(
            logger,
            defaultTimeout, dataTimeout, host, port,
            username, password,
            dir, encoding, overwrite, overwriteWarning,
            fileNameAtFirstLine, prefix, postfix, dateFormat,
            asciiMode, passiveMode
        );
        m_parser = parser;
        m_listParser = null;
    }

    public void setListParser(FTPFileListParser listParser) {
        m_listParser = listParser;
    }

    public ResultSet request(String query) throws CourierException {
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
            listCommand(p, rs);
        } else if (command.equals("get")) {
            getCommand(p, rs);
        } else if (command.equals("remove")) {
            final String name = p.shiftWordOrBracketedString(m_bracket);
            if (!getFtp().deleteFile(name)) {
                raiseError("removing file '" + name + "'");
            }
        } else if (command.equals("get-parse")) {
            final String name = p.shiftWordOrBracketedString(m_bracket);
            InputStream is = getFtp().retrieveFileStream(name);
            if (is == null) raiseError("retrieving file '" + name + "'");
            return m_parser.parse(new BufferedInputStream(is));
        } else {
            throw new RuntimeException("Invalid command '" + command + "'");
        }
        rs.beforeFirst();
        return rs;
    }

    private void raiseError(String opDesc) {
        throw new CourierException("Failed " + opDesc + ": reply = " + getFtp().getReplyString());
    }

    private void listCommand(StringSimpleParser p, DataBufferResultSet rs) throws SQLException, IOException {
        final String path = p.shiftWordOrBracketedString(m_bracket);
        p.skipBlanks();
        final Pattern re;
        String fieldNamePrefix = "";
        if (p.beyondEnd()) {
            re = null;
        } else {
            final String nameRegExp = p.shiftWordOrBracketedString(m_bracket);
            re = Pattern.compile(nameRegExp);
            fieldNamePrefix = p.shiftWordOrBracketedString(m_bracket);
        }
        rs.addColumn(new StringColumnInfo(fieldNamePrefix + "file", 1024));
        rs.addColumn(new StringColumnInfo(fieldNamePrefix + "FileName", 1024));
        rs.addColumn(new BigIntColumnInfo(fieldNamePrefix + "FileSize", false));
        rs.addColumn(new DateColumnInfo(fieldNamePrefix + "FileTime", new SimpleDateFormat()));

        if(!getFtp().changeWorkingDirectory(path)) {
            if (getFtp().getReplyCode() == 550) return;
            raiseError("changing dir '" + path + "'");
        }
        FTPFile[] list;
        if (m_listParser == null) {
            list = getFtp().listFiles();
        } else {
            list = getFtp().listFiles(m_listParser);
        }
        if (list == null) {
            int rc = getFtp().getReplyCode();
            if (rc < 200 || (rc >= 300 && rc != 550)) raiseError("listing '" + path + "'");
        } else {
            for (final FTPFile file: list) {
                String name = file.getName();
                if (re == null || re.matcher(name).matches()) {
                    rs.addRecord();
                    rs.updateString(1, name);
                    int ps = StringHelper.rfindChars(name, "/\\");
                    String fn = ps < 0? name : name.substring(ps + 1);
                    rs.updateString(2, fn);
                    rs.updateLong(3, file.getSize());
                    rs.updateDate(4, new Date(file.getTimestamp().getTime().getTime()));
                }
            }
        }
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
                if(!getFtp().retrieveFile(name, os)) raiseError("retrieving file '" + name + "'");
                field = "file";
                value = storeSpec;
            } else if (storeType.equals("var-name")) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                os = bos;
                if (!getFtp().retrieveFile(name, os)) raiseError("retrieving file '" + name + "'");
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
}
