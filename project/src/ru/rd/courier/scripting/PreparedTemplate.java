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
package ru.rd.courier.scripting;

import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringExpression;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.courier.utils.templates.SimplePreparedTemplate;

import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreparedTemplate implements ScriptExpression {
    private static final String c_blanks = " \n\t\r";
    private static final String m_begBrace = "[%";
    private static final String m_endBrace = "]";
    private static final char c_funcMark = '!';
    private static final double c_bufRatio = 1.5;

    private final int m_initBufLength;
    private final ScriptExpression[] m_strProvs;

    private static class Var implements ScriptExpression {
        private String m_varName;

        public Var(final String varName) {
            if (varName == null) throw new IllegalArgumentException("Variable name is null");
            m_varName = varName;
        }

        public String calculate(final Context ctx) throws CourierException {
            return ctx.getVar(m_varName);
        }
    }

    private static class PreparedTemplateExpr implements ScriptExpression {
        private final ScriptExpression m_template;

        public PreparedTemplateExpr(final ScriptExpression template) {
            m_template = template;
        }

        public String calculate(final Context ctx) throws CourierException {
            return m_template.calculate(ctx);
        }
    }

    private static class PosHolder {
        public int m_pos;

        public PosHolder(int pos) {
            m_pos = pos;
        }
    }

    private static class NullFormatter implements ScriptExpression {
        private final ScriptExpression m_exp;

        public NullFormatter(ScriptExpression exp) {
            if (exp == null) throw new NullPointerException();
            m_exp = exp;
        }

        public final String calculate(Context ctx) throws CourierException {
            String v = m_exp.calculate(ctx);
            if (v == null) return "NULL";
            return format(v, ctx);
        }

        protected String format(String str, Context ctx) throws CourierException {
            return str;
        }
    }

    private static class ParameterizedFormatter extends NullFormatter {
        public ParameterizedFormatter(ScriptExpression exp, String[] pars, int reqPars) {
            this(exp);
            checkParsLength(pars, reqPars);
        }

        public ParameterizedFormatter(ScriptExpression exp) {
            super(exp);
        }

        protected final static void checkParsLength(String[] pars, int reqPars) {
            if ((pars.length - 1) < reqPars) {
                throw new IllegalArgumentException(
                    "Formatter requires at least " + reqPars + " parameters");
            }
        }
    }

    private static class StringTypeFormatter extends NullFormatter {
        public StringTypeFormatter(ScriptExpression exp) {
            super(exp);
        }
        protected String format(String str, Context ctx) {
            StringBuffer buf = new StringBuffer();
            StringHelper.escapeSqlString(buf, str);
            return buf.toString();
        }
    }

    private static class BinaryTypeFormatter extends NullFormatter {
        public BinaryTypeFormatter(ScriptExpression exp) {
            super(exp);
        }
        protected String format(String str, Context ctx) {
            return "0x" + str;
        }
    }

    private static class DatetimeTypeFormatter extends NullFormatter {
        public DatetimeTypeFormatter(ScriptExpression exp) {
            super(exp);
        }
        protected String format(String str, Context ctx) {
            return "'" + str + "'";
        }
    }

    private static class DateFormatTypeFormatter extends ParameterizedFormatter {
        private String m_toDateFormat;

        public DateFormatTypeFormatter(ScriptExpression exp, String[] pars) {
            super(exp, pars, 1);
            m_toDateFormat = pars[1];
        }

        protected String format(String str, Context ctx) throws CourierException {
            try {
                DateFormat df = new SimpleDateFormat(m_toDateFormat);
                return df.format(ctx.getDateFormat().parse(str));
            } catch (ParseException e) {
                throw new CourierException(e);
            }
        }
    }

    private static class NumericTypeFormatter extends ParameterizedFormatter {
        DecimalFormat m_df;

        public NumericTypeFormatter(ScriptExpression exp, String[] pars) {
            super(exp, pars, 0);
            DecimalFormatSymbols dfs = new DecimalFormatSymbols();
            if (pars.length > 2) {
                dfs.setDecimalSeparator(pars[2].charAt(0));
            }
            if (pars.length > 3) {
                dfs.setGroupingSeparator(pars[3].charAt(0));
            }
            if (pars.length > 1) {
                m_df = new DecimalFormat(pars[1], dfs);
            }
        }

        protected String format(String str, Context ctx) {
            if (m_df == null) return str;
            return m_df.format(new Double(str));
        }
    }

    private static class ReplaceFormatter extends ParameterizedFormatter {
        private String m_chars;
        private String[] m_to;

        public ReplaceFormatter(ScriptExpression exp, String chars, String[] to) {
            super(exp);
            init(chars, to);
        }

        public ReplaceFormatter(ScriptExpression exp, String[] pars) {
            super(exp, pars, 0);
            StringBuffer charsBuffer = new StringBuffer((pars.length - 1)%2);
            List<String> toList = new LinkedList<String>();
            int i = 1;
            while (i < pars.length) {
                char ch;
                String charStr = pars[i];
                if (charStr.charAt(0) == '\\') {
                    switch (charStr.charAt(1)) {
                        case 'n': ch = '\n'; break;
                        case 'r': ch = '\r'; break;
                        case 't': ch = '\t'; break;
                        default: ch = charStr.charAt(1);
                    }
                } else {
                    ch = charStr.charAt(0);
                }
                charsBuffer.append(ch);
                toList.add(pars[i+1]);
                i += 2;
            }
            init(charsBuffer.toString(), toList.toArray(new String[charsBuffer.length()]));
        }

        private void init(String chars, String[] to) {
            m_chars = chars;
            m_to = to;
        }

        protected String format(String str, Context ctx) {
            return StringHelper.replaceChars(str, m_chars, m_to);
        }
    }

    private static class SurroundFormatter extends ParameterizedFormatter {
        private final String m_prefix;
        private final String m_postfix;

        public SurroundFormatter(ScriptExpression exp, String[] pars) {
            super(exp, pars, 0);
            if (pars.length > 1) m_prefix = pars[1];
            else m_prefix = "";
            if (pars.length > 2) m_postfix = pars[2];
            else m_postfix = "";
        }
        protected String format(String str, Context ctx) {
            return m_prefix + str + m_postfix;
        }
    }

    private static class StringProvider implements ScriptExpression {
        private String m_str;
        public StringProvider(final String str) { m_str = str; }
        public String calculate(final Context ctx) { return m_str; }
    }

    private static class SimpleFunctionProvider implements ScriptExpression {
        private final String m_funcName;
        private static Map s_func = new HashMap();

        public SimpleFunctionProvider(String funcName) { m_funcName = funcName; }
        private interface Function {
            String calculate(Context ctx);
        }

        static {
            s_func.put("now", new Function(){
                public String calculate(Context ctx) {
                    return "'" + ctx.getDateFormat().format(new Date()) + "'";
                }
            });
        }

        public String calculate(Context ctx) throws CourierException {
            Function func = (Function)s_func.get(m_funcName);
            if (func == null) {
                throw new CourierException(
                    "Function '" + m_funcName + "' for template not found"
                );
            }
            return func.calculate(ctx);
        }
    }

    private static class NowProvider implements ScriptExpression {
        String m_format;

        public NowProvider(final String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            p = StringHelper.skipDelims(str, p);
            if (p >= str.length()) throw new CourierException("Unexpected end of template");
            p = str.indexOf(m_endBrace, p);
            if (p < 0) throw new CourierException("No closing brackets for variable");
            String formatStr = str.substring(pos.m_pos, p).trim();
            if (formatStr.length() == 0) {
                m_format = null;
            } else {
                m_format = formatStr;
            }
            pos.m_pos = p + m_endBrace.length();
        }

        public String calculate(Context ctx) throws CourierException {
            final DateFormat df;
            if (m_format == null) {
                df = ctx.getDateFormat();
            } else {
                df = new SimpleDateFormat(m_format);
            }
            return df.format(new Date());
        }
    }

    private static class VarByNameProvider implements ScriptExpression {
        String m_varName;

        public VarByNameProvider(final String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            p = StringHelper.skipDelims(str, p);
            if (p >= str.length()) throw new CourierException("Unexpected end of template");
            p = str.indexOf(m_endBrace, p);
            if (p < 0) throw new CourierException("No closing brackets for variable");
            String varName = str.substring(pos.m_pos, p).trim();
            if (varName.length() == 0) {
                throw new CourierException("There is no variable name");
            } else {
                m_varName = varName;
            }
            pos.m_pos = p + m_endBrace.length();
        }

        public String calculate(Context ctx) throws CourierException {
            return ctx.getVar(ctx.getVar(m_varName));
        }
    }

    private static class VarProvider implements ScriptExpression {
        private final String m_name;

        public VarProvider(final String str, PosHolder pos) throws CourierException {
            int p = str.indexOf(m_endBrace, pos.m_pos);
            if (p < 0) throw new CourierException("No closing brackets for variable");
            m_name = str.substring(pos.m_pos, p).trim();
            pos.m_pos = p + m_endBrace.length();
        }

        public String calculate(final Context ctx) throws CourierException {
            return ctx.getVar(m_name);
        }
    }

    private static class SysVarProvider implements ScriptExpression {
        private final String m_name;

        public SysVarProvider(final String str, PosHolder pos) throws CourierException {
            int p = str.indexOf(m_endBrace, pos.m_pos);
            if (p < 0) throw new CourierException("No closing brackets for variable");
            m_name = str.substring(pos.m_pos, p).trim();
            pos.m_pos = p + m_endBrace.length();
        }

        public String calculate(final Context ctx) throws CourierException {
            return System.getProperty(m_name);
        }
    }

    private static class EnvVarProvider implements ScriptExpression {
        private final String m_name;

        public EnvVarProvider(final String str, PosHolder pos) throws CourierException {
            int p = str.indexOf(m_endBrace, pos.m_pos);
            if (p < 0) throw new CourierException("No closing brackets for variable");
            m_name = str.substring(pos.m_pos, p).trim();
            pos.m_pos = p + m_endBrace.length();
        }

        public String calculate(final Context ctx) throws CourierException {
            return System.getenv(m_name);
        }
    }

    private static class MapProvider implements ScriptExpression {
        private String m_varName;
        private final Map m_map = new HashMap();
        private ScriptExpression m_undef = null;
        private ScriptExpression m_null = null;
        private ScriptExpression m_else = null;
        private static final String c_blanks = " \n\t\r";
        private static final String c_elseKeyword = "ELSE";
        private static final String c_nullKeyword = "NULL";
        private static final String c_undefKeyword = "UNDEFINED";

        public MapProvider(String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            int np;
            if (str.charAt(p) == '\'') {
                np = StringHelper.findEndOfBracketedString(str, '\'', p + 1);
                m_varName = str.substring(p + 1, np - 1);
            } else {
                np = StringHelper.findChars(str, p, c_blanks);
                m_varName = str.substring(p, np);
            }
            p = np;
            String caseStr;
            while(true) {
                np = StringHelper.skipChars(str, p, c_blanks);
                if (np >= str.length()) throw new CourierException("Unexpected end of string");
                p = np;

                if (str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                    p += m_endBrace.length();
                    break;
                }

                boolean bKeyword;
                if (str.charAt(p) == '\'') {
                    bKeyword = false;
                    np = StringHelper.findEndOfBracketedString(str, '\'', p + 1);
                    caseStr = str.substring(p + 1, np - 1);
                } else {
                    bKeyword = true;
                    np = StringHelper.findChars(str, p, c_blanks);
                    if (np >= str.length()) throw new CourierException("Unexpected end of string");
                    caseStr = str.substring(p, np).toUpperCase();
                }

                p = np;
                p = StringHelper.skipChars(str, p, c_blanks);
                if (p >= str.length()) {
                    throw new CourierException(
                        "No expression for '" + caseStr + "' in the map"
                    );
                }
                ScriptExpression se;
                if (str.charAt(p) == '\'') {
                    pos.m_pos = p + 1;
                    se = new PreparedTemplateExpr(
                        new PreparedTemplate(str, pos, true, '\'')
                    );
                    np = pos.m_pos;
                } else {
                    np = StringHelper.findChars(str, p, c_blanks);
                    se = new Var(str.substring(p, np));
                }
                p = np;

                if (bKeyword) {
                    if (caseStr.equals(c_elseKeyword)) {
                        m_else = se;
                    } else if (caseStr.equals(c_nullKeyword)) {
                        m_null = se;
                    } else if (caseStr.equals(c_undefKeyword)) {
                        m_undef = se;
                    } else {
                        throw new CourierException("Unknown map keyword '" + caseStr + "'");
                    }
                } else {
                    m_map.put(caseStr, se);
                }
            }
            pos.m_pos = p;
        }

        public String calculate(final Context ctx) throws CourierException {
            if (m_undef != null) {
                if (!ctx.hasVar(m_varName)) return m_undef.calculate(ctx);
            }

            String v = ctx.getVar(m_varName);
            if (v == null) {
                if (m_null != null) return m_null.calculate(ctx);
            } else {
                ScriptExpression se = (ScriptExpression)m_map.get(v);
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

    private static class SubstrProvider implements ScriptExpression {
        private final int m_fromIndex, m_toIndex;
        private ScriptExpression m_tmpl = null;

        public SubstrProvider(String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            int np;
            np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            if (str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                throw new CourierException("Unexpected end of substr");
            }

            np = StringHelper.findChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            String numStr = str.substring(p, np).toUpperCase();
            m_fromIndex = Integer.parseInt(numStr);
            p = np;
            p = StringHelper.skipChars(str, p, c_blanks);
            if (p >= str.length()) throw new CourierException("Unexpected end of string");
            if (str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                throw new CourierException("Unexpected end of substr");
            }

            np = StringHelper.findChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            numStr = str.substring(p, np).toUpperCase();
            m_toIndex = Integer.parseInt(numStr);
            p = np;
            p = StringHelper.skipChars(str, p, c_blanks);
            if (p >= str.length()) throw new CourierException("Unexpected end of string");
            if (str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                throw new CourierException("Unexpected end of substr");
            }

            if (str.charAt(p) == '\'') {
                pos.m_pos = p + 1;
                m_tmpl = new PreparedTemplateExpr(
                    new PreparedTemplate(str, pos, true, '\'')
                );
                np = pos.m_pos;
                if (!str.substring(np, np + m_endBrace.length()).equals(m_endBrace)) {
                    throw new CourierException("End of var expected");
                }
            } else {
                np = str.indexOf(m_endBrace, p);
                if (np < 0) throw new CourierException("No closing brackets for variable");
                m_tmpl = new Var(str.substring(p, np).trim());
            }
            p = np;

            p += m_endBrace.length();

            pos.m_pos = p;
        }

        public String calculate(final Context ctx) throws CourierException {
            String str = m_tmpl.calculate(ctx);
            return str.substring(
                m_fromIndex, m_toIndex >= 0 ? m_toIndex : str.length()
            );
        }
    }

    private static class ReplaceProvider implements ScriptExpression {
        private final ScriptExpression m_tmpl;
        private final ScriptExpression m_reqexp;
        private final ScriptExpression m_replacement;

        public ReplaceProvider(String str, PosHolder pos) throws CourierException {
            m_tmpl = parseWordOrBracketedString(str, pos);
            m_reqexp = parseWordOrBracketedString(str, pos);
            m_replacement = parseWordOrBracketedString(str, pos);

            int p = pos.m_pos;
            int np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            if (!str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                throw new CourierException("End of var expected");
            }
            p += m_endBrace.length();
            pos.m_pos = p;
        }

        public String calculate(final Context ctx) throws CourierException {
            String str = m_tmpl.calculate(ctx);
            Pattern reqexp = Pattern.compile(m_reqexp.calculate(ctx));
            return reqexp.matcher(str).replaceAll(m_replacement.calculate(ctx));
        }
    }

    private static class IfExistsProvider implements ScriptExpression {
        private String m_varName;
        private ScriptExpression m_then = null;
        private ScriptExpression m_else = null;
        private static final String c_blanks = " \n\t\r";
        private static final String c_elseKeyword = "ELSE";

        public IfExistsProvider(String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            int np;
            np = StringHelper.findChars(str, p, c_blanks);
            m_varName = str.substring(p, np);
            p = np;
            np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            ScriptExpression se;
            if (str.charAt(p) == '\'') {
                pos.m_pos = p + 1;
                se = new PreparedTemplateExpr(
                    new PreparedTemplate(str, pos, true, '\'')
                );
                np = pos.m_pos;
            } else {
                np = StringHelper.findChars(str, p, c_blanks);
                se = new Var(str.substring(p, np));
            }
            p = np;
            m_then = se;

            np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            if (!str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                np = StringHelper.findChars(str, p, c_blanks);
                if (np >= str.length()) throw new CourierException("Unexpected end of string");
                String caseStr = str.substring(p, np);
                if (!caseStr.equalsIgnoreCase(c_elseKeyword)) {
                    throw new CourierException(
                        "'Else' expected but '" + caseStr + "' found");
                }

                p = np;
                p = StringHelper.skipChars(str, p, c_blanks);
                if (p >= str.length()) {
                    throw new CourierException("No expression for 'ELSE'");
                }
                if (str.charAt(p) == '\'') {
                    pos.m_pos = p + 1;
                    se = new PreparedTemplateExpr(
                        new PreparedTemplate(str, pos, true, '\'')
                    );
                    np = pos.m_pos;
                } else {
                    np = StringHelper.findChars(str, p, c_blanks);
                    se = new Var(str.substring(p, np));
                }
                p = np;
                m_else = se;
            }

            np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            if (!str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                throw new CourierException("End of var expected");
            }
            p += m_endBrace.length();
            pos.m_pos = p;
        }

        public String calculate(final Context ctx) throws CourierException {
            if (ctx.hasVar(m_varName)) {
                return m_then.calculate(ctx);
            } else {
                if (m_else != null) {
                    return m_else.calculate(ctx);
                } else {
                    return "";
                }
            }
        }
    }

    private static class IfValueProvider implements ScriptExpression {
        private String m_varName;
        private ScriptExpression m_then = null;
        private ScriptExpression m_else = null;
        private static final String c_elseKeyword = "ELSE";

        public IfValueProvider(String str, PosHolder pos) throws CourierException {
            int p = pos.m_pos;
            int np;
            np = StringHelper.findChars(str, p, c_blanks);
            m_varName = str.substring(p, np);
            p = np;
            np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            ScriptExpression se;
            if (str.charAt(p) == '\'') {
                pos.m_pos = p + 1;
                se = new PreparedTemplateExpr(
                    new PreparedTemplate(str, pos, true, '\'')
                );
                np = pos.m_pos;
            } else {
                np = StringHelper.findChars(str, p, c_blanks);
                se = new Var(str.substring(p, np));
            }
            p = np;
            m_then = se;

            np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            if (!str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                np = StringHelper.findChars(str, p, c_blanks);
                if (np >= str.length()) throw new CourierException("Unexpected end of string");
                String caseStr = str.substring(p, np);
                if (!caseStr.equalsIgnoreCase(c_elseKeyword)) {
                    throw new CourierException(
                        "'Else' expected but '" + caseStr + "' found");
                }

                p = np;
                p = StringHelper.skipChars(str, p, c_blanks);
                if (p >= str.length()) {
                    throw new CourierException("No expression for 'ELSE'");
                }
                if (str.charAt(p) == '\'') {
                    pos.m_pos = p + 1;
                    se = new PreparedTemplateExpr(
                        new PreparedTemplate(str, pos, true, '\'')
                    );
                    np = pos.m_pos;
                } else {
                    np = StringHelper.findChars(str, p, c_blanks);
                    se = new Var(str.substring(p, np));
                }
                p = np;
                m_else = se;
            }

            np = StringHelper.skipChars(str, p, c_blanks);
            if (np >= str.length()) throw new CourierException("Unexpected end of string");
            p = np;

            if (!str.substring(p, p + m_endBrace.length()).equals(m_endBrace)) {
                throw new CourierException("End of var expected");
            }
            p += m_endBrace.length();
            pos.m_pos = p;
        }

        public String calculate(final Context ctx) throws CourierException {
            boolean bValue;
            if (ctx.hasVar(m_varName)) {
                String v = ctx.getVar(m_varName);
                bValue = (v != null) && (v.length() > 0);
            } else {
                bValue = false;
            }

            if (bValue) {
                return m_then.calculate(ctx);
            } else {
                if (m_else != null) {
                    return m_else.calculate(ctx);
                } else {
                    return "";
                }
            }
        }
    }

    private static ScriptExpression parseWordOrBracketedString(String str, PosHolder pos) throws CourierException {
        ScriptExpression se;
        int p = pos.m_pos;
        p = StringHelper.skipChars(str, p, c_blanks);
        if (p >= str.length()) {
            throw new CourierException("No expression found at pos " + pos.m_pos);
        }
        if (str.charAt(p) == '\'') {
            pos.m_pos = p + 1;
            se = new PreparedTemplateExpr(
                new PreparedTemplate(str, pos, true, '\'')
            );
        } else {
            int np = StringHelper.findChars(str, p, c_blanks);
            se = new Var(str.substring(p, np));
            pos.m_pos = np;
        }
        return se;
    }

    public PreparedTemplate(final String str) throws CourierException {
        this(
            str, new PosHolder(0),
            false, // quoted
            ' '   // whatever char you want because quoted is false
        );
    }

    private interface FuncProvider {
        ScriptExpression createFunc(String str, PosHolder pos) throws CourierException;
    }

    private static final Map<String, FuncProvider> m_funcProviders = new HashMap<String, FuncProvider>();
    private static final String c_varFunc = "var";
    static {
        m_funcProviders.put(c_varFunc, new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new VarProvider(str, pos);
            }
        });
        m_funcProviders.put("sys-var", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new SysVarProvider(str, pos);
            }
        });
        m_funcProviders.put("env-var", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new EnvVarProvider(str, pos);
            }
        });
        m_funcProviders.put("map", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new MapProvider(str, pos);
            }
        });
        m_funcProviders.put("if-var", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new IfExistsProvider(str, pos);
            }
        });
        m_funcProviders.put("if-value", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new IfValueProvider(str, pos);
            }
        });
        m_funcProviders.put("now", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new NowProvider(str, pos);
            }
        });
        m_funcProviders.put("var-by-name", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new VarByNameProvider(str, pos);
            }
        });
        m_funcProviders.put("substr", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new SubstrProvider(str, pos);
            }
        });
        m_funcProviders.put("replace", new FuncProvider() {
            public ScriptExpression createFunc(String str, PosHolder pos) throws CourierException {
                return new ReplaceProvider(str, pos);
            }
        });
    }

    private PreparedTemplate(
        final String str, PosHolder pos,
        boolean quoted, char bracket
    ) throws CourierException {
        m_initBufLength = (int)(c_bufRatio * str.length());
        final List<ScriptExpression> provs = new LinkedList<ScriptExpression>();
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
            StringHelper.StringsResult sr = null;
            if (str.charAt(p) == '(') {
                p++;
                sr = StringHelper.splitString(
                    str, p, " ", '\'', -1, ")"
                );
                np = sr.m_pos;
                if (np >= str.length()) {
                    throw new CourierException(
                        "No closing bracket from pos " + p + " for variable type in template: " +
                         StringHelper.showErrorPos(str, p, "--><--")
                    );
                }
                if (sr.m_strings.length == 0) {
                    throw new CourierException("Empty template format");
                }
                varType = sr.m_strings[0];
                p = np + 1;
                StringHelper.skipChars(str, p, c_blanks);
                if (p >= str.length()) {
                    throw new CourierException("Expression after variable type is empty");
                }
            }

            ScriptExpression part = null;
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
            func = func.toLowerCase();
            if (m_funcProviders.containsKey(func)) {
                part = m_funcProviders.get(func).createFunc(str, pos);
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
                } else if (varType.equals("date-format")) {
                    part = new DateFormatTypeFormatter(part, sr.m_strings);
                } else if (varType.equals("number")) {
                    part = new NumericTypeFormatter(part, sr.m_strings);
                } else if (varType.equals("binary")) {
                    part = new BinaryTypeFormatter(part);
                } else if (varType.equals("replace")) {
                    part = new ReplaceFormatter(part, sr.m_strings);
                } else if (varType.equals("cdata")) {
                    part = new ReplaceFormatter(
                        part, "\"<>&",
                        new String[] {"&quot;", "&lt;", "&gt;", "&amp;"}
                    );
                } else if (varType.equals("cdata2")) {
                    part = new ReplaceFormatter(
                        part, "\"<>&",
                        new String[] {"&amp;&quot;", "&amp;lt;", "&amp;gt;", "&amp;amp;"}
                    );
                } else if (varType.equals("surround")) {
                    part = new SurroundFormatter(part, sr.m_strings);
                } else {
                    throw new CourierException(
                        "Unknown variable type '" + varType + "'"
                    );
                }
            } else {
                part = new NullFormatter(part);
            }
            provs.add(part);
        }
        m_strProvs = (ScriptExpression[])provs.toArray(new ScriptExpression[provs.size()]);
        pos.m_pos = p;
    }

    public String calculate(final Context ctx) throws CourierException {
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