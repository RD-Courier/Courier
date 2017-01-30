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

/**
 * User: AStepochkin
 * Date: 24.10.2008
 * Time: 11:10:41
 */
public class StringHelperTest extends TestCase {
    public void testEscapeSqlStringWithFuncs() {
        String[] ins = {
            "", "'",
            "aaaa", "aaaa'bbbb", "'aaaa'bbbb'",
            "\n", "\naaaa\nbbbb\n",
            "aaaa\nbbbb",
            "\r", "\raaaa\rbbbb\r",
            "aaaa\rbbbb"
        };
        String[] outs = {
            "''", "''''",
            "'aaaa'", "'aaaa''bbbb'", "'''aaaa''bbbb'''",
            "CHAR(13)", "CHAR(13)+'aaaa'+CHAR(13)+'bbbb'+CHAR(13)",
            "'aaaa'+CHAR(13)+'bbbb'",
            "CHAR(10)", "CHAR(10)+'aaaa'+CHAR(10)+'bbbb'+CHAR(10)",
            "'aaaa'+CHAR(10)+'bbbb'"
        };
        for (int i = 0; i < ins.length; i++) {
            String in = ins[i];
            String out = StringHelper.escapeSqlStringWithFuncs(in);
            assertEquals(outs[i], out);
        }
    }
}
