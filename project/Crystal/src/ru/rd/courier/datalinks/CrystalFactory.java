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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.scripting.dataaccess.CrystalDataSource;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.ObjectPoolIntf;
import org.w3c.dom.Node;

/**
 * User: Andrew A.Toschev
 * Date: Apr 13, 2006 Time: 3:37:11 PM
 * Description:
 */
public class CrystalFactory  implements PoolObjectFactory {
    private final CourierLogger m_logger;
    private final String m_host;
    private final String m_authentication;
    private final String m_username;
    private final String m_password;

    public CrystalFactory(CourierLogger logger, Node conf) {
        m_logger = logger;
        m_host = DomHelper.getNodeAttr(conf, "host");
        m_authentication = DomHelper.getNodeAttr(conf, "auth");
        m_username = DomHelper.getNodeAttr(conf, "username");
        m_password = DomHelper.getNodeAttr(conf, "password");
    }

    public Object getObject(ObjectPoolIntf objectPool) {
        try {
            return new CrystalDataSource(m_logger, m_username, m_password, m_host, m_authentication);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void returnObject(Object o) {
        ((CrystalDataSource)o).closeConnect();
    }

    public boolean checkObject(Object o) {
        return ((CrystalDataSource)o).checkConnect();
    }
}
