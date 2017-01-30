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

public class StringBufferSimpleParser extends AbstractSimpleParser {
    protected StringBuffer m_text;

    public void setText(StringBuffer text) {
        m_text = text;
        setPos(0);
    }

    public int length() {
        return m_text.length();
    }

    public char getCharAt(int pos) {
        return m_text.charAt(pos);
    }

    public char getCurChar() {
        return m_text.charAt(m_pos);
    }

    public String substr(int begIndex, int endIndex) {
        return m_text.substring(begIndex, endIndex);
    }

    public String substr(int endIndex) {
        return m_text.substring(m_pos, endIndex);
    }

    public String substrL(int length) {
        return m_text.substring(m_pos, Math.min(m_pos + length, m_text.length()));
    }

    public String backSubstr(int beginIndex) {
        return m_text.substring(beginIndex, m_pos);
    }

    public String backSubstrL(int length) {
        return m_text.substring(Math.max(m_pos - length, 0), m_pos);
    }

    public boolean beyondEnd() {
        return m_pos >= m_text.length();
    }

    public int indexOf(String str, int fromIndex) {
        return m_text.indexOf(str, fromIndex);
    }

    public int indexOf(char ch, int fromIndex) {
        int l = length();
        while (fromIndex < l) {
            if (m_text.charAt(fromIndex) == ch) return fromIndex;
            fromIndex++;
        }
        return -1;
    }
}
