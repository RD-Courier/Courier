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
package ru.rd.courier.utils.templates;

import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringContext;
import ru.rd.courier.utils.StringExpression;
import ru.rd.courier.utils.StringHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.*;

public class SimplePreparedTemplate implements StringExpression {
    private static final String c_blanks = " \n\t\r";
    private static final String m_begBrace = "[%";
    private static final String m_endBrace = "]";

    private static final char c_funcMark = '!';
    private static final String c_varFunc = "var";
    private static final String c_mapFunc = "map";
    private static final String c_nowFunc = "now";
    private static final double c_bufRatio = 1.5;

    private static final String c_databaseDateFormat = "yyyyMMdd hh:mm:ss";

    private final int m_initBufLength;
    private final ru.rd.courier.utils.StringExpression[] m_strProvs;

    private static class Var implements StringExpression {
        private String m_varName;

        public Var(final String varName) {
            if (varName == null) throw new IllegalArgumentException("Variable name is null");
            m_varName = varName;
        }

        public String calculate(final StringContext ctx) throws CourierException {
            return ctx.getVar(m_varName);
        }
    }

    private static class PreparedTemplateExpr implements StringExpression {
        private final StringExpression m_template;

        public PreparedTemplateExpr(final StringExpression template) {
            m_template = template;
        }

        public String calculate(final StringContext ctx) throws CourierException {
            return m_template.calculate(ctx);
        }
    }


    private static class PosHolder {
        public int m_pos;

        public PosHolder(int pos) {
            m_pos = pos;
        }
    }

    private static class GeneralTypeFormatter implements StringExpression {
        private final StringExpression m_exp;

        public GeneralTypeFormatter(StringExpression exp) {
            if (exp == null) throw new NullPointerException();
            m_exp = exp;
        }

        public final String calculate(StringContext ctx) throws CourierException {
            String v = m_exp.calculate(ctx);
            if (v == null) return "NULL";
            return format(v);
        }

        protected String format(String str) throws CourierException { return str; }
    }

    private static class StringTypeFormatter extends GeneralTypeFormatter {
        public StringTypeFormatter(StringExpression exp) {
            super(exp);
        }
        protected String format(String str) {
            StringBuffer buf = new StringBuffer();
            StringHelper.escapeSqlString(buf, str);
            return buf.toString();
        }
    }

    private static class DatetimeTypeFormatter extends GeneralTypeFormatter {
        public DatetimeTypeFormatter(StringExpression exp) {
            super(exp);
        }
        protected String format(String str) {
            return "CONVERT(DATETIME, '" + str + "')";
        }
    }

    private static class NumericTypeFormatter extends GeneralTypeFormatter {
        DecimalFormat m_df;

        public NumericTypeFormatter(StringExpression exp, String pattern) {
            super(exp);
            m_df = new DecimalFormat(pattern);
        }

        protected String format(String str) {
            return m_df.format(new Double(str).doubleValue());
        }
    }

    private static class StringProvider implements StringExpression {
        private String m_str;
        public StringProvider(final String str) { m_str = str; }
        public String calculate(final StringContext ctx) { return m_str; }
    }

    private static class SimpleFunctionProvider implements StringExpression {
        private final String m_funcName;
        private static Map s_func = new HashMap();

        public SimpleFunctionProvider(String funcName) { m_funcName = funcName; }
        private interface Function {
            String calculate(StringContext ctx);
        }

        static {
            s_func.put("now", new Function(){
                private final DateFormat s_df = new SimpleDateFormat(c_databaseDateFormat);
                public String calculate(StringContext ctx) {
                    return "'" + s_df.format(new Date()) + "'";
                }
            });
        }

        public String calculate(StringContext ctx) throws CourierException {
            Function func = (Function)s_func.get(m_funcName);
            if (func == null) {
                throw new CourierException(
                    "Function '" + m_funcName + "' for template not found"
                );
            }
            return func.calculate(ctx);
        }
    }

    private static class NowProvider implements StringExpression {
        DateFormat m_format;

        public NowProvider(final String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            p = StringHelper.skipDelims(str, p);
            if (p >= str.length()) throw new CourierException("Unexpected end of template");
            p = str.indexOf(SimplePreparedTemplate.m_endBrace, p);
            if (p < 0) throw new CourierException("No closing brackets for variable");
            String formatStr = str.substring(pos.m_pos, p).trim();
            if (formatStr.length() == 0) {
                m_format = DateFormat.getDateInstance();
            } else {
                m_format = new SimpleDateFormat(formatStr);
            }
            pos.m_pos = p + SimplePreparedTemplate.m_endBrace.length();
        }

        public String calculate(StringContext ctx) throws CourierException {
            return m_format.format(new Date());
        }
    }

    private static String[] getFuncPars(String str, PosHolder pos, int requiredParsCount) {
        StringHelper.StringsResult sr = StringHelper.splitString(
            str, pos.m_pos, " ", '\'', -1, SimplePreparedTemplate.m_endBrace
        );
        int p = sr.m_pos;
        if (p >= str.length()) {
            throw new RuntimeException(
                "Function at pos " + pos.m_pos + ": unexpected end of template");
        }
        if (sr.m_strings.length != requiredParsCount) {
            throw new RuntimeException(
                "Function at pos " + pos.m_pos + " requires " + requiredParsCount + " parameters");
        }
        pos.m_pos = p + SimplePreparedTemplate.m_endBrace.length();
        return sr.m_strings;
    }

    private static class ReplaceProvider implements StringExpression {
        private String m_varName, m_from, m_to;

        public ReplaceProvider(String str, PosHolder pos) {
            String[] pars = getFuncPars(str, pos, 3);
            m_varName = pars[0];
            m_from = pars[1];
            m_to = pars[2];
        }

        public String calculate(StringContext ctx) throws CourierException {
            return StringHelper.replace(ctx.getVar(m_varName), m_from, m_to);
        }
    }

    private static class VarProvider implements ru.rd.courier.utils.StringExpression {
        private final String m_name;

        public VarProvider(final String str, PosHolder pos) throws CourierException {
            int p = str.indexOf(SimplePreparedTemplate.m_endBrace, pos.m_pos);
            if (p < 0) throw new CourierException("No closing brackets for variable");
            m_name = str.substring(pos.m_pos, p).trim();
            pos.m_pos = p + 1;
        }

        public String calculate(final StringContext ctx) throws CourierException {
            return ctx.getVar(m_name);
        }
    }

    private static class MapProvider implements StringExpression {
        private String m_varName;
        private final Map m_map = new HashMap();
        private StringExpression m_null = null;
        private StringExpression m_else = null;
        private static final String c_blanks = " \n\t\r";
        private static final String c_elseKeyword = "ELSE";
        private static final String c_nullKeyword = "NULL";

        public MapProvider(String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            int np;
            np = StringHelper.findChars(str, p, c_blanks);
            m_varName = str.substring(p, np);
            p = np;
            String caseStr;
            while(true) {
                np = StringHelper.skipChars(str, p, c_blanks);
                if (np >= str.length()) throw new CourierException("Unexpected end of string");
                p = np;

                if (str.substring(p, p + SimplePreparedTemplate.m_endBrace.length()).equals(SimplePreparedTemplate.m_endBrace)) {
                    p += SimplePreparedTemplate.m_endBrace.length();
                    break;
                }

                boolean bElse = false;
                if (str.charAt(p) == '\'') {

                    np = StringHelper.findEndOfBracketedString(str, '\'', p + 1);
                    caseStr = str.substring(p + 1, np - 1);
                } else {
                    np = StringHelper.findChars(str, p, c_blanks);
                    if (np >= str.length()) throw new CourierException("Unexpected end of string");
                    caseStr = str.substring(p, np);
                    if (caseStr.equalsIgnoreCase(c_elseKeyword)) {
                        bElse = true;
                    } else if (caseStr.equalsIgnoreCase(c_nullKeyword)) {
                        caseStr = null;
                    } else {
                        throw new CourierException("Unknown map keyword '" + caseStr + "'");
                    }
                }

                p = np;
                p = StringHelper.skipChars(str, p, c_blanks);
                if (p >= str.length()) {
                    throw new CourierException(
                        "No expression for '" + (bElse ? c_elseKeyword : caseStr) + "' in the map"
                    );
                }
                ru.rd.courier.utils.StringExpression se;
                if (str.charAt(p) == '\'') {
                    pos.m_pos = p + 1;
                    se = new PreparedTemplateExpr(
                        new SimplePreparedTemplate(str, pos, true, '\'')
                    );
                    np = pos.m_pos;
                } else {
                    np = StringHelper.findChars(str, p, c_blanks);
                    se = new Var(str.substring(p, np));
                }
                p = np;
                if (bElse) {
                    m_else = se;
                } else if (caseStr == null) {
                    m_null = se;
                } else {
                    m_map.put(caseStr, se);
                }
            }
            pos.m_pos = p;
        }

        public String calculate(final StringContext ctx) throws CourierException {
            String v = ctx.getVar(m_varName);
            if (v == null) {
                if (m_null != null) return m_null.calculate(ctx);
            } else {
                ru.rd.courier.utils.StringExpression se = (ru.rd.courier.utils.StringExpression)m_map.get(v);
                if (se != null) return se.calculate(ctx);
            }
            if (m_else != null) {
                return m_else.calculate(ctx);
            } else {
                throw new CourierException(
                    "Expression for value '" + v + "' of var '" + m_varName + "' not specified"
                );
            }
        }
    }

    public SimplePreparedTemplate(final String str) throws CourierException {
        this(
            str, new PosHolder(0),
            false, // quoted
            ' '   // whatever char you want because quoted is false
        );
    }

    private SimplePreparedTemplate(
        final String str, PosHolder pos,
        boolean quoted, char bracket
    ) throws CourierException {
        m_initBufLength = (int)(c_bufRatio * str.length());
        final List provs = new LinkedList();
        int p = pos.m_pos;
        int np;
        while (true) {
            np = str.indexOf(m_begBrace, p);
            // process end of quoted template
            if (quoted) {
                int ne = StringHelper.findEndOfBracketedString(str, bracket, p);
                if ((ne >= 0) && ((np < 0) || (ne <= np))) {
                    provs.add(new StringProvider(
                        StringHelper.replaceBracket(str.substring(p, ne - 1), bracket)
                    ));
                    p = ne;
                    break;
                }
            }
            // process begin bracket escaping
            if ((np > 0) && (str.charAt(np - 1) == m_begBrace.charAt(0))) {
                provs.add(new StringProvider(
                    StringHelper.replaceBracket(str.substring(p, np - 1), bracket) + m_begBrace
                ));
                p = np + m_begBrace.length();
                continue;
            }
            // process - no begin bracket found - that is end of string
            if (np < 0) {
                provs.add(new StringProvider(
                    StringHelper.replaceBracket(str.substring(p), bracket)
                ));
                p = str.length();
                break;
            }
            // begin bracket found (end of quoted template processed above)
            if (np > p) {
                provs.add(new StringProvider(
                    StringHelper.replaceBracket(str.substring(p, np), bracket)
                ));
            }
            p = np + m_begBrace.length();

            String varType = null;
            StringHelper.StringsResult formatData = null;
            if (str.charAt(p) == '(') {
                p++;
                formatData = StringHelper.splitString(
                    str, p, " ", '\'', -1, ")"
                );
                np = formatData.m_pos;
                if (np >= str.length()) {
                    throw new CourierException(
                        "No closing bracket from pos " + p + " for variable type in template: " + str
                    );
                }
                if (formatData.m_strings.length == 0) {
                    throw new CourierException("Empty template format");
                }
                varType = formatData.m_strings[0];
                p = np + 1;
                StringHelper.skipChars(str, p, c_blanks);
                if (p >= str.length()) {
                    throw new CourierException("Expression after variable type is empty");
                }
            }

            StringExpression part = null;
            String func = c_varFunc;

            if (str.charAt(p) == c_funcMark) {
                np = StringHelper.findChars(str, p + 1, c_blanks + m_endBrace);
                if (np >= str.length()) {
                    throw new CourierException("End of string after function name");
                }
                func = str.substring(p + 1, np);
                np = StringHelper.skipChars(str, np, c_blanks);
                if (np >= str.length()) {
                    throw new CourierException("Expression after variable type is empty");
                }
                p = np;
            }

            pos.m_pos = p;
            if (func.equals(c_varFunc)) {
                part = new VarProvider(str, pos);
            } else if (func.equals(c_mapFunc)) {
                part = new MapProvider(str, pos);
            } else if (func.equals(c_nowFunc)) {
                part = new NowProvider(str, pos);
            } else if (func.equals("replace")) {
                part = new ReplaceProvider(str, pos);
            } else {
                part = new SimpleFunctionProvider(func);
                pos.m_pos++;
            }

            p = pos.m_pos;

            if (varType != null) {
                if (varType.equals("string")) {
                    part = new StringTypeFormatter(part);
                } else if (varType.equals("datetime")) {
                    part = new DatetimeTypeFormatter(part);
                } else if (varType.equals("number")) {
                    part = new NumericTypeFormatter(part, formatData.m_strings[1]);
                } else {
                    throw new CourierException(
                        "Unknown variable type '" + varType + "'"
                    );
                }
            } else {
                part = new GeneralTypeFormatter(part);
            }
            provs.add(part);
        }
        m_strProvs = (ru.rd.courier.utils.StringExpression[])provs.toArray(new ru.rd.courier.utils.StringExpression[provs.size()]);
        pos.m_pos = p;
    }

    public String calculate(final StringContext ctx) throws CourierException {
        final StringBuffer ret = new StringBuffer(m_initBufLength);
        String str;
        for(int i = 0; i < m_strProvs.length; i++) {
            str = m_strProvs[i].calculate(ctx);
            if (str  == null) return null;
            ret.append(str);
        }
        return ret.toString();
    }
}