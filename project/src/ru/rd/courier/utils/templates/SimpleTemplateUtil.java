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

import java.util.Map;

public class SimpleTemplateUtil {
    private final String m_begBrace;
    private final String m_endBrace;
    private static final String c_begBrace = "[%";
    private static final String c_endBrace = "]";

    public SimpleTemplateUtil(String begBrace, String endBrace) {
        m_begBrace = begBrace;
        m_endBrace = endBrace;
    }

    public SimpleTemplateUtil() {
        m_begBrace = c_begBrace;
        m_endBrace = c_endBrace;
    }

    public String process(final String str, final Map context) throws CourierException {
        final StringBuffer ret = new StringBuffer();
        int bpos = str.indexOf(m_begBrace);
        int epos = -1;
        while (bpos >= 0) {
            ret.append(str.substring(
                epos < 0 ? 0 : epos + m_endBrace.length(), bpos
            ));
            epos = str.indexOf(m_endBrace, bpos + m_begBrace.length());
            if (epos >= 0) {
                final String varName = str.substring(bpos + m_begBrace.length(), epos);
                ret.append(process(context.get(varName).toString(), context));
            } else {
                throw new CourierException("There is no closing braces in template");
            }
            bpos = str.indexOf(m_begBrace, epos + m_endBrace.length());
        }
        ret.append(str.substring(
            epos < 0 ? 0 : epos + m_endBrace.length(), str.length()
        ));
        return ret.toString();
    }
}
