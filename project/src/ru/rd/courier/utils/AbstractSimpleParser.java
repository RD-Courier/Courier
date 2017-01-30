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

import java.util.Properties;
import java.util.List;
import java.util.LinkedList;

public abstract class AbstractSimpleParser {
    private static final String c_blanks = " \n\t\r";
    private String m_blanks;
    protected int m_pos;

    public AbstractSimpleParser(String blanks) {
        m_blanks = blanks;
    }

    public AbstractSimpleParser() {
        this(c_blanks);
    }

    public void setBlanks(String blanks) {
        m_blanks = blanks;
    }

    public void setPos(int pos) {
        m_pos = pos;
    }

    public void incPos() {
        m_pos++;
    }

    public int getPos() {
        return m_pos;
    }

    private boolean isBlank(char ch) {
        return m_blanks.indexOf(ch) >= 0;
    }

    public interface CharChecker {
        boolean check(char ch);
    }

    private interface CharCheckerEx {
        int check(char ch);
        int INCLUDE = 0;
        int SKIP = INCLUDE + 1;
        int STOP = SKIP + 1;
    }

    private final CharChecker m_blankCharChecker = new CharChecker() {
        public boolean check(char ch) {
            return isBlank(ch);
        }
    };

    private static final CharChecker c_digitCharChecker = new CharChecker() {
        public boolean check(char ch) {
            return Character.isDigit(ch);
        }
    };

    public abstract int length();
    public abstract char getCurChar();
    public abstract char getCharAt(int pos);
    public abstract String substr(int begIndex, int endIndex);
    public abstract String substr(int endIndex);
    public abstract String substrL(int length);
    public abstract String backSubstr(int beginIndex);
    public abstract String backSubstrL(int length);
    public abstract boolean beyondEnd();
    public abstract int indexOf(String str, int fromIndex);
    public abstract int indexOf(char ch, int fromIndex);

    public void skip(CharChecker skipCharChecker, boolean reverseCheck) {
        while (
             !beyondEnd() && (reverseCheck ^ skipCharChecker.check(getCurChar()))
        ) {
            m_pos++;
        }
    }

    public int indexOf(String str) {
        return indexOf(str, m_pos);
    }

    public String endSubstr() {
        return substr(length());
    }

    public void skipBlanks() {
        shiftHelper(m_blankCharChecker, false);
    }

    public boolean thisBlank() {
        return isBlank(getCurChar());
    }

    public boolean thisChar(char ch, boolean needToMove) {
        if (beyondEnd()) return false;
        boolean res = (getCurChar() == ch);
        if (needToMove && res) m_pos++;
        return res;
    }

    public boolean thisCharThenMove(char ch) {
        return thisChar(ch, true);
    }

    public boolean thisChar(char ch) {
        return thisChar(ch, false);
    }

    public void ensureChar(char ch) {
        if (!thisChar(ch, true)) raiseError("Char '" + ch + "' expected");
    }

    public boolean thisStr(String str, boolean needToMove) {
        boolean ret = substrL(str.length()).equals(str);
        if (needToMove && ret) m_pos += str.length();
        return ret;
    }

    public boolean thisStr(String str) {
        return thisStr(str, false);
    }

    public boolean thisStrThenMove(String str) {
        return thisStr(str, true);
    }

    public boolean thisText(String str, boolean needToMove) {
        boolean ret = substrL(str.length()).equalsIgnoreCase(str);
        if (needToMove && ret) m_pos += str.length();
        return ret;
    }

    public boolean thisText(String str) {
        return thisText(str, false);
    }

    public boolean thisTextThenMove(String str) {
        return thisText(str, true);
    }

    private String shiftHelper(CharChecker skipCharChecker, boolean reverseCheck) {
        int pos = m_pos;
        skip(skipCharChecker, reverseCheck);
        return backSubstr(pos);
    }

    private String shiftHelperEx(CharCheckerEx skipCharChecker) {
        int pos = m_pos;
        boolean skipMode = false;
        StringBuffer res = new StringBuffer();
        while (true) {
            if (beyondEnd()) break;
            int checkResult = skipCharChecker.check(getCurChar());
            if (checkResult == CharCheckerEx.STOP) {
                break;
            } else if (checkResult == CharCheckerEx.SKIP) {
                if (!skipMode) {
                    res.append(backSubstr(pos));
                    skipMode = true;
                }
            } else {
                if (skipMode) {
                    pos = m_pos;
                    skipMode = false;
                }
            }
            m_pos++;
        }
        if (!skipMode) res.append(backSubstr(pos));
        return res.toString();
    }

    public String weakShiftToStr(String str) {
        int pos = m_pos;
        while (!(beyondEnd() || thisStr(str, false))) m_pos++;
        return backSubstr(pos);
    }

    public String shiftToStr(String str) {
        int pos = m_pos;
        String ret = weakShiftToStr(str);
        if (beyondEnd()) {
            raiseError("Could not shift to string '" + str + "'", pos);
        }
        return ret;
    }

    public String weakShiftBeyondStr(String str) {
        String ret = weakShiftToStr(str);
        m_pos += str.length();
        return ret;
    }

    public String shiftBeyondStr(String str) {
        int pos = m_pos;
        String ret = weakShiftBeyondStr(str);
        if (beyondEnd()) {
            raiseError("Could not shift beyond string '" + str + "'", pos);
        }
        return ret;
    }

    public String weakShiftToChar(char ch) {
        return weakShiftToStr(Character.toString(ch));
    }

    public String shiftToChar(char ch) {
        return shiftToStr(Character.toString(ch));
    }

    public String weakShiftToChars(String chars, boolean errorIfEnd) {
        int pos = m_pos;
        while (!beyondEnd() && (chars.indexOf(getCurChar()) < 0)) {
            m_pos++;
        }
        if (errorIfEnd && beyondEnd()) {
            raiseError("Could not shift to one of the chars '" + chars + "'", pos);
        }
        return backSubstr(pos);
    }

    public String shiftToChars(String chars) {
        return weakShiftToChars(chars, true);
    }

    public String weakShiftBeyondChar(char ch) {
        return weakShiftBeyondStr(Character.toString(ch));
    }

    public String shiftBeyondChar(char ch) {
        return shiftBeyondStr(Character.toString(ch));
    }

    public String shiftWord() {
        return shiftHelper(m_blankCharChecker, true);
    }

    public String shiftWordEx(final String stopChars) {
        return shiftHelper(
            new CharChecker() {
                public boolean check(char ch) {
                    return m_blankCharChecker.check(ch) || (stopChars.indexOf(ch) >= 0);
                }
            }
            , true
        );
    }

    public String shiftDigits() {
        return shiftHelper(c_digitCharChecker, false);
    }

    public int shiftInt() {
        return Integer.parseInt(shiftDigits());
    }

    public String shiftToChar(final char bracket, final char escapeChar) {
        int l = length() - 1;
        if (m_pos > l) ensureNotEnd("No closing bracket");
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ni = indexOf(bracket, m_pos);
            if (ni < 0) raiseError("No closing bracket from here");
            sb.append(substr(m_pos, ni));
            if (bracket == escapeChar) {
                m_pos = ni;
                if (ni >= l || getCharAt(ni + 1) != bracket) break;
                m_pos++;
            } else {
                if (m_pos == ni) break;
                m_pos = ni;
                if (getCharAt(ni - 1) != escapeChar) break;
            }
            sb.append(bracket);
            m_pos++;
            if (m_pos >= l) break;
        }
        return sb.toString();

        /*
        return shiftHelperEx(
            new CharCheckerEx() {
                private boolean m_skipped = false;
                public int check(char ch) {
                    if (m_skipped) {
                        m_skipped = false;
                        return INCLUDE;
                    }
                    if (ch != bracket) return INCLUDE;
                    if (escapeChar == ' ') return STOP;
                    if (m_pos >= length() - 1) return STOP;
                    if (getCharAt(m_pos + 1) == escapeChar) {
                        m_skipped = true;
                        return SKIP;
                    } else {
                        return STOP;
                    }
                }
            }
        );
        */
    }

    public String shiftBeyondChar(char bracket, char escapeChar, boolean ensureNotEnd) {
        int pos = m_pos;
        String ret = shiftToChar(bracket, escapeChar);
        if (ensureNotEnd && beyondEnd()) raiseError(
            "Could not shift beyond char '" + bracket +
            "' with esc '" + escapeChar + "'",
            pos
        );
        if (!beyondEnd()) m_pos++;
        return ret;
    }

    public String shiftBeyondChar(char bracket, char escapeChar) {
        return shiftBeyondChar(bracket, escapeChar, true);
    }

    public String shiftBracketedString(char bracket, boolean ensureNotEnd) {
        return shiftBeyondChar(bracket, bracket, ensureNotEnd);
    }

    public final String shiftBracketedString(char bracket) {
        return shiftBracketedString(bracket, true);
    }

    public final String shiftWordOrBracketedString(char bracket) {
        if (thisCharThenMove(bracket)) return shiftBracketedString(bracket);
        return shiftWord();
    }

    public final String shiftWordOrBracketedStringEx(char bracket, String stopChars) {
        if (thisCharThenMove(bracket)) return shiftBracketedString(bracket);
        return shiftWordEx(stopChars);
    }

    public final String shiftWordOrBracketedString(String brackets) {
        int bpos = brackets.indexOf(getCurChar());
        if (bpos >= 0) {
            m_pos++;
            return shiftBracketedString(brackets.charAt(bpos));
        }
        return shiftWord();
    }

    public final Property getProperty(char bracket, char stopValueMark) {
        skipBlanks();
        final String name = shiftToChar('=').trim();
        m_pos++;
        skipBlanks();
        ensureNotEnd();
        String value;
        if (stopValueMark == ' ') {
            value = shiftWordOrBracketedString(bracket);
        } else {
            value = weakShiftBeyondChar(stopValueMark);
        }
        return new Property(name, value);
    }

    public final Property getProperty(char bracket) {
        return getProperty(bracket, ' ');
    }

    public final String getProperty(
        String expectedName, boolean ignoreCase, char bracket, char stopValueMark
    ) {
        Property param = getProperty(bracket, stopValueMark);
        String name = param.name;
        if (ignoreCase) name = name.toLowerCase();
        if (!name.equals(expectedName)) {
            throw new RuntimeException(
                "Expected param name '" + expectedName +
                "' but actual '" + name + "'"
            );
        }
        return param.value;
    }

    public final String getProperty(
        String expectedName, boolean ignoreCase, char bracket
    ) {
        return getProperty(expectedName, ignoreCase, bracket, ' ');
    }

    public final Properties getProperties(Properties props, char bracket, String stopString) {
        if (props == null) props = new Properties();
        while (true) {
            skipBlanks();
            if (beyondEnd() || (stopString != null && thisStrThenMove(stopString))) return props;
            Property prop = getProperty(bracket);
            props.setProperty(prop.name, prop.value);
        }
    }

    public final String thisParamThenMove(
        String expectedName, boolean ignoreCase, char bracket
    ) {
        int pos = m_pos;

        skipBlanks();
        String name = weakShiftToChar('=');
        if (beyondEnd()) {
            setPos(pos);
            return null;
        }
        name = name.trim();
        if (ignoreCase) name = name.toLowerCase();
        if (!name.equals(expectedName)) {
            setPos(pos);
            return null;
        }
        m_pos++;
        skipBlanks();
        if (beyondEnd()) {
            setPos(pos);
            return null;
        }
        return shiftWordOrBracketedString(bracket);
    }

    public final String shiftLine() {
        String ret = weakShiftToChar('\n');
        if (beyondEnd()) return ret; else m_pos++;
        if ((ret.length() > 0) && (ret.charAt(ret.length() - 1) == '\r')) {
            return ret.substring(0, ret.length() - 1);
        }
        return ret;
    }

    public final void parseDelimitedListEx(char bracket, char separator, char end, List<String> list) {
        String stopChars = "" + separator + end;
        while (true) {
            skipBlanks();
            if (beyondEnd()) break;
            String item = shiftWordOrBracketedStringEx(bracket, stopChars);
            if (list != null) list.add(item);
            skipBlanks();
            if (beyondEnd()) break;
            if (separator == ' ') {
                if (thisCharThenMove(end)) {
                    break;
                }
            } else {
                if (!thisCharThenMove(separator)) {
                    ensureChar(end);
                    break;
                }
            }
        }
    }

    public final List<String> parseDelimitedList(char bracket, char separator, char end) {
        List<String> ret = new LinkedList<String>();
        parseDelimitedListEx(bracket, separator, end, ret);
        return ret;
    }

    public final void skipDelimitedList(char bracket, char separator, char end) {
        parseDelimitedListEx(bracket, separator, end, null);
    }

    public final List<String> parseList(char bracket, char end) {
        return parseDelimitedList(bracket, ' ', end);
    }

    public void findString(String str) {
        m_pos = indexOf(str);
        if (m_pos < 0) m_pos = length();
    }

    public void findChar(char ch) {
        findString(Character.toString(ch));
    }

    private int getValidPos(int pos) {
        if (pos < 0) return 0;
        if (pos > length()) return length();
        return pos;
    }

    protected String getErrorInfo(int pos) {
        StringBuffer sb = new StringBuffer();
        sb.append("pos = ").append(pos).append(" '");
        sb.append(substr(getValidPos(m_pos - 10), getValidPos(m_pos))).append("--><--");
        if (!beyondEnd()) sb.append(substrL(10)).append("'");
        return sb.toString();
    }

    public void ensureNotEnd(String message) {
        if (beyondEnd()) raiseError(
            message == null ? "Unexpected end of text" : message, false
        );
    }

    public void ensureNotEnd() {
        ensureNotEnd(null);
    }

    public void raiseError(String message, int pos) {
        throw new RuntimeException(message + ": " + getErrorInfo(pos));
    }

    public void raiseError(String message, boolean showInfo) {
        throw new RuntimeException(message + ": " + getErrorInfo(m_pos));
    }

    public void raiseError(String message) {
        raiseError(message, true);
    }
}
