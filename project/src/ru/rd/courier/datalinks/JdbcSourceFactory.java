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

import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.PoolException;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.jdbc.PropConnectionFactory;
import ru.rd.courier.scripting.dataaccess.jdbc.JdbcStringSource;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.RegexExtractor;
import ru.rd.courier.utils.StringHelper;
import ru.rd.utils.Disposable;
import org.w3c.dom.Node;

import java.util.Properties;
import java.util.Map;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 16.04.2009
 * Time: 14:33:10
 */
public final class JdbcSourceFactory implements PoolObjectFactory, JdbcObjectFactory, HostProvider, Disposable {
    private final CourierLogger m_logger;
    private final PropConnectionFactory m_conFactory;
    private final String m_host;
    private final String m_driverClass;
    private final boolean m_autoCommit;

    private static final String[] s_hostExtractorsData = new String[] {
        "jdbc:microsoft:sqlserver://([^;]+).*",
        "jdbc:sybase:Tds:([^:/]+).*"
    };
    private static final RegexExtractor s_hostExtractor = new RegexExtractor(s_hostExtractorsData, Pattern.CASE_INSENSITIVE);

    public static String extractMetaInfo(
        String driverClass, String name, Properties appProps,
        Map<String, Map<String, String>> driverInfos, Properties info
    ) {
        return extractMetaInfo(
            driverClass, name,
            appProps.getProperty("host"), appProps.getProperty("program-name"),
            driverInfos, info
        );
    }

    public static String extractMetaInfo(
        String driverClass, String name, String appHost, String progName,
        Map<String, Map<String, String>> driverInfos, Properties info
    ) {
        String progNameParamName = null;
        if (driverInfos.containsKey(driverClass)) {
            Map<String, String> di = driverInfos.get(driverClass);
            if (di.containsKey("host-param-name")) {
                info.setProperty(di.get("host-param-name"), appHost);
            }
            if (di.containsKey("program-param-name")) {
                progNameParamName = di.get("program-param-name");
                info.setProperty(progNameParamName, progName + ":" + name);
            }
        }
        return progNameParamName;
    }

    private String m_safeCommitSql;
    private String m_safeRollbackSql;

    public void initMetaInfo(String driverClass, Map<String, Map<String, String>> driverInfos) {
        Map<String, String> di = driverInfos.get(driverClass);
        if (di == null) return;
        m_safeCommitSql = StringHelper.stringParam(di, "safe-commit", null);
        m_safeRollbackSql = StringHelper.stringParam(di, "safe-rollback", null);
    }

    private String m_testSql = null;
    private final int m_checkWaitTimeout;
    private final int m_maxErrorsCount;

    public JdbcSourceFactory(
        CourierLogger poolLogger, Node n, String name, String appHost, String progName,
        Map<String, Map<String, String>> driverInfos, AccountProvider aprovider
    ) throws PoolException {
        m_logger = poolLogger;
        final Properties info = new Properties();
        m_driverClass = DomHelper.getNodeAttr(n, "driver");
        info.putAll(DomHelper.getElementParams(n));
        final String url = DomHelper.getNodeAttr(n, "url");
        m_host = s_hostExtractor.extract(url);
        Account account = AccountUtils.confAccount(m_host, "DB", name, aprovider, n);
        info.setProperty("user", account.name);
        info.setProperty("password", account.password);
        String progNameParamName;
        if (appHost != null &&  progName != null) {
            progNameParamName = extractMetaInfo(m_driverClass, name, appHost, progName, driverInfos, info);
        } else {
            progNameParamName = null;
        }
        m_testSql = DomHelper.getNodeAttr(n, "check-sql");
        m_checkWaitTimeout = DomHelper.getIntNodeAttr(n, "check-wait-timeout", 10 * 1000);
        m_maxErrorsCount = DomHelper.getIntNodeAttr(n, "max-consecutive-errors", -1);
        m_conFactory = new PropConnectionFactory(poolLogger, m_driverClass, url, info, progNameParamName);
        initMetaInfo(m_driverClass, driverInfos);
        m_autoCommit = !DomHelper.getBoolYesNo(n, "cancel-auto-commit", false);
    }

    public JdbcSourceFactory(
        CourierLogger poolLogger, Node n, String name, Properties appProps,
        Map<String, Map<String, String>> driverInfos, AccountProvider aprovider
    ) throws PoolException {
        this(
            poolLogger, n, name,
            appProps.getProperty("host"), appProps.getProperty("program-name"),
            driverInfos, aprovider
        );
    }

    public Object getObject(ObjectPoolIntf pool) {
        Connection con = (Connection)m_conFactory.getObject(pool);
        try {
            JdbcStringSource ret = new JdbcStringSource(m_logger, con, m_driverClass, m_autoCommit);
            ret.setSafeCommitSql(m_safeCommitSql);
            ret.setSafeRollbackSql(m_safeRollbackSql);
            return ret;
        } catch (Exception e) {
            try { con.close(); } catch (Exception e1) { m_logger.error(e1); }
            throw new RuntimeException(e);
        }
    }

    public void returnObject(Object o) {
        try {
            ((JdbcStringSource)o).getConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkObject(Object o) {
        JdbcStringSource so = (JdbcStringSource)o;
        if (m_maxErrorsCount > 0 && so.getFailedUseCount() > m_maxErrorsCount) return false;

        boolean ret = false;
        try {
            if (so.getConnection() == null || so.getConnection().isClosed()) {
                return false;
            }
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

    public void dispose() {
        if (m_conFactory instanceof Disposable) {
            ((Disposable)m_conFactory).dispose();
        }
    }

    public String getUrl() {
        return m_conFactory.getUrl();
    }

    public String getHost() {
        return m_host;
    }
}
