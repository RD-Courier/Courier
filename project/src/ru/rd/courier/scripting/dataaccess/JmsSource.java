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

import org.w3c.dom.Node;
import ru.rd.courier.StringHandler;
import ru.rd.courier.TransferProcessResult;
import ru.rd.courier.TransferResultListener;
import ru.rd.courier.datalinks.JmsReceiverFactory;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.ReqExpChecker;
import ru.rd.courier.utils.StringChecker;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.scheduling.leveled.StartStopListener;
import ru.rd.utils.InvokeAllRunnable;
import ru.rd.utils.KeepAlive;

import javax.jms.*;
import javax.naming.Context;
import java.util.Date;
import java.util.Timer;

/**
 * User: AStepochkin
 * Date: 24.09.2007
 * Time: 17:18:54
 */
public class JmsSource extends KeepAlive implements StartStopListener {
    private final StringChecker m_lostConChecker;
    private final StringHandler m_handler;
    private final ResultListener m_resHandler;
    private final String m_contextFactory;
    private final String m_providerUrl;
    private final String m_factoryName;
    private final String m_dest;
    private final String m_desc;
    private final int m_maxMessagesCount;
    private Destination m_destObj;
    private Connection m_connection = null;
    private Session m_session = null;
    private MessageConsumer m_receiver = null;
    private int m_mesCount;
    private int m_mesProcCount = 0;
    private boolean m_suspended;

    public JmsSource(CourierLogger logger, ObjectPoolIntf threadPool, Timer timer, Node conf, StringHandler handler) {
        super(
            logger, threadPool, timer,
            DomHelper.getTimeNodeAttr(conf, "check-interval", 60*1000),
            DomHelper.getTimeNodeAttr(conf, "start-stop-timeout", 10*1000)
        );
        m_handler = handler;
        m_resHandler = new ResultListener();
        m_lostConChecker = new ReqExpChecker(DomHelper.getNodeAttr(conf, "lost-connection-regex", true));
        m_contextFactory = DomHelper.getNodeAttr(conf, "context-factory", true);
        m_providerUrl = DomHelper.getNodeAttr(conf, "provider-url", true);
        m_factoryName = DomHelper.getNodeAttr(conf, "factory-name", true);
        m_dest = DomHelper.getNodeAttr(conf, "destination", true);
        m_destObj = null;
        m_desc = getClass().getSimpleName() + " (" + m_dest + ")";
        m_maxMessagesCount = DomHelper.getIntNodeAttr(conf, "max-message-count", 0);
        m_mesCount = 0;
        m_suspended = false;
        setState(c_stateStopped);
    }

    public String getDesc() {
        return m_desc;
    }

    public void start(Date parentStart) {
        start();
    }

    private class ResultListener implements TransferResultListener {
        public void transferFinished(TransferProcessResult result) {
            boolean needResume = false;
            synchronized(lock) {
                m_mesCount--;
                needResume = m_maxMessagesCount > 0 && m_mesCount <= m_maxMessagesCount / 2;
            }
            if (needResume) resumeCheck();
        }
    }

    private void mdebug(Message message, String mes) throws JMSException {
        debug(
            "onMessage " + message.getJMSMessageID() +
            " Count=" + m_mesCount + (mes == null ? "" : ": " + mes)
        );
    }

    private class InnerMessageListener implements MessageListener {
        public void onMessage(Message message) {
            try {
                synchronized(lock) {
                    if (m_state != c_stateStarted) {
                        mdebug(message, "state is not 'Started' = " + m_state);
                        return;
                    }
                    //if (m_suspended) {
                    //    mdebug(message, "suspended");
                    //    return;
                    //}
                    m_mesCount++;
                }
                mdebug(message, null);
                if (m_maxMessagesCount > 0 && m_mesCount >= m_maxMessagesCount) {
                    suspend();
                }
                m_mesProcCount++;
                boolean handleRes = m_handler.handle(((TextMessage)message).getText(), m_resHandler);
                m_mesProcCount--;
                if (handleRes) {
                    message.acknowledge();
                } else {
                    synchronized(lock) {
                        m_mesCount--;
                    }
                    mdebug(message, "Message not registered");
                }
            } catch (JMSException e) {
                m_logger.error("JmsSource.onMessage: " + JmsReceiver.jmsErrorMessage(e), e);
            }
        }
    }

    private void suspend() {
        //debug("suspend request Count = " + m_mesCount + " HandleCount = " + m_mesProcCount);
        MessageConsumer rec;
        synchronized(lock) {
            if (m_receiver == null || m_suspended) return;
            rec = m_receiver;
            m_receiver = null;
            m_suspended = true;
        }
        debug("suspend Count = " + m_mesCount + " HandleCount = " + m_mesProcCount);
        InvokeAllRunnable cw = new InvokeAllRunnable(m_logger, m_desc + " suspender", true);
        cw.add(rec, MessageConsumer.class, "close");
        m_threads.exec(cw, m_timeout, m_logger);
    }

    private void resumeCheck() {
        //debug("resume request Count = " + m_mesCount + " HandleCount = " + m_mesProcCount);
        boolean err;
        synchronized(lock) {
            if (!m_suspended) return;
            m_suspended = false;
            err = (m_receiver != null) || (m_connection == null);
        }
        if (err) {
            invalidate();
            return;
        }
        m_threads.exec(
            new Runnable() {
                public void run() {
                    try {
                        resume();
                    } catch (Exception e) {
                        m_logger.warning(e);
                        invalidate();
                    }
                }
            },
            m_timeout, m_logger
        );
    }

    private void resume() {
        debug("resume Count = " + m_mesCount + " HandleCount = " + m_mesProcCount);
        try {
            initReceiver();
        } catch (Exception e) {
            m_logger.warning(e);
            invalidate();
        }
    }

    private void initReceiver() throws JMSException {
        m_receiver = m_session.createConsumer(m_destObj);
        m_receiver.setMessageListener(new InnerMessageListener());
    }

    protected void init() throws Exception {
        Context context = null;
        try {
            context = JmsReceiverFactory.confContext(m_contextFactory, m_providerUrl);
            ConnectionFactory factory = (ConnectionFactory) context.lookup(m_factoryName);
            m_destObj = (Destination) context.lookup(m_dest);
            m_connection = factory.createConnection();
            m_connection.setExceptionListener(new ExceptionListener(){
                public void onException(JMSException e) {
                    m_logger.error("JmsSource.onException: " + JmsReceiver.jmsErrorMessage(e), e);
                    if (m_lostConChecker.isTrue(e.getMessage())) invalidate();
                }
            });
            m_session = m_connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            initReceiver();
            m_connection.start();
        } finally {
            if (context != null) {
                try { context.close(); } catch (Exception e) { m_logger.warning(e); }
            }
        }
    }

    protected Runnable getCleanupWork() {
        if (m_connection == null) return null;
        InvokeAllRunnable cw = new InvokeAllRunnable(m_logger, m_desc + " cleaner");
        if (m_receiver != null) cw.add(m_receiver, MessageConsumer.class, "close");
        if (m_session != null) cw.add(m_session, Session.class, "close");
        if (m_connection != null) cw.add(m_connection, Connection.class, "close");
        m_receiver = null;
        m_session = null;
        m_connection = null;
        m_destObj = null;
        return cw;
    }
}
