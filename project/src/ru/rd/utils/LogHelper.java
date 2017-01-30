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
package ru.rd.utils;

import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;

/**
 * User: AStepochkin
 * Date: 23.10.2007
 * Time: 17:38:23
 */
public class LogHelper {
    public static LogRecord warnRecord(String msg, Throwable thrown) {
        //LogRecord lr = new LogRecord(Level.WARNING, "Failed to close file '" + file + "'");
        LogRecord lr = new LogRecord(Level.WARNING, msg);
        lr.setThrown(thrown);
        return lr;
    }

    public static LogRecord warnRecord(String msg, Object params[], Throwable thrown) {
        LogRecord lr = warnRecord(msg, thrown);
        lr.setParameters(params);
        return lr;
    }

    public static LogRecord warnRecord(String msg, Object param1, Throwable thrown) {
        Object params[] = { param1 };
        return warnRecord(msg, params, thrown);
    }

    public static void logStdWarning(String msg, Object params[], Throwable thrown) {
        getStdLogger().log(warnRecord(msg, params, thrown));
    }

    public static void logStdWarning(String msg, Object param1, Throwable thrown) {
        getStdLogger().log(warnRecord(msg, param1, thrown));
    }

    public static void logStdWarning(String msg, Throwable thrown) {
        getStdLogger().log(warnRecord(msg, thrown));
    }

    private static Logger getStdLogger() {
        return Logger.getLogger("");
    }

    public static void clearHandlers(Logger logger) {
        for (Handler h: logger.getHandlers()) {
            logger.removeHandler(h);
        }
    }
}
