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
package ru.rd.courier.manager;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;

import java.util.logging.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.LinkedList;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 * User: STEPOCHKIN
 * Date: 29.09.2008
 * Time: 19:44:56
 */
public class LoggerAdapter implements CourierLogger {
    protected String m_name;
    protected Logger m_logger;
    protected CourierLogger m_parent;
    protected Map<String, CourierLogger> m_children = new HashMap<String, CourierLogger>();
    private Collection<Handler> m_handlers = new LinkedList<Handler>();

    public LoggerAdapter(LoggerAdapter parent, String name, boolean attachToManager) {
        m_parent = parent;
        m_name = name;

        m_logger = null;
        if (attachToManager) {
            m_logger = LogManager.getLogManager().getLogger(m_name);
            if (m_logger == null) {
                m_logger = Logger.getLogger(m_name);
            }
        } else {
            m_logger = Logger.getAnonymousLogger();
        }
        if (parent != null) {
            parent.addChildLogger(m_name, this);
            m_logger.setParent(parent.getInnerLogger());
        }
        m_logger.setLevel(Level.ALL);
    }

    public LoggerAdapter(LoggerAdapter parent, String name) {
        this(parent, name, (parent == null) && (name != null));
    }

    public LoggerAdapter(
        XmlLogProvider logProvider, LoggerAdapter parent, Node n
    ) {
        this(parent, DomHelper.getNodeAttr(n, "name"));
        try {
            Logger logger = getInnerLogger();
            logger.setUseParentHandlers(parent != null && !DomHelper.getBoolYesNo(n, "cancel-parent", false));
            logger.setLevel(
                XmlLogProvider.stringToLevel(DomHelper.getNodeAttr(n, "level"))
            );
            Element[] nl = DomHelper.getChildrenByTagName(n, "handler", false);
            for (Element cn : nl) {
                Handler h = logProvider.getHandlerProvider(
                        DomHelper.getNodeAttr(cn, "type")
                ).getHandler(cn);
                logger.addHandler(h);
                m_handlers.add(h);
            }

            nl = DomHelper.getChildrenByTagName(n, "logger", false);
            for (Element cn : nl) {
                new LoggerAdapter(logProvider, this, cn);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Error initialize logger '" + m_name + "'", e);
        }
    }

    public String getLoggerName() {
        return m_name;
    }

    public CourierLogger getParentLogger() {
        return m_parent;
    }

    public CourierLogger getChild(String name) {
        return m_children.get(name);
    }

    public void stop() {
        for(Handler h: m_handlers) {
            m_logger.removeHandler(h);
            h.close();
        }
        for(CourierLogger l: m_children.values()) {
            l.stop();
        }
    }

    protected void addChildLogger(String name, CourierLogger logger) {
        m_children.put(name, logger);
    }

    public Logger getInnerLogger() {
        return m_logger;
    }

    public void debug(String msg) {
        m_logger.fine(msg);
    }

    public void info(String msg) {
        m_logger.info(msg);
    }

    public void warning(String msg) {
        m_logger.warning(msg);
    }

    public void warning(Throwable e) {
        LogRecord lr = new LogRecord(Level.WARNING, e.getMessage());
        lr.setThrown(e);
        m_logger.log(lr);
    }

    public void warning(String msg, Throwable e) {
        LogRecord lr = new LogRecord(Level.WARNING, msg);
        lr.setThrown(e);
        m_logger.log(lr);
    }

    public void error(String msg) {
        m_logger.severe(msg);
    }

    public void error(Throwable e) {
        LogRecord lr = new LogRecord(Level.SEVERE, e.getMessage());
        lr.setThrown(e);
        m_logger.log(lr);
    }

    public void error(String msg, Throwable e) {
        LogRecord lr = new LogRecord(Level.SEVERE, msg);
        lr.setThrown(e);
        m_logger.log(lr);
    }

    private void logTracedMsg(final Level level, final String msg) {
        final Throwable t = new Throwable();
        final StackTraceElement[] trace =  t.getStackTrace();
        String className = "unknown";
        String methodName = "unknown";
        String lineMsg = "";
        if (trace.length > 2) {
            final StackTraceElement te = trace[trace.length - 3];
            className = te.getClassName();
            methodName = te.getMethodName();
            lineMsg = "(" + te.getFileName() + ":" + te.getLineNumber() + ")";
        }
        m_logger.logp(level, className, methodName, msg + lineMsg);
    }
}
