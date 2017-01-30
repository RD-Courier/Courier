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

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;

/**
 * User: AStepochkin
 * Date: 07.04.2005
 * Time: 17:15:44
 */
public class SectionsParserTest extends TestCase {
    private static final String[] c_data = new String[] {
        "before sections\n" +
        "ahsjd fhryet",
        "section-1",
        "first section content\n" +
        "first section content2\n" +
        "^^section-esc\n" +
        "first section content3",
        "section-2",
        "second section content\n" +
        "second section content2\n",
        "section-3",
        "third section content\n" +
        "third section content2 aaaa\n" +
        "third section content3"
    };

    private static String formData(String[] data) {
        StringBuffer ret = new StringBuffer(data[0]);
        for (int i = 1; i < data.length; i += 2) {
            if ((i == 1) && (data[0].length() == 0)) ret.append(SectionsParser.c_sectionBegin);
            else ret.append(SectionsParser.c_sectionDelimiter);
            ret.append(data[i]);
            ret.append('\n');
            ret.append(data[i+1]);
        }
        return ret.toString();
    }

    private static Map<String, String> formResult(String[] data) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(SectionsParser.c_defaultSectionName, data[0]);
        for (int i = 1; i < data.length; i += 2) {
            ret.put(data[i], SectionsParser.unescape(data[i+1]));
        }
        return ret;
    }

    public void testSimple() {
        SectionsParser p = new SectionsParser();
        assertEquals(p.parse(formData(c_data)), formResult(c_data));
    }
}
