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

import org.apache.commons.mail.SimpleEmail;

import javax.mail.MessagingException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MailLogger extends LoggerAdapter {
    private String m_subjectPrefix;
    private String[] m_to;
    private String m_from;
    private String m_smtpHost;

    private static final String c_debugLevel = "DEBUG";
    private static final String c_infoLevel = "INFO";
    private static final String c_warnLevel = "WARNING";
    private static final String c_errorLevel = "ERROR";

    public MailLogger(
        LoggerAdapter parent, String name,
        String subjectPrefix, String smtpHost, String from, String[] to
    ) {
        super(parent, name);
        m_subjectPrefix = subjectPrefix;
        m_smtpHost = smtpHost;
        m_from = from;
        m_to = to;
    }

    public void debug(String msg) {
        send(c_debugLevel, msg);
    }

    public void info(String msg) {
        send(c_infoLevel, msg);
    }

    public void warning(String msg) {
        send(c_warnLevel, msg);
    }

    public void warning(Throwable e) {
        send(c_warnLevel, exceptionToString(e));
    }

    public void warning(String msg, Throwable e) {
        send(c_warnLevel, msg + "-- caused by\n" + exceptionToString(e));
    }

    public void error(String msg) {
        send(c_errorLevel, msg);
    }

    public void error(Throwable e) {
        send(c_errorLevel, exceptionToString(e));
    }

    public void error(String msg, Throwable e) {
        send(c_errorLevel, msg + ": " + exceptionToString(e));
    }

    private String exceptionToString(Throwable e) {
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);
        e.printStackTrace(pw);
        pw.close();
        return out.toString();
    }

    private void send(String level, String msg) {
        SimpleEmail email = new SimpleEmail();
        try {
            email.setHostName(m_smtpHost);
            email.setFrom(m_from);
            for (int i = 0; i < m_to.length; i++) {
                email.addTo(m_to[i]);
            }
            email.setSubject(m_subjectPrefix + level);
            email.setMsg(msg);
            email.send();
        } catch(MessagingException e) {
            System.err.println(e.getMessage());
        }
    }
}
