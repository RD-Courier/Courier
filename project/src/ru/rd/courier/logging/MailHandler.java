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

import ru.rd.courier.utils.MailConnection;
import ru.rd.courier.utils.StringContext;
import ru.rd.courier.utils.templates.HashMapStringContext;
import ru.rd.courier.utils.templates.SimpleTemplate;
import ru.rd.thread.WorkThread;
import ru.rd.utils.State;
import ru.rd.utils.StatedObject;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MailHandler extends Handler {
    private final CourierLogger m_logger;
    private final String m_appPath;
    private final String m_subjectTemplate;
    private final String[] m_to;
    private final String m_from;
    //private final String m_smtpHost;
    private final boolean m_publishStackTrace;
    private final SimpleTemplate m_template;
    private final long m_connectTimeout;
    private final long m_sendTimeout;

    private final StatedObject m_state;
    private WorkThread m_thread = null;
    private MailConnection m_con;

    public MailHandler(
        String appPath, String subjectTemplate, String smtpHost,
        String from, String[] to,
        boolean publishStackTrace
    ) throws MessagingException {
        this(
            appPath, subjectTemplate,
            smtpHost, from, to, publishStackTrace, 60*1000, 20*1000
        );
    }

    private void checkArgumentNotNull(Object arg, String desc) {
        if (arg == null) {
            throw new IllegalArgumentException(desc + " cannot be null");
        }
    }

    private static final State c_runningState = new State("RUNNING");
    private static final State c_sendingState = new State("SENDING");
    private static final State c_closingState = new State("CLOSING");
    private static final State c_closedState = new State("CLOSED");

    public MailHandler(
        String appPath, String subjectTemplate, String smtpHost,
        String from, String[] to,
        boolean publishStackTrace,
        long connectTimeout, long sendTimeout
    ) throws MessagingException {
        m_logger = new LogHandlerLogger("Mail");
        m_state = new StatedObject(m_logger);
        m_state.setState(c_runningState);

        checkArgumentNotNull(subjectTemplate, "Subject template");
        checkArgumentNotNull(smtpHost, "SMTP host");
        checkArgumentNotNull(from, "'From'");
        checkArgumentNotNull(to, "'To'");
        setFormatter(new SimpleFormatter());

        m_appPath = appPath;
        m_subjectTemplate = subjectTemplate;
        //m_smtpHost = smtpHost;
        m_con = new MailConnection(smtpHost);
        //tryToConnectIfRequired();
        m_from = from;
        m_to = to;
        m_publishStackTrace = publishStackTrace;
        m_template = new SimpleTemplate("[%", "]");
        m_connectTimeout = connectTimeout;
        m_sendTimeout = sendTimeout;
    }

    private void ensureThread() {
        if (m_thread == null || m_thread.isInterrupted() || m_thread.isBusy()) {
            m_thread = new WorkThread(m_logger, "Mail logger timeout thread");
        }
    }

    private void tryToConnectIfRequired() {
        if (!m_con.isConnected()) {
            safeOperation(
                new Runnable() {
                    public void run() {
                        try { m_con.ensureConnected(); }
                        catch (MessagingException e) { m_logger.error(e); }
                    }
                }, m_connectTimeout, "connecting"
            );
        }
    }

    public void publish(final LogRecord record) {
        if (!isLoggable(record)) return;

        synchronized(m_state.lock) {
            if (m_state.getState() != c_runningState) return;
            m_state.setState(c_sendingState);
        }

        try {
            safePublish(record);
        } catch(Exception e) {
            m_logger.error(e);
        }

        synchronized(m_state.lock) {
            if (m_state.getState() == c_closingState) {
                m_state.setState(c_closedState);
            } 
            else if (m_state.getState() == c_sendingState) {
                m_state.setState(c_runningState);
            }
        }
    }

    private void safePublish(LogRecord record) throws Exception {
        String curMessage = record.getMessage();

        MimeMessage email = m_con.createMessage();
        email.setFrom(new InternetAddress(m_from));
        for (String to : m_to) {
            email.addRecipient(
                    Message.RecipientType.TO, new InternetAddress(to)
            );
        }

        StringContext ctx = new HashMapStringContext();
        ctx.setVar("app-path", m_appPath);
        ctx.setVar("host", InetAddress.getLocalHost().getHostName());
        ctx.setVar("level", record.getLevel().getName());
        int nlPos = curMessage.indexOf('\n');
        ctx.setVar("message", nlPos < 0 ? curMessage : curMessage.substring(0, nlPos));
        email.setSubject(m_template.process(m_subjectTemplate, ctx));

        StringWriter mesWriter = new StringWriter();
        PrintWriter mes = new PrintWriter(mesWriter);
        mes.println(curMessage);
        if (m_publishStackTrace && (record.getThrown() != null)) {
            mes.println();
            record.getThrown().printStackTrace(mes);
        }
        mes.flush();
        email.setText(mesWriter.getBuffer().toString(), "windows-1251");
        tryToSend(email);
    }

    private synchronized void tryToSend(final MimeMessage email) {
        tryToConnectIfRequired();
        if (!m_con.isConnected()) return;

        safeOperation(
            new Runnable() {
                public void run() {
                    try { 
                        m_con.send(email); 
                        m_con.close(); 
                    }
                    catch (MessagingException e) { m_logger.error(e); }
                }
            }, m_sendTimeout, "sending mail"
        );
    }

    private void safeOperation(WorkThread thread, Runnable work, long timeout, String operationName) {
        ensureThread();
        if (thread == null) thread = m_thread;
        boolean success = thread.launchWorkAndWait(work, timeout);
        if (!success) {
            m_logger.error(
                "Mail log handler " + operationName +
                " timeout (" + timeout + ") expired"
            );
            if (m_thread.isBusy()) {
                m_thread.stopRequest();
                m_thread.interrupt();
                m_thread = null;
            }
        }
    }

    private void safeOperation(Runnable work, long timeout, String operationName) {
        safeOperation(null, work, timeout, operationName);
    }

    public synchronized void flush() {}

    public synchronized void close() throws SecurityException {
        synchronized(m_state.lock) {
            State state = m_state.getState();
            if (state == c_closingState || state == c_closedState) return;
            if (state == c_sendingState) {
                m_state.setState(c_closingState);
                m_state.waitState(c_closedState, 500);
            } else {
                m_state.setState(c_closedState);
            }
        }

        WorkThread t;
        if (m_thread == null || m_thread.isBusy()) {
            t = new WorkThread(m_logger, "Close mail connection");
        } else {
            t = null;
        }

        if (m_con.isConnected()) {
            safeOperation(
                t,
                new Runnable() {
                    public void run() {
                        try { m_con.close(); }
                        catch (MessagingException e) { m_logger.error(e); }
                    }
                }, m_connectTimeout, "disconnecting"
            );
        }

        if (t != null) {
            if (!t.closeOrDump(1000)) {
                m_logger.error("Mail handler failed to close thread");
            }
        }

        if (m_thread != null) {
            if (!m_thread.closeOrDump(1000)) {
                m_logger.error("Mail handler failed to close thread");
            }
        }

        m_logger.stop();
    }
}
