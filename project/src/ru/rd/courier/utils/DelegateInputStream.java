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

import java.io.InputStream;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 15:01:14
 */
public class DelegateInputStream extends InputStream {
    private final InputStream m_is;

    public DelegateInputStream(InputStream is) {
        m_is = is;
    }

    public int read() throws IOException {
        return m_is.read();
    }

    public int read(byte b[]) throws IOException {
        return m_is.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException {
        return m_is.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        return m_is.skip(n);
    }

    public int available() throws IOException {
	    return m_is.available();
    }

    public void close() throws IOException {
        m_is.close();
    }

    public synchronized void mark(int readlimit) {
        m_is.mark(readlimit);
    }

    public synchronized void reset() throws IOException {
        m_is.reset();
    }

    public boolean markSupported() {
        return m_is.markSupported();
    }
}
