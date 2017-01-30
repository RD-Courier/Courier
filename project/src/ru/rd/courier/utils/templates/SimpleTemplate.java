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
import ru.rd.courier.utils.StringHelper;

public class SimpleTemplate implements Template {
    private final String m_begBrace;
    private final String m_endBrace;

    public SimpleTemplate(String begBrace, String endBrace) {
        m_begBrace = begBrace;
        m_endBrace = endBrace;
    }

    public SimpleTemplate() {
        this("[%", "]");
    }

    public String process(final String str, final StringContext ctx) throws CourierException {
        final StringBuffer ret = new StringBuffer();
        int bpos = str.indexOf(m_begBrace);
        int epos = -1;
        while (bpos >= 0) {
            if ((str.length() > bpos) && (str.charAt(bpos + 1) == m_begBrace.charAt(0))) {
                ret.append(str.substring(epos < 0 ? 0 : epos, bpos + 1));
                epos = bpos + 2;
            } else {
                ret.append(str.substring(epos < 0 ? 0 : epos, bpos));
                bpos += m_begBrace.length();
                epos = str.indexOf(m_endBrace, bpos);
                if (epos >= 0) {
                    final String instr = str.substring(bpos, epos);
                    epos += m_endBrace.length();
                    ret.append(getVar(ctx, instr));
                } else {
                    throw new CourierException(
                        "No closing bracket for variable in template");
                }
            }
            bpos = str.indexOf(m_begBrace, epos);
        }
        ret.append(str.substring(epos < 0 ? 0 : epos, str.length()));
        return ret.toString();
    }

    public String escape(String str) {
        return StringHelper.replace(str, m_begBrace, m_begBrace.charAt(0) + m_begBrace);
    }

    private String getVar(final StringContext ctx, final String instr) throws CourierException {
        final String ret;
        String varName = null;
        String cmd = null;
        if ((instr.length() > 0) && (instr.charAt(0) == '(') ) {
            final int pos = instr.indexOf(')', 1);
            if (pos < 0) {
                throw new CourierException(
                    "No closing bracket for command in template");
            }
            cmd = instr.substring(1, pos);
            varName = instr.substring(pos + 1).trim();
        } else {
            varName = instr;
        }
        final String var = ctx.getVar(varName);
        if (var == null) ret = "NULL";
        else {
            if (cmd == null) {
                ret = var;
            } else {
                if (cmd.equals("string")) {
                    ret = StringHelper.escapeSqlStringWithFuncs(var);
                } else if (cmd.equals("datetime")) {
                    ret = "CONVERT(DATETIME, " + StringHelper.escapeSqlStringWithFuncs(var) + ")";
                } else {
                    throw new CourierException("Unknown command: " + cmd);
                }
            }
        }
        return ret;
    }
}
