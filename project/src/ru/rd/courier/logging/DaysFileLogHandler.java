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
package ru.rd.courier.logging;

import java.io.File;
import java.io.OutputStream;
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class DaysFileLogHandler extends StreamHandler {
    private final DaysLogCleaner m_cleaner;

    public DaysFileLogHandler(
        File dir, String dateFormat, int days,
        String prefix, String postfix, boolean deleteUnknownFiles, boolean append,
        long maxSize
    ) {
        super();
        setFormatter(new SimpleFormatter());
        m_cleaner = new DaysLogCleaner(
            new DaysLogCleaner.CleanLogHandler() {
                public void setOutputStream(OutputStream stream) {
                    DaysFileLogHandler.this.setOutputStream(stream);
                }

                public void reportError(String message, Exception e) {
                    DaysFileLogHandler.this.reportError(message, e, ErrorManager.GENERIC_FAILURE);
                }
            },
            dir, dateFormat, days, prefix, postfix, deleteUnknownFiles, append, maxSize
        );
    }

    public DaysFileLogHandler(
        File dir, String dateFormat, int days,
        String prefix, String postfix, boolean append
    ) {
        this(dir, dateFormat, days, prefix, postfix, false, append, -1);
    }

    DaysLogCleaner getCleaner() {
        return m_cleaner;
    }

    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        m_cleaner.check();
        super.publish(record);
        flush();
    }
}
