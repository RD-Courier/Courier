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
package ru.rd.courier;

import org.w3c.dom.Element;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.DataReceiver;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.StandardOperationSupport;
import ru.rd.courier.scripting.expressions.string.PreparedTemplateExpr;
import ru.rd.courier.scripting.statements.Operation;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringContext;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.templates.HashMapStringContext;
import ru.rd.courier.utils.templates.SimplePreparedTemplate;

import java.sql.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Map;

class StandardUpdateOperation implements ScriptStatement {
    private CourierContext m_appl;
    private CourierLogger m_logger;
    private TargetProfile m_tProfile;
    private final String m_procPrefix;
    private Properties m_params;
    private String m_customBlock = null;
    private static final String c_updateOperation = "update-operation";
    private static final String c_dropProcOperation = "drop-proc-operation";

    public StandardUpdateOperation(
        CourierContext appl, CourierLogger logger, TargetProfile tProfile,  Element e
    ) throws CourierException {
        m_appl = appl;
        m_logger = logger;
        m_tProfile = tProfile;
        m_procPrefix = m_appl.getParam("standard-update-proc-prefix", "pcour_");
        m_params = DomHelper.getAttrParams(e);
        Element cb = DomHelper.getChild(e, "custom-block", false);
        if (cb != null) {
            m_customBlock = DomHelper.getNodeValue(cb);
        } else {
            m_customBlock = "";
        }
    }

    public void start(Context ctx) throws CourierException {
    }

    public void finish(final ru.rd.courier.scripting.Context ctx) throws CourierException {
        if (ctx.hasObject(c_dropProcOperation)) {
            try {
                final String name = m_appl.getScriptParam("target-db-name");
                DataReceiver receiver = ctx.getReceiver(name);
                if (!(receiver instanceof StandardOperationSupport)) {
                    throw new CourierException(
                        "Receiver " + name + " does not support standard operation");
                }
                StandardOperationSupport sos = (StandardOperationSupport)receiver;
                Connection con = sos.getConnection();
                if (con == null) {
                    throw new CourierException("Standard update can only be applied to JDBC receiver");
                }
                String catalog = con.getCatalog();
                try {
                    con.setCatalog("tempdb");
                    ((Operation)ctx.getObject(c_dropProcOperation)).exec(ctx);
                    receiver.flush();
                } finally {
                    con.setCatalog(catalog);
                }
            } catch(Exception e) {
                throw new CourierException(e);
            }
        }
    }

    private interface FieldToString {
        String process(String field, ResultSetMetaData md, int i) throws SQLException, CourierException;
    }

    private void iterateFields(
        ResultSetMetaData md, StringBuffer sb, String separator, FieldToString fts
    ) throws SQLException, CourierException {
        sb.setLength(0);
        StringBuffer sepReplacement = new StringBuffer(separator.length());
        for (int i = 0; i < separator.length(); i++) {
            sepReplacement.append(' ');
        }
        boolean notFirst = false;
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String str = fts.process(md.getColumnName(i), md, i);
            if (str != null) {
                sb.append('\n');
                if (notFirst) sb.append(separator);
                else sb.append(sepReplacement);
                sb.append(str);
                notFirst = true;
            }
        }
    }

    private static final String c_identity = "identity";
    private String getColumnType(Map<String, String> dp, ResultSetMetaData md, int i) throws SQLException {
        String type = md.getColumnTypeName(i);
        StringBuffer res = new StringBuffer(type);
        String driverClass = dp.get("class");
        if (driverClass.startsWith("com.microsoft.") || driverClass.startsWith("com.sybase.")) {
            int identityPos = type.toLowerCase().indexOf(c_identity);
            if (identityPos >= 0) {
                int endPos = identityPos + c_identity.length();
                if (identityPos > 0) identityPos--;
                res.delete(identityPos, endPos);
            }
        }

        //m_logger.debug("type --> " + res);

        String ps = "";
        switch(md.getColumnType(i)) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.BINARY:
            case Types.VARBINARY:
                if (md.getPrecision(i) == 0) {
                    ps = "(255)";
                } else {
                    ps = "(" + md.getPrecision(i) + ")";
                }
                break;
            case Types.FLOAT:
                if (md.getPrecision(i) == 0) {
                    ps = "(15)";
                } else {
                    ps = "(" + md.getPrecision(i) + ")";
                }
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                ps = "(" + md.getPrecision(i) + "," + md.getScale(i) + ")";
                break;
        }
        return res + ps;
    }

    private void prepare(Context ctx) throws CourierException, SQLException {
        final String name = m_appl.getScriptParam("target-db-name");
        DataReceiver receiver = ctx.getReceiver(name);
        if (!(receiver instanceof StandardOperationSupport)) {
            throw new CourierException(
                "Receiver " + name + " does not support standard operation");
        }
        StandardOperationSupport sos = (StandardOperationSupport)receiver;
        receiver.flush();
        Connection con = sos.getConnection();
        if (con == null) {
            throw new CourierException(
                "Standard update can only be applied to JDBC receiver");
        }
        String catalog = con.getCatalog();
        try {
            final Map<String, String> dp = m_appl.getDriverParams(sos.getType());
            StringContext vars = new HashMapStringContext();
            String tableName = m_params.getProperty("table-name");
            String userName = m_params.getProperty("user-name");
            String archDbName = m_params.getProperty("archive-db-name");
            String archTableName = "a_" + tableName;
            vars.setVar("TargetProfile", m_tProfile.getName());
            vars.setVar("Database", catalog);
            vars.setVar("TableName", tableName);
            vars.setVar("TimeStamp", m_params.getProperty("time-stamp"));
            vars.setVar("ArchiveDbName", archDbName);
            vars.setVar("ArchiveTableName", archTableName);
            vars.setVar("TimeStampFieldName", "CourierTimeStamp");

            ResultSet rs = ctx.getResultSet(m_appl.getScriptParam("data-rs-name"));
            ResultSetMetaData md = rs.getMetaData();

            final String varMark = dp.get("VarMark");
            final String StmtEndMark = dp.get("StatementEndMark");
            StringBuffer sb = new StringBuffer(32);

            String[] keyNames = StringHelper.splitString(m_params.getProperty("key-names"), ',');
            final Set<String> keyNamesSet = new HashSet<String>(keyNames.length);
            sb.setLength(0);
            for (int i = 0; i < keyNames.length; i++) {
                keyNames[i] = keyNames[i].trim();
                keyNamesSet.add(keyNames[i]);
                if (i != 0) sb.append(" and ");
                sb.append(keyNames[i] + " = " + varMark + keyNames[i]);
            }
            vars.setVar("KeyExpression", sb.toString());

            sb.setLength(0);
            for (int i = 0; i < keyNames.length; i++) {
                if (i != 0) sb.append(", ");
                sb.append(keyNames[i] + " ASC");
            }
            vars.setVar("KeyIndexNames", sb.toString());

            iterateFields(md, sb, ", ", new FieldToString(){
                public String process(String field, ResultSetMetaData md, int i) throws SQLException {
                    return field + " " + getColumnType(dp, md, i) + " NULL";
                }
            });
            vars.setVar("TableFieldsDefinitions", sb.toString());

            String procName = m_procPrefix + tableName.substring(
                0,
                tableName.length() - Math.max(0, m_procPrefix.length() + tableName.length() - 30)
            );
            vars.setVar("ProcName", procName);

            iterateFields(md, sb, ", ", new FieldToString(){
                public String process(String field, ResultSetMetaData md, int i) throws SQLException {
                    return varMark + field + " " + getColumnType(dp, md, i);
                }
            });
            vars.setVar("InputParams", sb.toString());

            String trimEnabledParam = m_params.getProperty("trim-strings");
            if ((trimEnabledParam != null) && (trimEnabledParam.equalsIgnoreCase("yes"))) {
                String sTmpl = dp.get("TrimFuncTemplate");
                final SimplePreparedTemplate trimTmpl = new SimplePreparedTemplate(sTmpl);
                sTmpl = dp.get("AssignTemplate");
                final SimplePreparedTemplate assignTmpl = new SimplePreparedTemplate(sTmpl);
                final HashMapStringContext sctx = new HashMapStringContext();
                final HashMapStringContext sctx2 = new HashMapStringContext();
                iterateFields(md, sb, "", new FieldToString(){
                    public String process(String field, ResultSetMetaData md, int i) throws CourierException, SQLException {
                        int type = md.getColumnType(i);
                        if((type != Types.CHAR) && (type != Types.VARCHAR)) return "";
                        sctx2.setVar("Argument", varMark + field);
                        sctx.setVar("VarName", varMark + field);
                        sctx.setVar("VarValue", trimTmpl.calculate(sctx2));
                        return (assignTmpl.calculate(sctx) + StmtEndMark + "\n");
                    }
                });
            } else {
                sb.setLength(0);
            }
            vars.setVar("StringsTrimming", sb.toString());

            iterateFields(md, sb, ", ", new FieldToString(){
                public String process(String field, ResultSetMetaData md, int i) {
                    return field;
                }
            });
            vars.setVar("FieldsList", sb.toString());

            iterateFields(md, sb, ", ", new FieldToString(){
                public String process(String field, ResultSetMetaData md, int i) {
                    return varMark + field;
                }
            });
            vars.setVar("InputParamsList", sb.toString());

            iterateFields(md, sb, " or ", new FieldToString(){
                public String process(String field, ResultSetMetaData md, int i) {
                    if (!keyNamesSet.contains(field)) {
                        return (
                            "(" + field + " <> " + varMark + field + ")" +
                            " or (" + field + " is NULL and " + varMark + field + " is not NULL)" +
                            " or (" + field + " is not NULL and " + varMark + field + " is NULL)"
                        );
                    }
                    else return null;
                }
            });
            vars.setVar("CompareValues", sb.toString());

            iterateFields(md, sb, ", ", new FieldToString(){
                public String process(String field, ResultSetMetaData md, int i) {
                    return field + " = " + varMark + field;
                }
            });
            vars.setVar("SetFields", sb.toString());

            Operation op;
            SimplePreparedTemplate tmpl;
            String scriptDbAlias = m_appl.getScriptParam("target-db-name");
            DatabaseMetaData dmd = con.getMetaData();
            ResultSet trs = dmd.getTables(catalog, userName, tableName, null);
            try {
                if (!trs.next()) {
                    tmpl = new SimplePreparedTemplate(
                        dp.get("StandardUpdate-CreateTableTemplate")
                    );
                    op = new Operation(scriptDbAlias, tmpl.calculate(vars));
                    op.start(ctx);
                    op.exec(ctx);
                    receiver.flush();
                    op.finish(ctx);

                    tmpl = new SimplePreparedTemplate(
                        dp.get("StandardUpdate-CreateIndexTemplate")
                    );
                    op = new Operation(scriptDbAlias, tmpl.calculate(vars));
                    op.start(ctx);
                    op.exec(ctx);
                    receiver.flush();
                    op.finish(ctx);
                }
            } finally {
                if (trs != null) trs.close();
            }

            if (archDbName != null) {
                trs = dmd.getTables(archDbName, userName, archTableName, null);
                try {
                    if (!trs.next()) {
                        tmpl = new SimplePreparedTemplate(
                            dp.get("StandardUpdate-CreateArchTableTemplate")
                        );
                        op = new Operation(scriptDbAlias, tmpl.calculate(vars));
                        con.setCatalog(archDbName);
                        op.start(ctx);
                        op.exec(ctx);
                        receiver.flush();
                        op.finish(ctx);
                    }
                } finally {
                    if (trs != null) trs.close();
                }
            }

            vars.setVar("CustomBlock", m_customBlock);

            String dropProc = m_params.getProperty("drop-procedure");
            if ((dropProc != null) && (dropProc.equalsIgnoreCase("yes"))) {
                tmpl = new SimplePreparedTemplate(
                    dp.get("StandardUpdate-DropProcTemplate")
                );
                op = new Operation(scriptDbAlias, tmpl.calculate(vars));
                ctx.setObject(c_dropProcOperation, op);
            }

            trs = dmd.getProcedures("tempdb", userName, procName);
            boolean procExists;
            try {
                procExists = trs.next();
            } finally {
                if (trs != null) trs.close();
            }
            if (!procExists) {
                tmpl = new SimplePreparedTemplate(
                    dp.get("StandardUpdate-CreateProcTemplate")
                );
                op = new Operation(scriptDbAlias, tmpl.calculate(vars));
                con.setCatalog("tempdb");
                op.start(ctx);
                op.exec(ctx);
                receiver.flush();
                op.finish(ctx);
            }

            iterateFields(md, sb, ", ", new FieldToString(){
                public String process(String field, ResultSetMetaData md, int i)
                throws SQLException {
                    String format = null;
                    switch(md.getColumnType(i)) {
                        case Types.CHAR:
                        case Types.VARCHAR:
                            format = "string";
                            break;
                        case Types.DATE:
                        case Types.TIME:
                        case Types.TIMESTAMP:
                            format = "string";
                            break;
                    }
                    format = format == null ? "" : "(" + format + ") ";
                    return "[%" + format + field + "]";
                }
            });
            vars.setVar("UpdateValues", sb.toString());

            tmpl = new SimplePreparedTemplate(
                dp.get("StandardUpdate-UpdateTemplate")
            );
            ctx.setObject(c_updateOperation, new Operation(
                scriptDbAlias, new PreparedTemplateExpr(tmpl.calculate(vars))
            ));
        } finally {
            con.setCatalog(catalog);
        }
    }

    public void exec(Context ctx) throws CourierException {
        try {
            if (!ctx.hasObject(c_updateOperation)) {
                prepare(ctx);
            }
            ((Operation)ctx.getObject(c_updateOperation)).exec(ctx);
        } catch (Exception e1) {
            throw new CourierException(e1);
        }
    }
}
