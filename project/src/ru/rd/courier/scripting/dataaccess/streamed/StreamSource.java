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

import org.xml.sax.SAXException;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.utils.*;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.WorkThread;
import ru.rd.utils.ExceptionCatchRunnable;
import ru.rd.utils.State;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 09.08.2006
 * Time: 13:15:08
 */
public class StreamSource extends TimedStringReceiver implements DataSource {
    private final CourierLogger m_logger;
    private final ObjectPoolIntf m_threadPool;
    private final StreamConnector m_streamConnector;
    private final StreamParser m_parser;
    private final boolean m_cacheData;
    private State m_state;
    private int m_timeout = 0;
    private StreamConnector m_cursf;
    private StreamParser m_curp;

    private static final State c_idleState = new State("IDLE");
    private static final State c_connectState = new State("CONNECT");
    private static final State c_parseState = new State("PARSE");
    private static final State c_cancelState = new State("CANCEL");

    private synchronized void setState(State state) {
        m_state = state;
    }

    private void ensureState(State state) {
        if (m_state != state) {
            throw new RuntimeException(
                "Invalid state: expected '" + state + "' actual '" + m_state + "'"
            );
        }
    }

    public StreamSource(
        CourierLogger logger, ObjectPoolIntf threadPool,
        StreamConnector streamConnector, StreamParser parser,
        boolean cacheData
    ) {
        m_logger = logger;
        m_threadPool = threadPool;
        m_state = c_idleState;
        m_streamConnector = streamConnector;
        m_parser = parser;
        m_cacheData = cacheData;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        return null;
    }

    protected List<LinkWarning> timedFlush() {
        return null;
    }

    protected void timedClose() throws CourierException {
        if (m_streamConnector != null) {
            m_streamConnector.cancel();
        }
        if (m_parser != null) {
            m_parser.cancel();
        }
    }

    public synchronized void setTimeout(int timeout) throws CourierException {
        ensureState(c_idleState);
        m_timeout = timeout;
    }

    public synchronized void cancel() throws CourierException {
        if (m_state == c_connectState) {
            m_state = c_cancelState;
            m_cursf.cancel();
        } else if (m_state == c_parseState) {
            m_state = c_cancelState;
            m_curp.cancel();
        }
    }

    public ResultSet request(final String query) throws CourierException {
        class ResultHolder {
            public ResultSet result = null;
        }

        try {
            WorkThread thread = (WorkThread)m_threadPool.getObject();
            try {
                final ResultHolder sh = new ResultHolder();
                ExceptionCatchRunnable task = new ExceptionCatchRunnable() {
                    protected void safeRun() throws Exception {
                        sh.result = innerRequest(query);
                    }
                };
                if (!thread.launchWorkAndWait(task, m_timeout*1000)) {
                    throw new RuntimeException("Timeout " + m_timeout + " sec. expired");
                }
                if (task.getException() != null) throw task.getException();
                return sh.result;
            } finally {
                m_threadPool.releaseObject(thread);
            }
        } catch (Exception e) {
            throw new CourierException(e);
        } finally {
            synchronized(this) {
                setState(c_idleState);
                m_cursf = null;
                m_curp = null;
            }
        }
    }

    public ResultSet innerRequest(String query) throws Exception {
        final StringSimpleParser p = new StringSimpleParser(query);

        synchronized(this) {
            ensureState(c_idleState);
            setState(c_connectState);
        }
        m_cursf = createConnector(p);

        InputStream is = m_cursf.createStream();
        if (m_cacheData) {
            ByteArrayOutputStream w = new ByteArrayOutputStream();
            StreamHelper.transfer(
                is, w, 4*1024,
                new Condition() {
                    public boolean isTrue() { return isCancelled(); }
                },
                true
            );

            //m_logger.debug("StreamSource cache:\n" + w.toString());

            is = new ByteArrayInputStream(w.toByteArray());
        }

        synchronized(this) {
            if (m_state == c_cancelState) {
                throw new RuntimeException("Request cancelled");
            }
            //ensureState(c_connectState);
            setState(c_parseState);
        }
        m_curp = createParser(p);
        return m_curp.parse(is);
    }

    private synchronized boolean isCancelled() {
        return (m_state == c_cancelState);
    }

    private StreamConnector createConnector(StringSimpleParser p) {
        if (m_streamConnector != null) {
            m_streamConnector.parseProperties(p);
            return m_streamConnector;
        }

        String type = p.getProperty("stream-type", true, '\'', ':');
        type = type.trim().toLowerCase();

        if (type.equals("url")) {
            return new UrlStreamConnector(
                new UrlStreamProperties(parseProperties(p))
            );
        } else if (type.equals("exec")) {
            p.skipBlanks();
            return new ExecStreamConnector(
                m_logger, m_threadPool, p.shiftWordOrBracketedString('\''), null, null
            );
        } else if (type.equals("ssh")) {
            Properties props = p.getProperties(null, '\'', "|");
            return new SshStreamConnector(
                m_logger, m_threadPool,
                StringHelper.stringParam(props, "host", null),
                StringHelper.intParam(props, "port", -1),
                StringHelper.stringParam(props, "username", null),
                StringHelper.stringParam(props, "password", null),
                StringHelper.stringParam(props, "command", null)
            );
        } else if (type.equals("const")) {
            Properties props = p.getProperties(null, '\'', "|");
            String data = StringHelper.stringParam(props, "data", "");
            return new ConstStreamConnector(data);
        } else {
            throw new RuntimeException("Invalid source type '" + type + "'");
        }
    }

    private static Properties parseProperties(StringSimpleParser p) {
        return p.getProperties(null, '\'', "|");
    }

    private StreamParser createParser(StringSimpleParser p) throws IOException, ParserConfigurationException, SAXException {
        if (m_parser != null) {
            m_parser.parseProperties(p);
            return m_parser;
        }

        String type = p.getProperty("parser-type", true, '\'', ':');
        type = type.trim().toLowerCase();
        if (type.equals("xml")) {
            String xml =
                "<?xml version=\"1.0\" encoding=\"windows-1251\" ?>" +
                "<conf>" + p.endSubstr() + "</conf>";
            return new XPathParserFactory(DomHelper.parseString(xml)).createParser();
        } else if (type.equals("json")) {
            String xml =
                "<?xml version=\"1.0\" encoding=\"windows-1251\" ?>" +
                "<conf>" + p.endSubstr() + "</conf>";
            return new JSONPathParserFactory(DomHelper.parseString(xml)).createParser();
        } else if (type.equals("null")) {
            Properties props = p.getProperties(null, '\'', "|");
            return new NullParser(
                StringHelper.stringParam(props, "encoding", "cp1251"),
                new IterColumnInfo[] {
                    new IterColumnInfo(
                        StringHelper.stringParam(props, "field-name")
                    )
                }
            );
        } else if (type.equals("file")) {
            Properties props = p.getProperties(null, '\'', "|");
            return new MoveToFileParser(
                m_logger, 
                StringHelper.stringParam(props, "field-name"),
                StringHelper.stringParam(props, "file")
            );
        } else {
            throw new RuntimeException("Invalid parser type '" + type + "'");
        }
    }
}
