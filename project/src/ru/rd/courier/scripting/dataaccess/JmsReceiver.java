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

import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringChecker;
import ru.rd.courier.utils.ReqExpChecker;
import ru.rd.courier.logging.CourierLogger;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.Context;
import java.util.List;
import java.util.regex.Pattern;

/**
 * User: AStepochkin
 * Date: 18.09.2007
 * Time: 19:26:12
 */
public class JmsReceiver extends TimedStringReceiver {
    private final CourierLogger m_logger;
    private final StringChecker m_lostConChecker;
    private boolean isValid;
    private Connection m_connection = null;
    Session m_session = null;
    MessageProducer m_sender = null;

    public JmsReceiver(
        CourierLogger logger, String lostConnRegex, ConnectionFactory factory, 
        Destination dest, boolean persistent, long timeToLive, int priority
    ) throws Exception {
        m_logger = logger;
        isValid = true;
        try {
            m_lostConChecker = new ReqExpChecker(lostConnRegex);
            m_connection = factory.createConnection();
            m_connection.setExceptionListener(new ExceptionListener(){
                public void onException(JMSException e) {
                    m_logger.error("JmsReceiver.onException: " + jmsErrorMessage(e), e);
                    if (m_lostConChecker.isTrue(e.getMessage())) cleanup();
                }
            });
            m_session = m_connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            m_sender = m_session.createProducer(dest);
            m_sender.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            m_sender.setTimeToLive(timeToLive);
            m_sender.setPriority(priority);
            m_connection.start();
        } catch (Exception e) {
            cleanup();
            throw e;
        }
    }

    static String jmsErrorMessage(JMSException e) {
        String linked = "";
        Exception le = e.getLinkedException();
        if (le != null) {
            linked = " LinkedError=" + le + ": " + le.getMessage();
        }
        return "ErrorCode=" + e.getErrorCode() + linked;
    }

    public boolean isValid() {
        synchronized(this) {
            return isValid;
        }
    }

    private void cleanup() {
        synchronized(this) {
            if (!isValid) return;
            isValid = false;
        }

        if (m_sender != null) {
            try { m_sender.close(); } catch (Exception e) { m_logger.warning(e); }
            m_sender = null;
        }
        if (m_session != null) {
            try { m_session.close(); } catch (Exception e) { m_logger.warning(e); }
            m_session = null;
        }
        if (m_connection != null) {
            try { m_connection.close(); } catch (Exception e) { m_logger.warning(e); }
            m_connection = null;
        }
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        try {
            TextMessage message = m_session.createTextMessage();
            message.setText(operation);
            m_sender.send(message);
        } catch (JMSException e) {
            throw new RuntimeException(jmsErrorMessage(e), e);
        }

        return null;
    }

    protected List<LinkWarning> timedFlush() {
        return null;
    }

    protected void timedClose() throws CourierException {
        cleanup();
    }

    public void setTimeout(int timeout) throws CourierException {
    }

    public void cancel() throws CourierException {
    }
}
