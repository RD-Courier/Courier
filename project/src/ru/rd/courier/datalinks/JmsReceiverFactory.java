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
package ru.rd.courier.datalinks;

import ru.rd.pool.ObjectPoolIntf;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.scripting.dataaccess.JmsReceiver;
import org.w3c.dom.Node;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * User: AStepochkin
 * Date: 19.09.2007
 * Time: 11:56:18
 */
public class JmsReceiverFactory extends ReceiverFactory {
    private final String m_lostConnRegex;
    private final String m_contextFactory;
    private final String m_providerUrl;
    private final String m_factoryName;
    private final String m_destName;
    private ConnectionFactory m_factory;
    private Destination m_dest;
    private final boolean m_persistent;
    private final long m_timeToLive;
    private final int m_priority;

    public static synchronized Context confContext(String contextFactory, String providerUrl) throws NamingException {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        props.put(Context.PROVIDER_URL, providerUrl);
        return new InitialContext(props);
    }

    public static Context confContext(Node conf) throws NamingException {
        return confContext(
            DomHelper.getNodeAttr(conf, "context-factory", true),
            DomHelper.getNodeAttr(conf, "provider-url", true)
        );
    }

    public JmsReceiverFactory(CourierLogger logger, Node conf) {
        super(logger, null);

        m_lostConnRegex = DomHelper.getNodeAttr(conf, "lost-connection-regex", true);
        m_contextFactory = DomHelper.getNodeAttr(conf, "context-factory", true);
        m_providerUrl = DomHelper.getNodeAttr(conf, "provider-url", true);
        m_factoryName = DomHelper.getNodeAttr(conf, "factory-name", true);
        m_destName = DomHelper.getNodeAttr(conf, "destination", true);
        m_persistent = DomHelper.getBoolYesNo(conf, "persistent", true);
        m_timeToLive = DomHelper.getLongNodeAttr(conf, "time-to-live", 0);
        m_priority = DomHelper.getIntNodeAttr(conf, "priority", 0);
        if (m_priority > 9) throw new RuntimeException("Priority " + m_priority + " should be < 10");
        m_factory = null;
        m_dest = null;
    }

    private void delayedInit() throws NamingException {
        if (m_factory != null) return;
        Context context = confContext(m_contextFactory, m_providerUrl);
        try {
            m_factory = (ConnectionFactory) context.lookup(m_factoryName);
            m_dest = (Destination) context.lookup(m_destName);
        } finally {
            try { context.close(); } catch (Exception e1) { m_logger.warning(e1); }
        }
    }

    public String getDesc() {
        return getClass().getName();
    }

    public Object getObject(ObjectPoolIntf pool) {
        try {
            delayedInit();
            return new JmsReceiver(
                m_logger, m_lostConnRegex, m_factory, m_dest, m_persistent, m_timeToLive, m_priority
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkObject(Object o) {
        JmsReceiver jms = (JmsReceiver)o;
        return jms.isValid();
    }

}
