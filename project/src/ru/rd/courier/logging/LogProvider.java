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
package ru.rd.courier.logging;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.CourierContext;
import ru.rd.courier.CourierException;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.logging.data.DaysFileLog;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.thread.Condition;
import ru.rd.thread.ThreadHelper;
import ru.rd.thread.WorkThread;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogProvider {
    private File m_logPath;
    private Map<String, HandlerProvider> m_handlerTypes;
    private CourierLogger m_rootLog = null;
    private boolean m_deleteUnknownFiles;
    private File m_pipesLogDir;
    private final String m_prefix;
    private final String m_postfix;
    private String m_pipesLogDateFormat;
    private int m_logStoreDays;
    private long m_logMaxSize = 0;

    public abstract static class HandlerProvider {
        public Handler getHandler(Node n) throws CourierException {
            Properties params = new Properties();
            Element[] nl = DomHelper.getChildrenByTagName(n, "param", false);
            for (Element nd : nl) {
                params.put(
                        DomHelper.getNodeAttr(nd, "name"),
                        DomHelper.getNodeAttr(nd, "value")
                );
            }
            checkParams(params);
            Handler ret = getHandler(params);
            ret.setLevel(stringToLevel(DomHelper.getNodeAttr(n, "level")));
            return ret;
        }

        protected abstract Handler getHandler(Properties params) throws CourierException;
        protected void checkParams(Properties params) throws CourierException {}
    }

    private static final String c_allLevel = "all";
    private static final String c_infoLevel = "info";
    private static final String c_warningLevel = "warning";
    private static final String c_errorLevel = "error";
    private static final String c_offLevel = "off";

    private static Map<String, Level> s_levelMap = new HashMap<String, Level>();
    static {
        s_levelMap.put(c_allLevel, Level.ALL);
        s_levelMap.put(c_infoLevel, Level.INFO);
        s_levelMap.put(c_warningLevel, Level.WARNING);
        s_levelMap.put(c_errorLevel, Level.SEVERE);
        s_levelMap.put(c_offLevel, Level.OFF);
    }

    public static Level stringToLevel(String level) {
        return s_levelMap.get(level);
    }

    private static void checkParam(
        Properties params, String name, String loggerType
    ) throws CourierException {
        if (!params.containsKey(name)) {
            throw new CourierException(
                "Paramater '" + name + "' for logger " + loggerType + " not found");
        }
    }

    public LogProvider(final CourierContext appl, Element n) throws CourierException {
        String logPathAttr = DomHelper.getNodeAttr(n, "dir", false);
        if (logPathAttr == null) logPathAttr = "";
        m_logPath = appl.getAppFile(logPathAttr);
        if (!m_logPath.exists()) {
            if (!m_logPath.mkdirs()) {
                throw new CourierException(
                    "Unable to create catalog: " + m_logPath.getAbsolutePath());
            }
        }
        Node dn = DomHelper.getChild(n, "transfer-data");
        m_pipesLogDir = new File(
            m_logPath, DomHelper.getNodeAttr(dn, "pipelines-dir")
        );
        m_prefix = DomHelper.getNodeAttr(dn, "file-name-prefix", "");
        m_postfix = DomHelper.getNodeAttr(dn, "file-name-postfix", "");
        m_pipesLogDateFormat = DomHelper.getNodeAttr(dn, "date-format");
        m_logStoreDays = DomHelper.getIntNodeAttr(dn, "store-days");
        m_logMaxSize = DomHelper.getLongNodeAttr(dn, "max-size-megabytes", 100) * 1024 * 1024;
        m_deleteUnknownFiles = DomHelper.getBoolYesNo(dn, "delete-unknown-files", false);
        initHandlerProviders(appl);
        m_rootLog = new LoggerAdapter(this, null, DomHelper.getChild(n, "logger"));
    }

    private void initHandlerProviders(final CourierContext appl) {
        m_handlerTypes = new HashMap<String, HandlerProvider>();
        final String cFileType = "file-by-days";
        m_handlerTypes.put(
            cFileType,
            new HandlerProvider() {
                public Handler getHandler(Properties params) throws CourierException {
                    checkParam(params, "dir", cFileType);
                    String prop = params.getProperty("file-name-prefix");
                    String prefix = prop == null ? "" : prop;
                    prop = params.getProperty("file-name-postfix");
                    String postfix = prop == null ? "" : prop;
                    return new DaysFileLogHandler(
                        getLogFile(params.getProperty("dir")),
                        params.getProperty("date-format"),
                        Integer.parseInt(params.getProperty("days")),
                        prefix, postfix,
                        StringHelper.boolParam(params, "delete-unknown-files", false),
                        true,
                        StringHelper.intParam(params, "max-size-megabytes", 100) * 1024 * 1024
                    );
                }
            }
        );
        m_handlerTypes.put(
            "mail",
            new HandlerProvider() {
                private static final String c_SubjectTemplateAttr = "subject-template";
                private static final String c_SubjectPrefixAttr = "subject-prefix";
                public Handler getHandler(Properties params) throws CourierException {
                    try {
                        Handler mh = new MailHandler(
                            appl.getAppFile("").getAbsolutePath(),
                            params.getProperty(c_SubjectTemplateAttr),
                            params.getProperty("smtp-host"),
                            params.getProperty("from", ""),
                            params.getProperty("to").split(","),
                            StringHelper.boolParam(params, "publish-stack-trace", false),
                            StringHelper.timeParam(params, "connect-timeout", 60*1000),
                            StringHelper.timeParam(params, "send-timeout", 20*1000)
                        );

                        long sameInterval = StringHelper.longParam(
                            params, "same-message-interval-min", -1
                        );

                        if (sameInterval <= 0) return mh;

                        return new SameDiscardLogHandler(
                            mh, sameInterval * 60 * 1000,
                            StringHelper.intParam(params, "same-message-buffer-size", 0)
                        );
                    } catch (Exception e) {
                        throw new CourierException(e);
                    }
                }
                protected void checkParams(Properties params) throws CourierException {
                    if (
                        !params.containsKey(c_SubjectTemplateAttr) &&
                        !params.containsKey(c_SubjectPrefixAttr)
                    ) {
                        throw new CourierException(
                            "One of attributes: " + c_SubjectTemplateAttr +
                                ", " + c_SubjectPrefixAttr +
                            "should be specified"
                        );
                    }
                    if (
                        !params.containsKey(c_SubjectTemplateAttr) &&
                        params.containsKey(c_SubjectPrefixAttr)
                    ) {
                        params.put(
                            c_SubjectTemplateAttr,
                            params.get(c_SubjectPrefixAttr) + "[%level]: [%message]"
                        );
                        params.remove(c_SubjectPrefixAttr);
                    }
                }
            }
        );
        m_handlerTypes.put(
            "console",
            new HandlerProvider() {
                public Handler getHandler(Properties params) {
                    Handler ih = new ConsoleHandlerEx(System.out);
                    ih.setLevel(Level.ALL);
                    ih.setFormatter(new SimpleFormatter("{0,time}"));
                    Handler eh = new ConsoleHandlerEx(System.err);
                    eh.setLevel(Level.ALL);
                    eh.setFormatter(new SimpleFormatter("{0,time}"));
                    return new SwitchStreamHandler(Level.INFO, ih, eh);
                }
            }
        );

        m_handlerTypes.put(
            "test",
            new HandlerProvider() {
                public Handler getHandler(Properties params) {
                    Handler h = new Handler() {
                        private final Object m_lock = new Object();
                        private final long m_timeout = 1 * 1000;
                        private WorkThread m_thread = null;

                        public synchronized void publish(LogRecord record) {
                            if (m_thread == null) {
                                m_thread = new WorkThread(m_rootLog, "test handler");
                            }
                            boolean res = m_thread.launchWorkAndWait(new Runnable() {
                                public void run() {
                                    ThreadHelper.waitEvent(
                                        m_lock,
                                        new Condition() {
                                            public boolean isTrue() {
                                                return false;
                                            }
                                        },
                                        0
                                    );
                                }
                            }, m_timeout);
                            if (!res) {
                                m_thread.closeOrDump(1);
                                m_thread = null;
                            }
                        }

                        public void flush() {}
                        public void close() throws SecurityException {}
                    };
                    h.setFormatter(new SimpleFormatter());
                    return new SameDiscardLogHandler(
                        h,
                        StringHelper.timeParam(params, "send-interval", 60*1000),
                        StringHelper.intParam(params, "buffer-size", 0)
                    );
                }
            }
        );
    }

    public LoggerAdapter getLogger(String  name) {
        if ((name == null) || (name.equals(""))) return (LoggerAdapter)m_rootLog;
        String[] logNames = StringHelper.splitString(name, '.');
        CourierLogger res = m_rootLog;
        for (String logName : logNames) {
            res = res.getChild(logName);
        }
        return (LoggerAdapter)res;
    }

    public DataLogger getPipeLogger(String pipeName, boolean buffered) {
        DataLogger l = new DaysFileLog(
            ConsoleCourierLogger.instance(),
            new File(m_pipesLogDir, pipeName), "cp1251", buffered,
            m_pipesLogDateFormat, m_logStoreDays, m_prefix, m_postfix,
            m_deleteUnknownFiles, true, m_logMaxSize
        );
//        l = new AsyncDataLogger(this, l, getThreadPool(), getTimer(), 5000, 100, 1000000);
        return l;
    }

    private File getLogFile(String name) {
        if ((name == null) || name.equals("")) return m_logPath;
        return new File(m_logPath, name);
    }

    public void close() {
        m_rootLog.stop();
    }

    public HandlerProvider getHandlerProvider(String name) {
        return m_handlerTypes.get(name);
    }
}
