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
import ru.rd.courier.CourierContext;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.jdbc.JdbcStringSource;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * User: AStepochkin
 * Date: 17.04.2009
 * Time: 13:17:00
 */
public class JavaXSourceFactory implements PoolObjectFactory, HostProvider {
    protected final CourierLogger m_logger;
    protected final DataSource m_ds;
    private final String m_testSql;
    private final int m_checkWaitTimeout;
    private final int m_maxErrorsCount;
    private final String m_host;
    protected final boolean m_autoCommit;

    public JavaXSourceFactory(
        CourierLogger logger, DataSource ds,
        String host,
        String testSql, int checkWaitTimeout, int maxErrorsCount,
        boolean autoCommit
    ) {
        m_logger = logger;
        m_ds = ds;
        m_host = host;
        m_testSql = testSql;
        m_checkWaitTimeout = checkWaitTimeout;
        m_maxErrorsCount = maxErrorsCount;
        m_autoCommit = autoCommit;
    }

    public JavaXSourceFactory(CourierLogger logger, DataSource ds, String host, String defCheckSql, Node conf) {
        this(
            logger, ds, host,
            defCheckSql == null ?
                DomHelper.getNodeAttr(conf, "check-sql") : DomHelper.getNodeAttr(conf, "check-sql", defCheckSql),
            DomHelper.getIntNodeAttr(conf, "check-wait-timeout", 10 * 1000),
            DomHelper.getIntNodeAttr(conf, "max-consecutive-errors", -1),
            !DomHelper.getBoolYesNo(conf, "cancel-auto-commit", false)
        );
    }

    public JavaXSourceFactory(CourierLogger logger, DataSource ds, String host, Node conf) {
        this(logger, ds, host, null, conf);
    }

    public Object getObject(ObjectPoolIntf pool) throws Exception {
        return new JdbcStringSource(
            m_logger, m_ds.getConnection(),
            null, //"com.microsoft.jdbc.sqlserver.SQLServerDriver"
            m_autoCommit
        );
    }

    public boolean checkObject(Object o) {
        JdbcStringSource so = (JdbcStringSource)o;
        if (m_maxErrorsCount > 0 && so.getFailedUseCount() > m_maxErrorsCount) return false;

        boolean ret = false;
        try {
            Statement stmt = so.getConnection().createStatement();
            try {
                if (m_checkWaitTimeout > 0) stmt.setQueryTimeout(m_checkWaitTimeout);
                stmt.execute(m_testSql);
                ret = true;
            } finally {
                try { if (stmt != null) stmt.close(); }
                catch (Exception e) { m_logger.warning(e); }
            }
        } catch (Exception e) {
            m_logger.error(e);
        }
        return ret;
    }

    public void returnObject(Object o) {
        try {
            JdbcStringSource so = (JdbcStringSource)o;
            so.getConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Boolean boolParam(Node conf, String name) {
        if (DomHelper.hasAttr(conf, name)) {
            return DomHelper.getBoolYesNo(conf, name);
        } else {
            return null;
        }
    }

    protected static Integer intParam(Node conf, String name) {
        if (DomHelper.hasAttr(conf, name)) {
            return DomHelper.getIntNodeAttr(conf, name);
        } else {
            return null;
        }
    }

    protected static String strParam(Node conf, String name) {
        return DomHelper.getNodeAttr(conf, name, null);
    }

    protected static String strParam(Node conf, String name, String def) {
        return DomHelper.getNodeAttr(conf, name, def);
    }

    protected static String getProgName(String name, CourierContext ctx) {
        return ctx.getParam("program-name") + ':' + name;
    }

    protected static String getHostParam(CourierContext ctx) {
        return ctx.getParam("host");
    }

    protected static String confHost(Node conf) {
        return DomHelper.getNodeAttr(conf, "host");
    }

    public String getHost() {
        return m_host;
    }
}
