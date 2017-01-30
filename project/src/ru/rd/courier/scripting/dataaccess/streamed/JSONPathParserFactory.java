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
package ru.rd.courier.scripting.dataaccess.streamed;

import ru.rd.courier.datalinks.JSONPathSourceFactory;
import ru.rd.courier.utils.StringSimpleParser;
import org.w3c.dom.Node;

import java.sql.ResultSet;
import java.io.InputStream;

/**
 * User: AStepochkin
 * Date: 10.08.2006
 * Time: 10:46:12
 */
public class JSONPathParserFactory implements StreamParserFactory {
    private final JSONPathSourceFactory m_factory;

    public JSONPathParserFactory(Node conf) {
        m_factory = new JSONPathSourceFactory(null, null, conf);
    }

    public StreamParser createParser() {
        return new StreamParser() {
            public void parseProperties(StringSimpleParser p) {}

            public ResultSet parse(InputStream is) {
                try {
                    return m_factory.createSource().getResultSet(is);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public void cancel() {}
        };
    }
}
