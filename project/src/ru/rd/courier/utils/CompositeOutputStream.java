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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.CourierLoggerAdapter;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 15.11.2007
 * Time: 15:52:31
 */
public class CompositeOutputStream extends OutputStream {
    private final CourierLogger m_logger;
    private final List<OutputStream> m_outs = new LinkedList<OutputStream>();

    public CompositeOutputStream(CourierLogger logger) {
        super();
        if (logger == null) {
            m_logger = new CourierLoggerAdapter(Logger.getLogger(""));
        } else {
            m_logger = logger;
        }
    }

    public void addOutput(OutputStream out) {
        m_outs.add(out);
    }

    public void write(int b) throws IOException {
        for (OutputStream out: m_outs) {
            out.write(b);
        }
    }

    public void write(byte b[], int off, int len) throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) throw new IndexOutOfBoundsException();

        for (OutputStream out: m_outs) {
            out.write(b, off, len);
        }
    }

    public void flush() throws IOException {
        for (OutputStream out: m_outs) {
            out.flush();
        }
    }

    public void close() throws IOException {
        for (OutputStream out: m_outs) {
            try {
                out.close();
            } catch (IOException e) {
                m_logger.warning(e);
            }
        }
    }
}
