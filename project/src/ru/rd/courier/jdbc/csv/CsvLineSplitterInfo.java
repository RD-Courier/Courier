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
package ru.rd.courier.jdbc.csv;

import ru.rd.courier.utils.StringBufferSimpleParser;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringHelper;
import org.w3c.dom.Node;

import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 11.08.2006
 * Time: 11:26:37
 */
public class CsvLineSplitterInfo {
    protected final char m_stringBracket;
    protected final boolean m_useBracket;
    protected final char m_separator;
    protected final boolean m_needToTrim;
    protected final String m_nullWord;

    public CsvLineSplitterInfo(
        char stringBracket, boolean useBracket, char separator,
        boolean needToTrim, String nullWord
    ) {
        m_stringBracket = stringBracket;
        m_useBracket = useBracket;
        m_separator = separator;
        m_needToTrim = needToTrim;
        m_nullWord = nullWord;
    }

    public CsvLineSplitterInfo(CsvLineSplitterInfo info) {
        m_stringBracket = info.m_stringBracket;
        m_useBracket = info.m_useBracket;
        m_separator = info.m_separator;
        m_needToTrim = info.m_needToTrim;
        m_nullWord = info.m_nullWord;
    }

    public CsvLineSplitterInfo(Node conf) {
        this(
            DomHelper.getNodeAttr(conf, "bracket", "'").charAt(0),
            DomHelper.hasAttr(conf, "bracket"),
            DomHelper.getNodeAttr(conf, "separator", ",").charAt(0),
            DomHelper.getBoolYesNo(conf, "trim", false),
            DomHelper.getNodeAttr(conf, "null-word", null)
        );
    }

    public CsvLineSplitterInfo(Properties conf) {
        this(
            StringHelper.stringParam(conf, "StringBracket", "'").charAt(0),
            conf.containsKey("StringBracket"),
            StringHelper.stringParam(conf, "Separator", ",").charAt(0),
            StringHelper.boolParamTF(conf, "NeedToTrim", false),
            StringHelper.stringParam(conf, "NullWord", null)
        );
    }

    public char getBracket() {
        return m_stringBracket;
    }

    public boolean getUseBracket() {
        return m_useBracket;
    }

    public boolean getNeedToTrim() {
        return m_needToTrim;
    }

    public char getSeparator() {
        return m_separator;
    }

    public String getNullWord() {
        return m_nullWord;
    }
}
