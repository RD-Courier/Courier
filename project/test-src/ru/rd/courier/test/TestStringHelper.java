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
package ru.rd.courier.test;

import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringHelper;

public class TestStringHelper {
    public static void main(String[] args) throws CourierException {
        String[] data;
        String[] trueData;

        test("Single without quotes", "abcd", new String[]{"abcd"});
        test("Single without quotes and blanks", " abcd ", new String[]{"abcd"});
        test("Single with quotes", "'abcd'", new String[]{"abcd"});
        test("Single with quotes and blanks", " 'abcd' ", new String[]{"abcd"});
        test("With and without quotes", "aaaa 'abcd'", new String[]{"aaaa", "abcd"});
        test("With inner quotes", "'abcd ''1111'' cccc'' '", new String[]{"abcd '1111' cccc' "});
        test(
            "General",
            "  1234 'aaaa bbbb cccc' asas 'sasa' 'aaaa ''bbbb'''",
            new String[]{"1234", "aaaa bbbb cccc", "asas", "sasa", "aaaa 'bbbb'"}
        );
        testExc("Skipped quote", "'abcd", new String[]{"abcd"});
    }

    private static boolean testExc(String desc, String dataStr, String[] trueData) {
        boolean res = false;
        try {
            test(desc, dataStr, trueData);
        } catch (CourierException e) {
            res = true;
        }
        return res;
    }

    private static void test(String desc, String dataStr, String[] trueData) throws CourierException {
        String[] data = StringHelper.splitString(dataStr, " ", '\'');
        System.out.println(
            "test " + desc + " --> " + compareStringArrays(data, trueData)
        );
    }

    private static boolean compareStringArrays(String[] a1, String[] a2) {
        boolean res = true;
        if (a1.length != a2.length) {
            res = false;
        } else {
            for (int i = 0; i < a1.length; i++) {
                if (!a1[i].equals(a2[i])) {
                    res = false;
                    break;
                }
            }
        }
        return res;
    }
}
