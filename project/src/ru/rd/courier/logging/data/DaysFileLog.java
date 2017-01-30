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
package ru.rd.courier.logging.data;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.DayProvider;
import ru.rd.courier.logging.FastDayProvider;
import ru.rd.courier.logging.DaysLogCleaner;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.ErrorManager;

public class DaysFileLog extends AbstractDataLogger {
    private final CourierLogger m_errLogger;
    private final DaysLogCleaner m_cleaner;
    private boolean m_buffered;
    private OutputStream m_output;

    public DaysFileLog(
        CourierLogger errLogger,
        File dir, String encoding, boolean buffered, String dateFormat,
        int days, String prefix, String postfix,
        boolean deleteUnknownFiles, boolean append, long maxSize
    ) {
        super(encoding);
        m_errLogger = errLogger;
        m_cleaner = new DaysLogCleaner(
            new DaysLogCleaner.CleanLogHandler() {
                public void setOutputStream(OutputStream stream) {
                    DaysFileLog.this.setOutputStream(stream);
                }

                public void reportError(String message, Exception e) {
                    DaysFileLog.this.reportError(message, e);
                }
            },
            dir, dateFormat, days, prefix, postfix, deleteUnknownFiles, append, maxSize
        );
    }

    private void setOutputStream(OutputStream os) {
        if (m_output != null) {
            close();
        }
        m_output = os;
        if (m_buffered) {
            m_output = new BufferedOutputStream(m_output);
        }
    }

    public void log(byte[] msg, int offset, int length) {
        if (m_output == null) throw new IllegalStateException("Log already closed");
        try {
            m_cleaner.check();
            m_output.write(msg, offset, length);
        } catch (IOException e) { reportError(e); }
    }

    public void flush() {
        if (m_output == null) new IllegalStateException("Log already closed");
        try { m_output.flush(); }
        catch (IOException e) { reportError(e); }
    }

    public synchronized void close() {
        if (m_output == null) return;
        OutputStream output = m_output;
        m_output = null;
        try { output.close(); }
        catch(IOException e) { reportError(e); }
    }

    private void reportError(String msg) {
        m_errLogger.error(msg);
    }

    private void reportError(String msg, Throwable e) {
        m_errLogger.error(msg, e);
    }

    private void reportError(Throwable e) {
        m_errLogger.error(e);
    }
}
