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

import org.apache.axis.components.net.SocketFactoryFactory;
import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.utils.Disposable;

import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 20.12.2005
 * Time: 13:33:00
 */
public class SoapFactory extends ReceiverFactory implements PoolObjectFactory, Disposable {
    public SoapFactory(CourierLogger logger, ObjectPoolIntf threadPool, Node conf) {
        super(logger, threadPool);
    }

    public Object getObject(ObjectPoolIntf pool) {
        return null;
    }

    public void dispose() {
        try { SocketFactoryFactory.closeFactory("http"); }
        catch(IOException e) { m_logger.warning(e); }

        try { SocketFactoryFactory.closeFactory("https"); }
        catch(IOException e) { m_logger.warning(e); }
    }

    public String getDesc() {
        return null;
    }
}
