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
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.logging.CourierLogger;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

import com.sybase.jdbc2.jdbc.SybDataSource;

/**
 * User: AStepochkin
 * Date: 17.04.2009
 * Time: 13:20:52
 */
public class SybaseSourceFactory extends JavaXSourceFactory {
    public SybaseSourceFactory(CourierLogger logger, String name, CourierContext ctx, Node conf) throws SQLException {
        super(logger, initSource(name, conf, ctx), confHost(conf), "select GetDate()", conf);
    }

    private static DataSource initSource(String name, Node conf, CourierContext ctx) throws SQLException {
        String sp;
        Integer ip;

        SybDataSource ds = new SybDataSource();
        Properties props = new Properties();
        props.setProperty("APPLICATIONNAME", getProgName(name, ctx));
        props.setProperty("HOSTNAME", getHostParam(ctx));
        ds.setConnectionProperties(props);
        String host = confHost(conf);
        ds.setServerName(host);
        ds.setDatabaseName(DomHelper.getNodeAttr(conf, "db"));
        Account account = AccountUtils.confDbAccount(host, name, ctx.getAccountProvider(), conf);
        ds.setUser(account.name);
        ds.setPassword(account.password);
        sp = strParam(conf, "data-source");
        if (sp != null) ds.setDataSourceName(sp);
        sp = strParam(conf, "description");
        if (sp != null) ds.setDescription(sp);
        ip = intParam(conf, "login-timeout");
        if (ip != null) ds.setLoginTimeout(ip);
        //ds.setMessageHandler();
        sp = strParam(conf, "network-protocol");
        if (sp != null) ds.setNetworkProtocol(sp);
        ip = intParam(conf, "port");
        if (ip != null) ds.setPortNumber(ip);
        sp = strParam(conf, "resource-manager-name");
        if (sp != null) ds.setResourceManagerName(sp);
        ip = intParam(conf, "resource-manager-type");
        if (ip != null) ds.setResourceManagerType(ip);
        //ds.setSybMessageHandler();
        return ds;
    }
}
