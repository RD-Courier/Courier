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
package ru.rd.courier.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 07.04.2005
 * Time: 12:37:49
 */
public class SectionsParser {
    private StringSimpleParser m_parser;
    private String m_sectionName;
    private Map<String, StringBuffer> m_bufs;

    static final char c_sectionBegin = '^';
    static final String c_sectionDelimiter = "\n" + c_sectionBegin;
    private static final char c_escStr = '^';
    private static final String c_sectionDelimiterEsc = c_sectionDelimiter + c_escStr;
    static final String c_defaultSectionName = "default";

    public SectionsParser() {
        m_parser = new StringSimpleParser();
    }

    public static String escape(String str) {
        return StringHelper.replace(str, c_sectionDelimiter, c_sectionDelimiterEsc);
    }

    public static String unescape(String str) {
        return StringHelper.replace(str, c_sectionDelimiterEsc, c_sectionDelimiter);
    }

    private void append(String sectionName, String content) {
        if (!m_bufs.containsKey(sectionName)) m_bufs.put(sectionName, new StringBuffer());
        m_bufs.get(sectionName).append(content);
    }

    private void readSectionContent() {
        append(m_sectionName, m_parser.weakShiftBeyondStr(c_sectionDelimiter));
        while (m_parser.thisChar(c_escStr)) {
            append(m_sectionName, "\n");
            append(m_sectionName, m_parser.weakShiftBeyondStr(c_sectionDelimiter));
        }
    }

    public Map<String, String> parse(String text) {
        m_bufs = new HashMap<String, StringBuffer>();
        m_parser.setText(text);
        if (!m_parser.thisCharThenMove(c_sectionBegin)) {
            m_sectionName = c_defaultSectionName;
            readSectionContent();
        }
        while (!m_parser.beyondEnd()) {
            m_sectionName = m_parser.weakShiftBeyondChar('\n').trim();
            readSectionContent();
        }

        return DomHelper.buffersToStrings(m_bufs);
    }
}
