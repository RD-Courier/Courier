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

import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.UrlSource;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;

/**
 * Created by IntelliJ IDEA.
 * User: AStepochkin
 * Date: 07.08.2006
 * Time: 11:52:33
 * To change this template use File | Settings | File Templates.
 */
public class UrlSourceFactory extends ReceiverFactory {
    private final String m_proxyHost;
    private final int m_proxyPort;
    private final int m_connectTimeout;
    private final int m_readTimeout;

    public UrlSourceFactory(CourierLogger logger, ObjectPoolIntf threadPool, Node conf) {
        super(logger, threadPool);
        m_proxyHost = DomHelper.getNodeAttr(conf, "proxy-host", null);
        if (m_proxyHost != null) {
            m_proxyPort = DomHelper.getIntNodeAttr(conf, "proxy-port");
        } else {
            m_proxyPort = 0;
        }
        m_connectTimeout = DomHelper.getIntNodeAttr(conf, "connect-timeout", 0);
        m_readTimeout = DomHelper.getIntNodeAttr(conf, "read-timeout", 0);
    }

    public Object getObject(ObjectPoolIntf pool) {
        return createSource();
    }

    public UrlSource createSource() {
        return new UrlSource(
            m_logger, m_threadPool,
            m_proxyHost, m_proxyPort,
            m_connectTimeout, m_readTimeout
        );
    }
}
