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
package ru.rd.courier.scripting.statements;

import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.utils.DomHelper;
import ru.rd.utils.Base64;

import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * User: AStepochkin
 * Date: 11.02.2008
 * Time: 15:10:49
 */
public class QueryXml extends QueryStatement {
    private final ScriptExpression m_varName;
    private final ScriptExpression m_recordTag;

    public QueryXml(
        ScriptExpression dbName, ScriptExpression sql,
        ScriptExpression varName, ScriptExpression recordTag
    ) {
        super(dbName, sql);
        m_varName = varName;
        m_recordTag = recordTag;
    }

    protected void processResultSet(ResultSet rs, Context ctx) throws SQLException {
        rsXmlToVar(rs, ctx, m_recordTag, m_varName);
    }

    public static void rsXmlToVar(ResultSet rs, Context ctx, ScriptExpression recordTag, ScriptExpression varName) throws SQLException {
        ctx.setVar(varName.calculate(ctx), resultSetToString(rs, ctx, recordTag.calculate(ctx)));
    }

    public static String resultSetToString(ResultSet rs, Context ctx, String recordTag) throws SQLException {
        final ResultSetMetaData md = rs.getMetaData();
        StringWriter buf = new StringWriter();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        int ri = 0;
        while (true) {
            if (ctx.isCanceled()) return null;
            long begTime = System.currentTimeMillis();
            if (!rs.next()) break;
            ctx.addSourceTime(System.currentTimeMillis() - begTime);
            if (ri > 0) buf.write("\n");
            buf.write("<" + recordTag + ">");
            for (int i = 1; i <= md.getColumnCount(); i++) {
                final String colName = md.getColumnName(i);
                buf.write("<" + colName + ">");
                String value;
                switch (md.getColumnType(i)) {
                    case Types.TIMESTAMP: {
                        value = df.format(rs.getTimestamp(i));
                        break;
                    }
                    case Types.BLOB: {
                        value = Base64.encodeBytes(rs.getBytes(i), Base64.DONT_BREAK_LINES);
                        break;
                    }
                    default: {
                        value = DomHelper.escString(rs.getString(i));
                    }
                }
                buf.write(value);
                buf.write("</" + colName + ">");
            }
            buf.write("</" + recordTag + ">");
            ri++;
        }
        return buf.toString();
    }
}
