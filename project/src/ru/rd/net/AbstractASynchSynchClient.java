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
package ru.rd.net;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoSession;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.thread.Condition;
import ru.rd.thread.ThreadHelper;

/**
 * User: AStepochkin
 * Date: 08.10.2008
 * Time: 16:00:07
 */
public class AbstractASynchSynchClient {
    protected final CourierLogger m_logger;
    protected final Object m_lock;
    protected final Condition m_recCondition;
    protected IoSession m_session;
    protected CloseFuture m_closeFuture;
    protected Object m_result;

    public AbstractASynchSynchClient(CourierLogger logger, IoSession session) {
        m_logger = logger;
        m_session = session;
        m_lock = new Object();
        m_recCondition = new Condition() {
            public boolean isTrue() {
                return m_result != null || !getSession().isConnected();
            }
        };
        m_closeFuture = null;
    }

    private boolean m_waiting = false;

    public final void send(Object message) {
        synchronized(m_lock) {
            getSession().write(message);
        }
    }

    public final Object write(Object message, long timeout) throws InterruptedException {
        synchronized(m_lock) {
            m_result = null;
            //m_logger.debug("AsynchWrite: message = " + message + " Lock = " + m_lock);
            getSession().write(message);
            m_waiting = true;
            try {
                if (!ThreadHelper.waitEvent(m_lock, m_recCondition, timeout)) {
                    throw new RuntimeException("Timeout (" + timeout + ") expired");
                }
            } finally {
                m_waiting = false;
            }
            if (m_result == null) {
                throw new RuntimeException(
                    "AsynchWrite returned null (SessionClosed=" + getSession().isConnected() + ")"
                );
            }
            Object res = m_result;
            m_result = null;
            return res;
        }
    }

    public void resultReceived(Object result) throws Exception {
        synchronized(m_lock) {
            //m_logger.debug("AbstractASynchSynchClient.resultReceived: waiting = " + m_waiting + " Message = " + result + " Lock = " + m_lock);
            m_result = result;
            m_lock.notifyAll();
        }
    }

    public void sessionClosed() throws Exception {
        synchronized(m_lock) {
            m_lock.notifyAll();
        }
    }

    public boolean close(long timeout) {
        synchronized(m_lock) {
            if (m_closeFuture == null) {
                m_closeFuture = getSession().close();
            }
        }
        return m_closeFuture.join(timeout);
    }

    public boolean isValid() {
        return getSession().isConnected();
    }

    protected final IoSession getSession() {
        return m_session;
    }

    protected final void setSession(IoSession session) {
        m_session = session;
    }
}
