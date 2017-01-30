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
package ru.rd.courier.jdbc.mock;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.rd.courier.jdbc.ConnectionDrivenJdbcStatement;
import ru.rd.courier.jdbc.CorrectUpdateResult;
import ru.rd.courier.jdbc.ErrorUpdateResult;
import ru.rd.courier.jdbc.JdbcDatabase;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.File;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MockDatabase implements JdbcDatabase {
    private static MockDatabase s_globalDatabase = null;

    private static final String c_updateCommand = "U";
    private static final String c_selectCommand = "S";
    private static final String c_testCommand = "T";
    private static final String c_errorCommand = "E";
    private static final String c_updateErrorKeyword = "Error";

    private Logger m_logger;
    private Map<String, MockTable> m_tables = new HashMap<String, MockTable>();
    private long m_responseDelay = 0;

    public MockDatabase(Logger logger) {
        if (logger == null) {
            m_logger = Logger.getLogger(getClass().getPackage().getName());
        } else {
            m_logger = logger;
        }
    }

    private static Logger findLogger(Element e) {
        if (e.hasAttribute("logger"))
            return Logger.getLogger(e.getAttribute("logger"));
        return null;
    }

    public MockDatabase(Element e, Logger logger) throws SQLException {
        this(logger == null ? findLogger(e) : logger);
        Element tt = null;
        Element[] tl = null;
        tt = DomHelper.getChild(e, "tables");
        tl = DomHelper.getChildrenByTagName(tt, "table", false);
        if (tl != null) {
            for (int i = 0; i < tl.length; i++) {
                MockTable mt = new MockTable(m_logger, tl[i]);
                m_tables.put(mt.getName(), mt);
            }
        }
    }

    public void close() throws SQLException {
        for (MockTable t: m_tables.values()) {
            t.close();
        }
    }

    private static Element getFileElement(File conf) throws SQLException {
        try {
            return DomHelper.parseXmlFile(conf).getDocumentElement();
        } catch (Exception e) {
            SQLException ne = new SQLException("See cause exception message");
            ne.initCause(e);
            throw ne;
        }
    }

    public MockDatabase(File conf, Logger logger) throws SQLException {
        this(getFileElement(conf), logger);
    }

    public final static String cConfFilePropName = "MockDbConf";

    public static MockDatabase getGlobalDatabase() throws SQLException {
        if (s_globalDatabase == null) {
            String confFileName = System.getProperty(cConfFilePropName, "");
            if (!confFileName.equals("")) {
                Document conf = null;
                try {
                    conf = DomHelper.parseXmlFile(new File(confFileName));
                } catch (Exception e) {
                    throw new SQLException(e.getMessage());
                }
                s_globalDatabase = new MockDatabase(
                    conf.getDocumentElement(), null
                );
            } else {
                s_globalDatabase = new MockDatabase(null);
            }
        }

        return s_globalDatabase;
    }

    public static void setGlobalDatabase(MockDatabase db) {
        s_globalDatabase = db;
    }

    public static void closeGlobalDatabase() throws SQLException {
        if (s_globalDatabase == null) return;
        MockDatabase db = s_globalDatabase;
        s_globalDatabase = null;
        db.close();
    }

    public Connection getConnection(Properties props) throws SQLException {
        return new MockConnection(this, props);
    }

    public MockTable getTable(String name) {
        return m_tables.get(name);
    }

    public void addTable(String name, Element data) throws SQLException {
        m_tables.put(name, new MockTable(m_logger, name, data));                  
    }

    public List request(
        ConnectionDrivenJdbcStatement stmt, String request
    ) throws SQLException {
        List res = new LinkedList();
        try {
            if (m_responseDelay > 0) Thread.sleep(m_responseDelay);
            StringSimpleParser p = new StringSimpleParser(request);
            while (!p.beyondEnd()) {
                final String command = p.shiftWord();
                p.skipBlanks();
                final String line;
                if (command.equals("T")) {
                    line = p.shiftWordOrBracketedString('\'');
                    p.skipBlanks();
                } else {
                    line = p.shiftLine();
                }
                if (!handleLine(stmt, command, line, res)) {
                    res.add(new CorrectUpdateResult(1));
                }
            }
        } catch (Exception e) {
            SQLException ne = new SQLException("See cause");
            ne.initCause(e);
            throw ne;
        }
        return res;
    }

    private boolean handleLine(
        ConnectionDrivenJdbcStatement stmt, String command, String line, List res
    ) throws InterruptedException, SQLException {
        String data = line;
        int p = 0;
        p = StringHelper.skipDelims(data, 0);
        Properties props = new Properties();
        if (
               (p < data.length() - 1)
            && (data.charAt(p) == '(')
            && (data.charAt(p+1) != '(')
        ) {
            p = StringHelper.parseParams(props, data, '"', p + 1, ')');
            data = data.substring(p);
        }
        if (props.containsKey("Sleep")) {
            Thread.sleep(Long.parseLong(props.getProperty("Sleep")));
        } else if (props.containsKey("RandomSleep")) {
            Thread.sleep(
                (long)(Long.parseLong(props.getProperty("Sleep")) * Math.random())
            );
        }
        if (props.containsKey("WarningCount")) {
            int count = Integer.parseInt(props.getProperty("WarningCount"));
            for (int i = 0; i < count; i++) {
                stmt.addWarning(new SQLWarning("Mock warning", "MOCK"));
            }
        }
        boolean ret = true;
        if(command.equalsIgnoreCase(c_updateCommand)) {
            if (props.containsKey(c_updateErrorKeyword)) {
                res.add(new ErrorUpdateResult(data));
            } else {
                res.add(new CorrectUpdateResult(1));
            }
        } else if(command.equalsIgnoreCase(c_selectCommand)) {
            res.add(executeQuery(stmt, data));
        } else if(command.equalsIgnoreCase(c_testCommand)) {
            //res.add();
        } else if(command.equalsIgnoreCase(c_errorCommand)) {
            throw new SQLException(line);
        } else {
            ret = false;
        }
        return ret;
    }

    private ResultSet executeQuery(Statement stmt, String request) throws SQLException {
        try {
            Properties props = new Properties();
            StringHelper.parseParams(props, request, '"', 0);
            if (props.containsKey("Sleep")) {
                long sleepInterval = Integer.parseInt(props.getProperty("Sleep"));
                long sleepFinish = System.currentTimeMillis() + sleepInterval;
                while (true) {
                    long sleepRemainder = sleepFinish - System.currentTimeMillis();
                    if (sleepRemainder < 0) break;
                    try {
                        Thread.sleep(sleepRemainder);
                    } catch(InterruptedException e) {
                        if (!props.containsKey("StubbornSleep")) {
                            throw e;
                        }
                    }
                }
            }
            ResultSet ret;
            if (props.containsKey("TableName")) {
                DataBufferResultSet dbRs;
                MockTable mt = m_tables.get(props.getProperty("TableName"));
                if (mt == null) {
                    throw new SQLException(
                        "Table '" + props.getProperty("TableName") + "' not found");
                }
                if (props.containsKey("ColName")) {
                    if (props.containsKey("Greater")) {
                        dbRs = mt.selectWhereFieldGreater(
                            props.getProperty("ColName"),
                            Integer.parseInt(props.getProperty("Greater"))
                        );
                    } else if (props.containsKey("DateGreater")) {
                        dbRs = mt.selectWhereFieldGreater(
                            props.getProperty("ColName"),
                            getDateFormat().parse(props.getProperty("DateGreater"))
                        );
                    } else {
                        throw new SQLException("greater value not found");
                    }
                } else {
                    dbRs = mt.selectAll();
                }
                dbRs.setStatement(stmt);
                ret = dbRs;
            } else {
                ret = MockSimpleResultSet.createFromRequest(stmt, props);
            }
            return ret;
        } catch (Exception e) {
            SQLException ne = new SQLException("See cause");
            ne.initCause(e);
            throw ne;
        }
    }

    private DateFormat m_dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    public DateFormat getDateFormat() {
        return m_dateFormat;
    }

    public synchronized void setResponseDelay(long delay) {
        m_responseDelay = delay;
    }
}
