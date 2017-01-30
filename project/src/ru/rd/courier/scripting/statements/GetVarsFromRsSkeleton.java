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

import ru.rd.courier.scripting.*;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringHelper;
import ru.rd.utils.Base64;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 05.07.2005
 * Time: 15:44:24
 */
public abstract class GetVarsFromRsSkeleton implements ScriptStatement {
    private boolean m_errorAsNull;
    private String m_prefix;
    private String m_xmlvar;
    private String m_xmlRecordTag;
    private boolean m_xmlAttributes;

    protected GetVarsFromRsSkeleton(
        boolean errorAsNull, String prefix,
        String xmlvar, String xmlRecordTag, boolean xmlAttributes
    ) {
        setErrorAsNull(errorAsNull);
        setPrefix(prefix);
        setXmlVar(xmlvar);
        setXmlRecordTag(xmlRecordTag);
        setXmlAttributes(xmlAttributes);
    }

    protected GetVarsFromRsSkeleton(boolean errorAsNull, String prefix) {
        setErrorAsNull(errorAsNull);
        setPrefix(prefix);
    }

    protected GetVarsFromRsSkeleton(boolean errorAsNull) {
        setErrorAsNull(errorAsNull);
    }

    protected GetVarsFromRsSkeleton() {
        m_errorAsNull = false;
        m_prefix = null;
        m_xmlvar = null;
        m_xmlRecordTag = null;
        m_xmlAttributes = false;
    }

    public void setErrorAsNull(boolean errorAsNull) {
        m_errorAsNull = errorAsNull;
    }

    public void setPrefix(String prefix) {
        m_prefix = prefix;
    }

    public void setPrefix(ScriptExpression prefix, Context ctx) {
        m_prefix = prefix == null ? null : prefix.calculate(ctx);
    }

    public void setXmlVar(String xmlvar) {
        m_xmlvar = xmlvar;
    }

    public void setXmlRecordTag(String xmlRecordTag) {
        m_xmlRecordTag = xmlRecordTag;
    }

    public void setXmlAttributes(boolean xmlAttributes) {
        m_xmlAttributes = xmlAttributes;
    }

    protected abstract ResultSet getResultSet(final Context ctx) throws CourierException;

    public final void start(final Context ctx) {}
    public final void finish(final Context ctx) {}

    public final void exec(final Context ctx) throws CourierException {
        getVars(ctx);
    }

    private void getVars(final Context ctx) throws CourierException {
        try {
            String prefix = m_prefix;
            ResultSet rs = getResultSet(ctx);
            final ResultSetMetaData md = rs.getMetaData();
            StringBuffer xml = null;
            if (m_xmlvar != null) {
                xml = new StringBuffer(md.getColumnCount() * 10);
                if (m_xmlAttributes) {
                    xml.append(m_xmlRecordTag == null ? "<record" : "<" + m_xmlRecordTag);
                } else {
                    if (m_xmlRecordTag != null) {
                        xml.append('<').append(m_xmlRecordTag).append('>');
                    }
                }
            }
            for (int i = 1; i <= md.getColumnCount(); i++) {
                if (ctx.isCanceled()) return;
                String colName = md.getColumnName(i);
                String fieldName = colName;
                if (prefix != null) fieldName = prefix + fieldName;
                try {
                    switch (md.getColumnType(i)) {
                        case Types.TIMESTAMP: {
                            ctx.setDateVar(fieldName, rs.getTimestamp(i));
                            break;
                        }
                        case Types.DATE: {
                            ctx.setDateVar(fieldName, rs.getDate(i));
                            break;
                        }
                        case Types.BLOB: {
                            ctx.setVar(
                                fieldName,
                                Base64.encodeBytes(rs.getBytes(i), Base64.DONT_BREAK_LINES)
                            );
                            break;
                        }
                        default: {
                            ctx.setVar(fieldName, rs.getString(i));
                        }
                    }
                } catch (SQLException e) {
                    if (m_errorAsNull) {
                        ctx.setVar(fieldName, (String)null);
                        ctx.error("Error getting column '" + colName + "'", e);
                    } else {
                        throw new CourierException(
                            "Error getting column '" + colName + "'", e);
                    }
                }
                if (xml != null) {
                    String v = ctx.getVar(fieldName);
                    v = StringHelper.replaceChars(v, "<\"", new String[] {"&lt;", "&quot;"});
                    if (m_xmlAttributes) {
                        xml.append(' ').append(fieldName).append("=\"").append(v).append('"');
                    } else {
                        xml.append('<').append(fieldName).append('>');
                        xml.append(v);
                        xml.append('<').append('/').append(fieldName).append('>');
                    }
                }
            }
            if (xml != null) {
                if (m_xmlAttributes) {
                    xml.append("/>");
                } else {
                    if (m_xmlRecordTag != null) {
                        xml.append("</").append(m_xmlRecordTag).append('>');
                    }
                }
                ctx.setVar(m_xmlvar, xml.toString());
            }
        } catch (SQLException e) {
            throw new CourierException(e);
        }
    }

    public static void getVarsFromRs(
        final ResultSet rs, final Context ctx, boolean errorAsNull, String prefix
    ) throws CourierException {
        GetVarsFromDirectRs mover = new GetVarsFromDirectRs(rs);
        mover.setErrorAsNull(errorAsNull);
        mover.setPrefix(prefix);
        mover.exec(ctx);
    }

    public static void getVarsFromRs(
        final ResultSet rs, final Context ctx, boolean errorAsNull, String prefix,
        String xmlvar, String xmlRecordTag, boolean xmlAttributes
    ) throws CourierException {
        try {
            final ResultSetMetaData md = rs.getMetaData();
            StringBuffer xml = null;
            if (xmlvar != null) {
                xml = new StringBuffer(md.getColumnCount() * 10);
                if (xmlAttributes) {
                    xml.append(xmlRecordTag == null ? "<record" : "<" + xmlRecordTag);
                } else {
                    if (xmlRecordTag != null) {
                        xml.append('<').append(xmlRecordTag).append('>');
                    }
                }
            }
            for (int i = 1; i <= md.getColumnCount(); i++) {
                if (ctx.isCanceled()) return;
                String colName = md.getColumnName(i);
                String fieldName = colName;
                if (prefix != null) fieldName = prefix + fieldName;
                try {
                    switch (md.getColumnType(i)) {
                        case Types.TIMESTAMP: {
                            ctx.setDateVar(fieldName, rs.getTimestamp(i));
                            break;
                        }
                        case Types.DATE: {
                            ctx.setDateVar(fieldName, rs.getDate(i));
                            break;
                        }
                        case Types.BLOB: {
                            ctx.setVar(
                                fieldName,
                                Base64.encodeBytes(rs.getBytes(i), Base64.DONT_BREAK_LINES)
                            );
                            break;
                        }
                        default: {
                            ctx.setVar(fieldName, rs.getString(i));
                        }
                    }
                } catch (SQLException e) {
                    if (errorAsNull) {
                        ctx.setVar(fieldName, (String)null);
                        ctx.error("Error getting column '" + colName + "'", e);
                    } else {
                        throw new CourierException(
                            "Error getting column '" + colName + "'", e);
                    }
                }
                if (xmlvar != null) {
                    String v = ctx.getVar(fieldName);
                    v = StringHelper.replaceChars(v, "<\"", new String[] {"&lt;", "&quot;"});
                    if (xmlAttributes) {
                        xml.append(' ').append(fieldName).append("=\"").append(v).append('"');
                    } else {
                        xml.append('<').append(fieldName).append('>');
                        xml.append(v);
                        xml.append('<').append('/').append(fieldName).append('>');
                    }
                }
            }
            if (xmlvar != null) {
                if (xmlAttributes) {
                    xml.append("/>");
                } else {
                    if (xmlRecordTag != null) {
                        xml.append("</").append(xmlRecordTag).append('>');
                    }
                }
                ctx.setVar(xmlvar, xml.toString());
            }
        } catch (SQLException e) {
            throw new CourierException(e);
        }
    }
}
