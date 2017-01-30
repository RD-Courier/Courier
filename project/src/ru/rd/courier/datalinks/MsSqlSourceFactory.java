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

import com.microsoft.jdbcx.sqlserver.SQLServerDataSource;
import org.w3c.dom.Node;
import ru.rd.courier.CourierContext;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;

import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 16.04.2009
 * Time: 13:51:22
 */
public class MsSqlSourceFactory extends JavaXSourceFactory {
    public MsSqlSourceFactory(CourierLogger logger, String name, CourierContext ctx, Node conf) throws SQLException {
        super(logger, initSource(name, conf, ctx), confHost(conf), "select GetDate()", conf);
    }

    private static javax.sql.DataSource initSource(String name, Node conf, CourierContext ctx) throws SQLException {
        String sp;
        Boolean bp;
        Integer ip;

        SQLServerDataSource ds = new SQLServerDataSource();
        bp = boolParam(conf, "batch-performance-workaround");
        if (bp != null) ds.setBatchPerformanceWorkaround(bp);
        ds.setDatabaseName(DomHelper.getNodeAttr(conf, "db"));
        sp = strParam(conf, "data-source");
        if (sp != null) ds.setDataSourceName(sp);
        sp = strParam(conf, "description");
        if (sp != null) ds.setDescription(sp);
        bp = boolParam(conf, "embedded");
        if (bp != null) ds.setEmbedded(bp);
        ip = intParam(conf, "host-process");
        if (ip != null) ds.setHostProcess(ip);
        ip = intParam(conf, "login-timeout");
        if (ip != null) ds.setLoginTimeout(ip);
        //ds.setLogWriter(null);
        String host = confHost(conf);
        ds.setNetAddress(host);
        Account account = AccountUtils.confDbAccount(host, name, ctx.getAccountProvider(), conf);
        ds.setUser(account.name);
        ds.setPassword(account.password);
        ip = intParam(conf, "port");
        if (ip != null) ds.setPortNumber(ip);
        sp = strParam(conf, "program-name");
        if (sp == null) sp = getProgName(name, ctx);
        if (sp != null) ds.setProgramName(sp);
        sp = strParam(conf, "role-name");
        if (sp != null) ds.setRoleName(sp);
        sp = strParam(conf, "select-method");
        if (sp != null) ds.setSelectMethod(sp);
        bp = boolParam(conf, "send-string-parameters-as-unicode");
        if (bp != null) ds.setSendStringParametersAsUnicode(bp);
        sp = strParam(conf, "server");
        if (sp == null) sp = host;
        ds.setServerName(sp);
        sp = strParam(conf, "spy-attributes");
        if (sp != null) ds.setSpyAttributes(sp);
        sp = getHostParam(ctx);
        if (sp != null) ds.setWSID(sp);
        return ds;
    }
}
