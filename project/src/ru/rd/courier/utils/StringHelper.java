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

import ru.rd.courier.CourierException;
import ru.rd.utils.SimpleArrayList;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.util.*;

public class StringHelper {
    public static boolean thisSubString(String str, int begPos, String findStr) {
        return (str.substring(begPos, findStr.length()).equals(findStr));
    }

    public static boolean thisSubText(String str, int begPos, String findStr) {
        return (str.substring(begPos, findStr.length()).equalsIgnoreCase(findStr));
    }

    public static String replaceChars(final String src, String chars, String[] to) {
        final int charsLength = chars.length();
        if (charsLength != to.length) {
            throw new RuntimeException(
                "Chars count " + chars.length() + " <> strings count " + to.length);
        }
        if (charsLength == 1) {
            return replace(src, chars.charAt(0), to[0]);
        }
        StringBuffer res = null;
        int afterLastCopied = 0;
        for (int pos = 0; pos < src.length(); pos++) {
            int charIndex = chars.indexOf(src.charAt(pos));
            if (charIndex >= 0) {
                if (res == null) res = new StringBuffer(
                    src.length() + to[charIndex].length() - 1
                );
                if (pos > afterLastCopied) res.append(src.substring(afterLastCopied, pos));
                afterLastCopied = pos + 1;
                res.append(to[charIndex]);
            }
        }
        if (res == null) return src;
        res.append(src.substring(afterLastCopied, src.length()));
        return res.toString();
    }

    public static String replace(final String src, final char from, final String to) {
        int pos = src.indexOf(from);
        if (pos < 0) return src;
        final StringBuffer res = new StringBuffer(src.length() + 4 * (to.length() - 1));
        int prevPos = 0;
        while (pos >= 0) {
            res.append(src.substring(prevPos, pos));
            res.append(to);
            prevPos = pos + 1;
            pos = src.indexOf(from, prevPos);
        }
        res.append(src.substring(prevPos, src.length()));
        return res.toString();
    }

    public static String replace(final String src, final String from, final String to) {
        int pos = src.indexOf(from);
        if (pos < 0) return src;
        final StringBuffer res = new StringBuffer(src.length() + 4 * (to.length() - 1));
        int prevPos = 0;
        while (pos >= 0) {
            res.append(src.substring(prevPos, pos));
            res.append(to);
            prevPos = pos + from.length();
            pos = src.indexOf(from, prevPos);
        }
        res.append(src.substring(prevPos, src.length()));
        return res.toString();
    }

    public static String replaceBracket(final String src, final char bracket) {
        return replace(
            src,
            new String(new char[] {bracket, bracket}),
            new String(new char[] {bracket})
        );
    }

    public static String formatSqlString(final String str) {
        if (str  == null) return "NULL";
        return "'" + str + "'";
    }

    public static void escapeSqlString(StringBuffer out, final String str) {
        out.ensureCapacity(out.capacity() + str.length() + 2);
        out.append('\'');
        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
            if (ch == '\'') out.append("''"); else out.append(ch);
        }
        out.append('\'');
    }

    public static String escapeSqlStringWithFuncs(final String str) {
        if (str.length() == 0) return "''";
        int i = 0;
        final StringBuffer ret = new StringBuffer(str.length());
        boolean isSimpleString = true;
        for (; i < str.length(); i++) {
            final char ch = str.charAt(i);
            switch (ch) {
                case '\r': {
                    if (i > 0) {
                        if (isSimpleString) ret.append('\'');
                        ret.append('+');
                    }
                    ret.append("CHAR(10)");
                    isSimpleString = false;
                    break;
                }
                case '\n': {
                    if (i > 0) {
                        if (isSimpleString) ret.append('\'');
                        ret.append('+');
                    }
                    ret.append("CHAR(13)");
                    isSimpleString = false;
                    break;
                }
                default: {
                    if (i == 0) {
                        ret.append("'");
                    } else if (!isSimpleString) {
                        ret.append("+'");
                    }
                    if (ch == '\'') ret.append("''"); else ret.append(ch);
                    isSimpleString = true;
                    break;
                }
            }
        }
        if (isSimpleString) ret.append('\'');
        return ret.toString();
    }

    public static String escapeSqlStringEx(final String str) {
        if (str == null) return "NULL";
        return escapeSqlStringWithFuncs(str);
    }

    public static int skipChars(String str, int firstPos, String chars) {
        int pos = firstPos;
        while (pos < str.length()) {
            if (chars.indexOf(str.charAt(pos)) < 0) break;
            pos++;
        }
        return pos;
    }

    private static final String c_Delimiters = " \t\n\r";
    public static int skipDelims(String str, int startPos) {
        return skipChars(str, startPos, c_Delimiters);
    }

    public static int findChars(String str, int firstPos, String chars) {
        int pos = firstPos;
        while (pos < str.length()) {
            if (chars.indexOf(str.charAt(pos)) >= 0) break;
            pos++;
        }
        return pos;
    }

    public static boolean isSubstring(String str, int pos, String sstr) {
        int i = 0;
        while (i < sstr.length()) {
            int p = pos + i;
            if (p >= str.length()) return false;
            if (str.charAt(p) != sstr.charAt(i)) return false;
            i++;
        }
        return true;
    }

    public static int findChars(String str, int firstPos, String chars, String endString) {
        int pos = firstPos;
        while (pos < str.length()) {
            if (chars.indexOf(str.charAt(pos)) >= 0) break;
            if (endString != null && isSubstring(str, pos, endString)) break;
            pos++;
        }
        return pos;
    }

    public static int rfindChars(String str, String chars) {
        int pos = str.length();
        while (true) {
            pos--;
            if (pos < 0) break;
            if (chars.indexOf(str.charAt(pos)) >= 0) break;
        }
        return pos;
    }

    public static String[] splitStringAndTrim(String str, char sep) {
        String[] ret = splitString(str, sep);
        if (ret == null) return null;
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ret[i].trim();
        }
        return ret;
    }

    public static String[] splitString(String str, char sep, int begPos) {
        if (str == null) return null;
        Collection res = new LinkedList();
        int i = begPos;
        int ni;
        int length = str.length();
        while(true) {
            if (i >= length) break;
            ni = str.indexOf(sep, i);
            if (ni < 0) {
                res.add(str.substring(i));
                break;
            }
            res.add(str.substring(i, ni));
            i = ni + 1;
            if (i == length) {
                res.add("");
                break;
            }
        }
        return (String[])res.toArray(new String[res.size()]);
    }

    public static String[] splitString(String str, char sep) {
        return splitString(str, sep, 0);
    }

    public static String[] splitString(
        String str, String separators, char bracket
    ) throws CourierException {
        return splitString(str, separators, bracket, -1);
    }

    public static String[] splitString(
        String str, String separators, char bracket, int maxParts
    ) throws CourierException {
        StringsResult sr = splitString(str, 0, separators, bracket, maxParts, null);
        return sr.m_strings;
    }

    public static StringsResult splitString(
        String str, int begPos, String separators,
        char bracket, int maxParts, String endString
    ) {
        Collection res = new LinkedList();
        int i = begPos;
        int ni;
        while(true) {
            if ((maxParts > 0) && (res.size() == maxParts - 1)) {
                res.add(str.substring(i));
                break;
            }
            if (i >= str.length()) break;
            if (separators.indexOf(str.charAt(i)) >= 0) i++;
            //i = skipChars(str, i, separators);
            if (
                (i >= str.length()) ||
                ((endString != null) && (str.substring(i, i + endString.length()).equals(endString)))
            ) break;
            if (str.charAt(i) == bracket) {
                if (i == (str.length() - 1)) {
                    throw new RuntimeException("End of string without closing bracket");
                }
                StringBuffer sb = new StringBuffer();
                while (true) {
                    i++;
                    ni = str.indexOf(bracket, i);
                    if (ni < 0) throw new RuntimeException("No closing bracket");
                    sb.append(str.substring(i, ni));
                    i = ni + 1;
                    if ((i >= str.length()) || (str.charAt(i) != bracket)) break;
                    sb.append(bracket);
                }
                res.add(sb.toString());
            } else {
                ni = findChars(str, i, separators, endString);
                res.add(str.substring(i, ni));
                i = ni;
            }
        }
        return new StringsResult(i, (String[])res.toArray(new String[res.size()]));
    }


    public static String glueStrings(String[] strArr, String separator) {
        if (strArr.length == 0) return "";
        StringBuffer buf = new StringBuffer(strArr[0].length());
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) buf.append(separator);
            buf.append(strArr[i]);
        }
        return buf.toString();
    }

    public static int[] toIntArray(String str, char separator) {
        String[] strArr = splitString(str, separator);
        int[] res = new int[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            res[i] = Integer.parseInt(strArr[i]);
        }
        return res;
    }

    public static int findEndOfBracketedString(String str, char endBracket, int begin) {
        return findEndOfBracketedString(str, endBracket, endBracket, begin);
    }

    public static int findEndOfBracketedString(String str, char endBracket, char escChar, int begin) {
        if (begin >= str.length()) {
            throw new RuntimeException("Closing bracket not found");
        }
        int res = begin;
        while (true) {
            res = str.indexOf(endBracket, res);
            if (res < 0) throw new RuntimeException("Closing bracket not found");
            res++;
            if ((res >= str.length()) || (str.charAt(res) != escChar)) break;
            res++;
        }
        return res;
    }

    public static void trimLFCR(StringBuffer buffer) {
        int i = buffer.length() - 1;
        while(i >= 0) {
            char ch = buffer.charAt(i);
            if ((ch != '\r') && (ch != '\n')) break;
            i--;
        }
        buffer.setLength(i + 1);
    }

    public static class FindStringResult {
        public int m_pos;
        public int m_strIdx;
    }

    public static class StringsResult {
        public int m_pos;
        public String[] m_strings;

        public StringsResult(int pos, String[] strings) {
            m_pos = pos;
            m_strings = strings;
        }
    }

    public static void findString(String str, int from, String[] subStrs, FindStringResult res) {
        res.m_strIdx = -1;
        res.m_pos = -1;
        for (int i = 0; i < subStrs.length; i++) {
            int pos = str.indexOf(subStrs[i], from);
            if (pos >= 0) {
                if ((res.m_pos == -1) || (pos < res.m_pos)) {
                    res.m_pos = pos;
                    res.m_strIdx = i;
                }
            }
        }
    }

    public static int parseParams(
        Properties props, String str, char bracket, int begin, char endChar
    ) {
        final String valueDelims = (endChar == ' ') ? c_Delimiters :  c_Delimiters + endChar;
        int p1 = begin, p2;
        while(true) {
            p1 = skipChars(str, p1, c_Delimiters);
            if ((p1 >= str.length()) || ((endChar != ' ') && (str.charAt(p1) == endChar))) break;
            p2 = str.indexOf('=', p1);
            if (p2 < 0) throw new RuntimeException("Missing '=' character");
            String name = str.substring(p1, p2).trim();
            p1 = p2 + 1;
            p1 = skipChars(str, p1, c_Delimiters);
            if (p1 >= str.length()) break;
            String value;
            if ((bracket != ' ') && (str.charAt(p1) == bracket)) {
                p2 = findEndOfBracketedString(str, bracket, p1 + 1);
                value = replaceBracket(str.substring(p1 + 1, p2 - 1), bracket);
            } else {
                p2 = findChars(str, p1, valueDelims);
                if (p2 > str.length()) break;
                value = str.substring(p1, p2);
            }
            props.put(name, value);
            p1 = p2;
        }
        return p1 + 1;
    }

    public static int parseParams(
        Properties props, String str, char bracket, int begin
    ) {
        return parseParams(props, str, bracket, begin, ' ');
    }

    public static int parseParams(Properties props, String str, char bracket) {
        return parseParams(props, str, bracket, 0, ' ');
    }

    public static int parseParams(Properties props, String str) {
        return parseParams(props, str, ' ', 0, ' ');
    }

    public static boolean boolParam(
        Properties props, String paramName, boolean defaultValue
    ) {
        if (props.containsKey(paramName)) {
            return props.getProperty(paramName).equalsIgnoreCase("yes");
        } else {
            return defaultValue;
        }
    }

    public static boolean boolParam(
        Map<String, String> props, String paramName, boolean defaultValue
    ) {
        if (props.containsKey(paramName)) {
            return props.get(paramName).equalsIgnoreCase("yes");
        } else {
            return defaultValue;
        }
    }

    public static boolean boolParamTF(
        Properties props, String paramName, boolean defaultValue
    ) {
        if (props.containsKey(paramName)) {
            return props.getProperty(paramName).equalsIgnoreCase("true");
        } else {
            return defaultValue;
        }
    }

    public static int intParam(Properties props, String paramName, int defaultValue) {
        if (props.containsKey(paramName)) {
            return Integer.parseInt(props.getProperty(paramName));
        } else {
            return defaultValue;
        }
    }

    public static void ensureParam(Properties props, String paramName) {
        if (!props.containsKey(paramName)) {
            throw new RuntimeException("There is no '" + paramName + "' parameter");
        }
    }

    public static void ensureParam(Map<String, String> props, String paramName) {
        if (!props.containsKey(paramName)) {
            throw new RuntimeException("There is no '" + paramName + "' parameter");
        }
    }

    public static int intParam(Properties props, String paramName) {
        ensureParam(props, paramName);
        return Integer.parseInt(props.getProperty(paramName));
    }

    public static long longParam(Properties props, String paramName, long defaultValue) {
        if (props.containsKey(paramName)) {
            return Integer.parseInt(props.getProperty(paramName));
        } else {
            return defaultValue;
        }
    }

    public static String stringParam(Properties props, String paramName, String defaultValue) {
        if (props.containsKey(paramName)) {
            return props.getProperty(paramName);
        } else {
            return defaultValue;
        }
    }

    public static String stringParam(Properties props, String paramName) {
        ensureParam(props, paramName);
        return props.getProperty(paramName);
    }

    public static String stringParam(Map<String, String> props, String paramName, String defaultValue) {
        if (props.containsKey(paramName)) {
            return props.get(paramName);
        } else {
            return defaultValue;
        }
    }

    public static String stringParam(Map<String, String> props, String paramName) {
        ensureParam(props, paramName);
        return props.get(paramName);
    }

    public static long timeParam(Properties props, String paramName, long defaultValue) {
        if (props.containsKey(paramName)) {
            return parseTime(props.getProperty(paramName));
        } else {
            return defaultValue;
        }
    }

    public static boolean readLine(Reader r, StringBuffer out) throws IOException {
        return readLineEx(r, out) > 0;
    }

    private static int readLineEx(Reader r, StringBuffer out) throws IOException {
        out.setLength(0);
        int res = 0;
        int ich;
        while ((ich = r.read()) >= 0) {
            res++;
            char ch = (char)ich;

            if (ch == '\n') break;
            if (ch != '\r') out.append(ch);
        }
        return res;
    }

    public static String showErrorPos(String str, int pos, String marker) {
        return (
            str.substring(0, Math.min(pos, str.length())) + marker +
            str.substring(Math.min(pos, str.length()))
        );
    }

    public static long parseTimeUnit(long time, String unit) {
        if ("ms".equals(unit)) time *= 1;
        else if ("s".equals(unit)) time *= 1000;
        else if ("m".equals(unit)) time *= 60*1000;
        else if ("h".equals(unit)) time *= 60*60*1000;
        else if ("d".equals(unit)) time *= 24*60*60*1000;
        else if ("w".equals(unit)) time *= 7*24*60*60*1000;
        else throw new RuntimeException("Unknown time unit '" + unit + "'");
        return time;
    }

    public static long parseTime(String time, String defaultUnit) {
        StringSimpleParser p = new StringSimpleParser();
        p.setText(time);
        long res = Long.parseLong(p.shiftDigits());
        String unit = p.endSubstr().trim().toLowerCase();
        if (unit.length() == 0) {
            if (defaultUnit == null)
                throw new RuntimeException("Unspecified time unit");
            unit = defaultUnit;
        }
        return parseTimeUnit(res, unit);
    }

    public static long parseTime(String time) {
        return parseTime(time, null);
    }

    public static long parseTimeSecondsDef(String time) {
        return parseTime(time, "s");
    }

    public static void stringListToXml(
        List res, Writer out, String tagName
    ) {
        if (res == null) return;
        for (Object portion: res) {
            try {
                out.write("\n<" + tagName + ">");
                out.write(portion.toString());
                out.write("</"+ tagName +">");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String stringListToXml(List res, String tagName) {
        StringWriter buffer = new StringWriter();
        stringListToXml(res, buffer, tagName);
        return buffer.toString();
    }

    public static Map<String, String> arrayToMap(String[] array) {
        Map<String, String> ret = new HashMap<String, String>();
        for (int i = 0; i + 1 < array.length; i += 2) {
            ret.put(array[i], array[i+1]);
        }
        return ret;
    }

    public static List<String> list(String s) {
        List<String> ret = new ArrayList<String>();
        ret.add(s);
        return ret;
    }

    public static List<String> list(String[] array) {
        return new SimpleArrayList<String>(array);
    }

    public static List<String> list(String csv, String sep) {
        return new SimpleArrayList<String>(csv.split(sep));
    }
}
