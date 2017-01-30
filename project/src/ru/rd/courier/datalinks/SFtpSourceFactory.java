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

import org.w3c.dom.Node;
import ru.rd.courier.CourierException;
import ru.rd.courier.CourierContext;
import ru.rd.courier.scripting.dataaccess.SFtpSource;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.SFtpReceiver;
import ru.rd.courier.scripting.dataaccess.streamed.StreamParser;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;

/**
 * User: AStepochkin
 * Date: 11.04.2008
 * Time: 13:35:29
 */
public class SFtpSourceFactory extends FileBasedReceiverFactory {
    private String m_host;
    private int m_port;
    private String m_username;
    private String m_password;
    private StreamParser m_parser;

    public SFtpSourceFactory(
        CourierLogger logger,
        String encoding, boolean overwrite, boolean overwriteWarning,
        String prefix, String postfix, String dir, boolean fileNameAtFirstLine, String dateFormat,
        String host, int port, String username, String password, StreamParser parser
    ) {
        super(
            logger, encoding, overwrite, overwriteWarning,
            prefix, postfix, dir, fileNameAtFirstLine, dateFormat
        );
        init(host, port, username, password, parser);
    }

    public SFtpSourceFactory(CourierLogger logger, String name, CourierContext ctx, Node conf) throws CourierException {
        super(logger, conf);

        final StreamParser parser;
        final Node parserConf = DomHelper.getChild(conf, "parser", false);
        if (parserConf == null) {
            parser = null;
        } else {
            parser = StreamSourceFactory.getParserFactory(logger, parserConf).createParser();
        }

        String host = DomHelper.getNodeAttr(conf, "host");
        Account account = AccountUtils.confAccount(host, "SFTP", name, ctx.getAccountProvider(), conf);

        init(
            host,
            DomHelper.getIntNodeAttr(conf, "port", -1),
            account.name, account.password, parser
        );
    }

    private void init(String host, int port, String username, String password, StreamParser parser) {
        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;
        m_parser = parser;
    }

    public Object getObject(ObjectPoolIntf pool) throws Exception {
        return new SFtpSource(
            m_logger, m_encoding, m_overwrite, m_overwriteWarning,
            m_fileNameAtFirstLine, m_dir, m_prefix, m_postfix, m_dateFormat,
            m_host, m_port, m_username, m_password, m_parser
        );
    }

    public boolean checkObject(Object o) {
        return ((SFtpReceiver)o).isValid();
    }
}
