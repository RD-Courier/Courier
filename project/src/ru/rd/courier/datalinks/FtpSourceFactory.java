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
package ru.rd.courier.datalinks;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.commons.net.ftp.FTPFileListParser;
import org.apache.commons.net.ftp.parser.NTFTPEntryParser;
import org.apache.commons.net.ftp.parser.EnterpriseUnixFTPEntryParser;
import org.apache.commons.net.ftp.parser.UnixFTPEntryParser;
import org.apache.commons.net.ftp.parser.VMSFTPEntryParser;
import ru.rd.courier.CourierException;
import ru.rd.courier.CourierContext;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.FtpReceiver;
import ru.rd.courier.scripting.dataaccess.FtpSource;
import ru.rd.courier.scripting.dataaccess.streamed.StreamParserFactory;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;

/**
 * User: AStepochkin
 * Date: 29.06.2005
 * Time: 18:48:53
 */
public class FtpSourceFactory extends FileBasedReceiverFactory implements HostProvider {
    private int m_defaultTimeout;
    private int m_dataTimeout;
    private String m_host;
    private int m_port;
    private String m_username;
    private String m_password;
    private StreamParserFactory m_parserFactory;
    private boolean m_asciiMode;
    private boolean m_passiveMode;
    private ListParserFactory m_listParserFactory;

    public String getHost() {
        return m_host;
    }

    private interface ListParserFactory {
        FTPFileListParser createListParser();
    }

    private static class NtListParserFactory implements ListParserFactory {
        public FTPFileListParser createListParser() {
            return new NTFTPEntryParser();
        }
    }

    private static class UnixListParserFactory implements ListParserFactory {
        public FTPFileListParser createListParser() {
            return new UnixFTPEntryParser();
        }
    }

    private static class EUnixListParserFactory implements ListParserFactory {
        public FTPFileListParser createListParser() {
            return new EnterpriseUnixFTPEntryParser();
        }
    }

    private static class VMSListParserFactory implements ListParserFactory {
        public FTPFileListParser createListParser() {
            return new VMSFTPEntryParser();
        }
    }

    public FtpSourceFactory(
        CourierLogger logger,
        int defaultTimeout, int dataTimeout,
        String host, int port, String username, String password, String encoding,
        boolean overwrite, boolean overwriteWarning,
        String prefix, String postfix, String dateFormat,
        String dir, boolean fileNameAtFirstLine,
        boolean asciiMode, boolean passiveMode,
        StreamParserFactory parserFactory
    ) {
        super(
            logger,
            encoding, overwrite, overwriteWarning, prefix,
            postfix, dir, fileNameAtFirstLine, dateFormat
        );
        init(
            defaultTimeout, dataTimeout, host, port, username, password,
            asciiMode, passiveMode, parserFactory
        );
    }

    public String getDesc() {
        return super.getDesc() + " | host = " + m_host;
    }

    private void init(
        int defaultTimeout, int dataTimeout,
        String host, int port, String username, String password,
        boolean asciiMode, boolean passiveMode,
        StreamParserFactory parserFactory
    ) {
        m_defaultTimeout = defaultTimeout;
        m_dataTimeout = dataTimeout;
        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;
        m_asciiMode = asciiMode;
        m_passiveMode = passiveMode;
        m_parserFactory = parserFactory;
        m_listParserFactory = null;
    }

    public FtpSourceFactory(CourierLogger logger, String name, CourierContext ctx, Node conf) throws CourierException {
        super(logger, conf);

        final StreamParserFactory parserFactory;
        final Node parserConf = DomHelper.getChild(conf, "parser", false);
        if (parserConf == null) {
            parserFactory = null;
        } else {
            parserFactory = StreamSourceFactory.getParserFactory(logger, parserConf);
        }

        String host = DomHelper.getNodeAttr(conf, "host");
        Account account = AccountUtils.confAccount(host, "FTP", name, ctx.getAccountProvider(), conf);

        init(
            getTimeoutFromNode(conf, "default-timeout"),
            getTimeoutFromNode(conf, "data-timeout"),
            host,
            DomHelper.getIntNodeAttr(conf, "port", -1),
            account.name,
            account.password,
            DomHelper.getBoolYesNo(conf, "ascii", true),
            DomHelper.getBoolYesNo(conf, "passive", false),
            parserFactory
        );

        String listParser = DomHelper.getNodeAttr(conf, "list-parser", null);

        if (listParser != null) {
            if (listParser.equalsIgnoreCase("NT")) {
                m_listParserFactory = new NtListParserFactory();
            } else if (listParser.equalsIgnoreCase("UNIX")) {
                m_listParserFactory = new UnixListParserFactory();
            } else if (listParser.equalsIgnoreCase("EUNIX")) {
                m_listParserFactory = new EUnixListParserFactory();
            } else if (listParser.equalsIgnoreCase("VMS")) {
                m_listParserFactory = new VMSListParserFactory();
            }
        }
    }

    private static int getTimeoutFromNode(Node n, String attrName) throws CourierException {
        try {
            Element e = (Element)n;
            int ret = -1;
            if (e.hasAttribute(attrName)) {
                ret = Integer.parseInt(e.getAttribute(attrName)) * 1000;
            }
            return ret;
        } catch(Exception e) {
            throw new CourierException(
                "Error getting timeout from attribute " + attrName, e
            );
        }
    }

    public Object getObject(ObjectPoolIntf pool) {
        try {
            FtpSource src = new FtpSource(
                m_logger,
                m_defaultTimeout, m_dataTimeout,
                m_host, m_port, m_username, m_password, m_dir, m_encoding,
                m_overwrite, m_overwriteWarning,
                m_fileNameAtFirstLine, m_prefix, m_postfix, m_dateFormat,
                m_asciiMode, m_passiveMode,
                m_parserFactory == null ? null : m_parserFactory.createParser()
            );
            if (m_listParserFactory != null) {
                src.setListParser(m_listParserFactory.createListParser());
            }
            return src;
        } catch (CourierException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkObject(Object o) {
        FtpReceiver fr = (FtpReceiver)o;
        try {
            return fr.check();
        } catch (CourierException e) {
            m_logger.warning(e);
            return false;
        }
    }
}
