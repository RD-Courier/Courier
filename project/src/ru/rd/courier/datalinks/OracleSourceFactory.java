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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.CourierContext;
import ru.rd.courier.utils.DomHelper;
import org.w3c.dom.Node;

import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;

/**
 * User: AStepochkin
 * Date: 20.07.2009
 * Time: 13:58:33
 */
public class OracleSourceFactory extends JavaXSourceFactory {
    public OracleSourceFactory(CourierLogger logger, String name, CourierContext ctx, Node conf) throws SQLException {
        super(logger, initSource(name, conf, ctx), confHost(conf), "select 1 from dual", conf);
    }

    private static javax.sql.DataSource initSource(String name, Node conf, CourierContext ctx) throws SQLException {
        String sp;
        Boolean bp;
        Integer ip;

        OracleDataSource ds = new OracleDataSource();
        sp = strParam(conf, "driver-type", "thin");
        ds.setDriverType(sp);
        String host = confHost(conf);
        sp = strParam(conf, "server");
        if (sp == null) sp = host;
        ds.setServerName(sp);
        ip = intParam(conf, "port");
        if (ip != null) ds.setPortNumber(ip);
        sp = strParam(conf, "service-name");
        if (sp != null) ds.setServiceName(sp);
        sp = DomHelper.getNodeAttr(conf, "db");
        ds.setDatabaseName(sp);
        Account account = AccountUtils.confDbAccount(host, name, ctx.getAccountProvider(), conf);
        ds.setUser(account.name);
        ds.setPassword(account.password);

        sp = strParam(conf, "connection-cache-name");
        if (sp != null) ds.setConnectionCacheName(sp);
        bp = boolParam(conf, "connection-caching-enabled");
        if (bp != null) ds.setConnectionCachingEnabled(bp);
        sp = strParam(conf, "data-source");
        if (sp != null) ds.setDataSourceName(sp);
        sp = strParam(conf, "description");
        if (sp != null) ds.setDescription(sp);
        sp = strParam(conf, "tns-entry");
        if (sp != null) ds.setTNSEntryName(sp);
        bp = boolParam(conf, "explicit-caching-enabled");
        if (bp != null) ds.setExplicitCachingEnabled(bp);
        bp = boolParam(conf, "fast-connection-failover-enabled");
        if (bp != null) ds.setFastConnectionFailoverEnabled(bp);
        bp = boolParam(conf, "implicit-caching-enabled");
        if (bp != null) ds.setImplicitCachingEnabled(bp);
        ip = intParam(conf, "login-timeout");
        if (ip != null) ds.setLoginTimeout(ip);
        //ds.setLogWriter(null);
        sp = strParam(conf, "network-protocol");
        if (sp != null) ds.setNetworkProtocol(sp);
        sp = strParam(conf, "ons-configuration");
        if (sp != null) ds.setONSConfiguration(sp);
        sp = strParam(conf, "url");
        if (sp != null) ds.setURL(sp);
        return ds;
    }
}

