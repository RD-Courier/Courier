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

import java.io.IOException;
import java.io.Reader;

/**
 * User: AStepochkin
 * Date: 18.02.2005
 * Time: 17:35:59
 */
public class LineReader {
    private final Reader m_reader;
    private long m_bytesRead = 0;
    private boolean m_fullLine = false;

    public LineReader(Reader reader) {
        m_reader = reader;
    }

    public boolean skipLine() throws IOException {
        m_fullLine = false;
        int ich;
        while ((ich = m_reader.read()) >= 0) {
            m_bytesRead++;
            char ch = (char)ich;
            if (ch == '\n') { m_fullLine = true; break; }
        }
        return ich >= 0;
    }

    public void appendLine(StringBuffer buffer) throws IOException {
        m_fullLine = false;
        int ich;
        while ((ich = m_reader.read()) >= 0) {
            m_bytesRead++;
            char ch = (char)ich;
            buffer.append(ch);
            if (ch == '\n') { m_fullLine = true; break; }
        }
    }

    public String readLine(boolean addLFCR) throws IOException {
        StringBuffer sb = new StringBuffer();
        appendLine(sb);
        if (!addLFCR) StringHelper.trimLFCR(sb);
        return sb.toString();
    }

    public String readLine() throws IOException {
        return readLine(false);
    }

    public String readLineOrNull() throws IOException {
        StringBuffer sb = new StringBuffer();
        long bytesRead = m_bytesRead;
        appendLine(sb);
        if (m_bytesRead > bytesRead) {
            StringHelper.trimLFCR(sb);
            return sb.toString();
        }
        return null;
    }

    public int appendFullLines(StringBuffer buffer, int lineCount) throws IOException {
        StringBuffer sb = new StringBuffer();
        int realCount = 0;
        for (; realCount < lineCount; realCount++) {
            sb.setLength(0);
            appendLine(sb);
            if (m_fullLine) {
                buffer.append(sb);
            } else {
                break;
            }
        }
        return realCount;
    }

    public long getBytesRead() {
        return m_bytesRead;
    }

    public void close() throws IOException {
        m_reader.close();
    }
}
