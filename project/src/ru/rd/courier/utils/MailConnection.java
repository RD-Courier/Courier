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
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 28.07.2005
 * Time: 15:40:57
 */
public class MailConnection {
    private final String m_hostName;
    private final Session m_session;
    private final Transport m_transport;
    private final String m_authType;
    private final String m_username;
    private final String m_password;

    public MailConnection(String hostName, String authType, String username, String password) throws MessagingException {
        m_hostName = hostName;
        m_authType = authType;
        m_username = username;
        m_password = password;

        m_session = getMailSession(m_hostName, m_authType, m_username, m_password);
        m_transport = m_session.getTransport();
    }

    public MailConnection(String hostName) throws MessagingException {
        this(hostName, null, null, null);
    }

    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_SMTP_AUTH_MECHANISMS = "mail.smtp.auth.mechanisms";
    private static final String NTLM = "NTLM";
    private static final String SMTP = "smtp";
    private static final String MAIL_HOST = "mail.host";

    private static Session getMailSession(String smtpHost, String authType, String username, String password) throws MessagingException {
	final String l_username = username;
	final String l_password = password;

        Properties properties = System.getProperties();
        properties.setProperty(MAIL_TRANSPORT_PROTOCOL, SMTP);
        properties.setProperty(MAIL_HOST, smtpHost);

        javax.mail.Authenticator authenticator = null;

        if(l_username != null && l_username.trim() != "") {
            properties.setProperty(MAIL_SMTP_AUTH, "true");
            authenticator = new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(l_username, l_password);
                    }
                };

            if(authType != null && authType.trim() != "") {
                properties.setProperty(MAIL_SMTP_AUTH_MECHANISMS, authType);
            } 
        } else {
            properties.setProperty(MAIL_SMTP_AUTH, "false");
        }

        return Session.getInstance(properties, authenticator);
    }

    public String getHostName() {
        return m_hostName;
    }

    public MimeMessage createMessage() {
        return new MimeMessage(m_session);
    }

    public MimeMessage createMessage(InputStream content) throws MessagingException {
        return new MimeMessage(m_session, content);
    }

    public boolean isConnected() {
        return m_transport.isConnected();
    }

    public void ensureConnected() throws MessagingException {
        if (!isConnected()) m_transport.connect();
    }

    public void send(Message msg) throws MessagingException {
        m_transport.sendMessage(msg, msg.getAllRecipients());
    }

    public void close() throws MessagingException {
        m_transport.close();
    }
}
