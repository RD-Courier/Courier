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
package ru.rd.test;

import ru.rd.courier.logging.CourierLogger;

import java.util.List;
import java.util.LinkedList;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * User: AStepochkin
 * Date: 25.07.2005
 * Time: 15:10:36
 */
public class TestCourierLogger implements CourierLogger {
    private final boolean m_showOnConsole;
    private final List<ErrorInfo> m_errors = new LinkedList<ErrorInfo>();

    public TestCourierLogger(boolean showOnConsole) {
        m_showOnConsole = showOnConsole;
    }

    public TestCourierLogger() {
        this(true);
    }

    private void addError(String msg, Throwable e) {
        show(msg, e);
        m_errors.add(new ErrorInfo(msg, e));
    }

    private void show(String msg, Throwable e) {
        if (m_showOnConsole) {
            StringWriter sb = new StringWriter();
            if (msg != null) {
                //sb.write("msg = ");
                sb.write(msg);
            }
            if (e != null) {
                if (sb.getBuffer().length() > 0) sb.write('\n');
                sb.write("exception = ");
                PrintWriter pw = new PrintWriter(sb);
                e.printStackTrace(pw);
                pw.close();
                //if (sb.length() > 0) sb.append(" ");
                //sb.append("exception = ");
                //sb.append(e.getMessage());
            }
            System.err.println(sb.toString());
        }
    }

    public final List<ErrorInfo> getErrors() {
        return m_errors;
    }

    public String getLoggerName() {
        return "test";
    }

    public CourierLogger getParentLogger() {
        return null;
    }

    public CourierLogger getChild(String name) {
        return null;
    }

    public void stop() {
    }

    public void debug(String msg) {
        show(msg, null);
    }
    public void info(String msg) {
        show(msg, null);
    }
    public void warning(String msg) {
        show(msg, null);
    }
    public void warning(Throwable e) {
        show(null, e);
    }
    public void warning(String msg, Throwable e) {
        show(msg, e);
    }
    public void error(String msg) {
        addError(msg, null);
    }
    public void error(Throwable e) {
        addError(null, e);
    }
    public void error(String msg, Throwable e) {
        addError(msg, e);
    }
}
