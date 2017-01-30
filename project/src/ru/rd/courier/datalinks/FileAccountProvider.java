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
package ru.rd.courier.datalinks;

import ru.rd.courier.utils.StringSimpleParser;

import java.io.File;
import java.io.IOException;

/**
 * User: Astepochkin
 * Date: 07.05.2009
 * Time: 15:37:34
 */
public class FileAccountProvider extends DefaultAccountProvider {
    private static final char BRACKET = '\'';

    public FileAccountProvider(File f, String pwd) throws IOException {
        String accounts = AccountUtils.decrypt(f, pwd);
        StringSimpleParser p = new StringSimpleParser(accounts);
        while (true) {
            p.skipBlanks();
            if (p.beyondEnd()) break;
            String akey = p.shiftWordOrBracketedStringEx(BRACKET, ":");
            p.skipBlanks();
            p.ensureChar(':');
            p.skipBlanks();
            String aname = p.shiftWordOrBracketedString(BRACKET);
            p.skipBlanks();
            String apwd = p.shiftWordOrBracketedStringEx(BRACKET, ";");
            p.skipBlanks();
            p.ensureChar(';');
            addCodeAccount(akey, new Account(aname, apwd));
        }
    }
}
