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

import java.util.LinkedList;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 22.07.2005
 * Time: 10:35:26
 */
public class GlTradeLineParser extends CsvLineSplitterInfo implements LineSplitter {
    public GlTradeLineParser(
        char stringBracket, boolean useBracket,
        char separator, boolean needToTrim, String nullWord
    ) {
        super(stringBracket, useBracket, separator, needToTrim, nullWord);
    }

    public GlTradeLineParser(CsvLineSplitterInfo info) {
        super(info);
    }

    public String[] parse(StringBuffer text) {
        StringBufferSimpleParser p = new StringBufferSimpleParser();
        p.setText(text);
        List<String> res = new LinkedList<String>();
        while (true) {
            if (p.beyondEnd()) break;
            String str;
            if (m_useBracket && p.thisCharThenMove(m_stringBracket)) {
                StringBuffer sb = new StringBuffer();
                while (!p.beyondEnd()) {
                    sb.append(p.weakShiftBeyondChar(m_stringBracket));
                    if (p.beyondEnd() || p.thisChar(m_separator)) break;
                    sb.append(m_stringBracket);
                }
                str = sb.toString();
            } else {
                str = p.weakShiftToChar(m_separator);
            }
            if (m_needToTrim) str = str.trim();
            if ((m_nullWord != null) && m_nullWord.equals(str)) str = null;
            res.add(str);

            if(!p.beyondEnd()) {
                if (p.thisCharThenMove(m_separator)) {
                    if(p.beyondEnd()) res.add("");
                } else {
                    p.raiseError("Separator '" + m_separator + "' expected");
                }
            }
        }
        return res.toArray(new String[res.size()]);
    }
}
