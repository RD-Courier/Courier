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
package ru.rd.courier.scripting.dataaccess;

import javax.mail.*;
import javax.mail.event.TransportEvent;

import java.util.List;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 11.07.2005
 * Time: 11:23:06
 */
public class TestTransport extends Transport {
    private static List<Message> m_messages = new LinkedList<Message>();

    private static synchronized void addMessage(Message msg) {
        m_messages.add(msg);
    }

    public static synchronized void clearMessages() {
        m_messages.clear();
    }

    public static synchronized List<Message> getMessages() {
        return m_messages;
    }

    public TestTransport(Session session, URLName urlname) {
        super(session, urlname);
    }

    public void sendMessage(Message msg, Address[] addresses) throws MessagingException {
        addMessage(msg);
        notifyTransportListeners(
            TransportEvent.MESSAGE_DELIVERED,
            addresses, new Address[0], new Address[0], msg
        );
    }

    protected boolean protocolConnect(
        String host, int port, String user, String password
    ) throws MessagingException {
        return true;
    }

}
