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

import ru.rd.courier.FileDataTestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 22.07.2005
 * Time: 12:39:30
 */
public class GlTradeLineParserTest extends FileDataTestCase {
    private GlTradeLineParser m_parser;
    private final List<Item> m_data = new ArrayList<Item>();

    private char m_separator;
    private char m_bracket;
    private String m_leftBlanks;
    private String m_rightBlanks;


    protected void courierSetUp() {
        initTest(',', '\'', "", "");
        m_parser = new GlTradeLineParser(m_bracket, true, m_separator, true, null);
        clearData();
    }

    protected void courierTearDown() {
    }

    private void initTest(
        char separator, char bracket,
        String leftBlanks, String rightBlanks
    ) {
        m_separator = separator;
        m_bracket = bracket;
        m_leftBlanks = leftBlanks;
        m_rightBlanks = rightBlanks;
    }

    private static class Item {
        public String m_str;
        public boolean m_bracketed;

        public Item(String str, boolean bracketed) {
            m_str = str;
            m_bracketed = bracketed;
        }
        public String getString() { return m_str; }
        public void setString(String str) { m_str = str; }
        public void setBracketed(boolean bracketed) { m_bracketed = bracketed; }
        public boolean getBracketed() { return m_bracketed; }
    }

    private Item getItem(int index) {
        return m_data.get(index);
    }

    private void addData(String[] strData, boolean bracketed) {
        for (String str: strData) {
            addData(str, bracketed);
        }
    }

    private void addData(String str, boolean bracketed) {
        m_data.add(new Item(str, bracketed));
    }

    private void addRandData(boolean bracketed) {
        addData(m_data.size() + "-" + Math.random(), bracketed);
    }

    private void addRandData(boolean[] bracketed) {
        for (boolean b: bracketed) addRandData(b);
    }

    private void setData(int index, String str, boolean bracketed) {
        getItem(index).setString(str);
        getItem(index).setBracketed(bracketed);
    }

    private void clearData() {
        m_data.clear();
    }

    private void parseAndCheck() {
        StringBuffer sb = new StringBuffer();
        List<String> expData = new LinkedList<String>();
        int i = 0;
        for (Item item: m_data) {
            expData.add(item.getString());

            if (i > 0) {
                sb.append(m_separator);
                sb.append(m_leftBlanks);
            }
            if (item.getBracketed()) sb.append(m_bracket);
            sb.append(item.getString());
            if (item.getBracketed()) sb.append(m_bracket);
            sb.append(m_rightBlanks);
            i++;
        }
        String[] expDataArr = (String[])expData.toArray(new String[m_data.size()]);
        //System.out.println(new String(sb) + "<<<<");
        checkArrays(expDataArr, m_parser.parse(sb));
    }

    private String[] parse(String str) {
        StringBuffer sb = new StringBuffer(str);
        return m_parser.parse(sb);
    }

    private String getCharString(char ch, int count) {
        char[] buf = new char[count];
        for (int i = 0; i < count; i++) buf[i] = ch;
        return new String(buf);
    }

    public void testGeneral() {
        final String[] blankVariants = new String[] {"", " ", "  ", "\t", " \t"};
        iterateArray(1, 3, new CombinationHandler() {
            public void execute(final int[] dimArr) throws Exception {
                final int dim = dimArr[0] + 1;
                final boolean[] bracketsArray = new boolean[dim];
                iterateArray(2, blankVariants.length - 1, new CombinationHandler() {
                    public void execute(final int[] blankArr) throws Exception {
                        iterateArray(dim, 1, new CombinationHandler() {
                            public void execute(int[] arr) throws Exception {
                                initTest(
                                    m_separator, m_bracket,
                                    blankVariants[blankArr[0]],
                                    blankVariants[blankArr[1]]
                                );
                                intArrayToBool(arr, bracketsArray);
                                addRandData(bracketsArray);
                                parseAndCheck();
                                clearData();
                            }
                        });
                    }
                });
            }
        });
    }

    public void testBrackets() {
        checkArrays(
            new String[] {getCharString(m_bracket, 7)},
            parse(getCharString(m_bracket, 9))
        );
        String expStr1, actStr1, expStr2, actStr2;
        expStr1 = getCharString(m_bracket, 7); actStr1 = getCharString(m_bracket, 9);
        expStr2 = getCharString(m_bracket, 7); actStr2 = getCharString(m_bracket, 9);
        checkArrays(
            new String[] { expStr1, expStr2 },
            parse(actStr1 + " , " + actStr2)
        );

        expStr1 = "abcd'"; actStr1 = "'abcd''";
        expStr2 = "'ab'cd'"; actStr2 = "''ab'cd''";
        checkArrays(
            new String[] { expStr1, expStr2 },
            parse(actStr1 + " , " + actStr2)
        );
    }
}
