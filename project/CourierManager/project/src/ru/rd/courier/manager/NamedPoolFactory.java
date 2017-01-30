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
package ru.rd.courier.manager;

import org.w3c.dom.Element;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.NamedConfigProvider;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Astepochkin
 * Date: 30.09.2008
 * Time: 17:23:32
 */
public abstract class NamedPoolFactory extends PoolInitializer {
    private boolean m_caseInsensitive;
    private final NamedConfigProvider m_conf;
    private final Map<String, ObjectPoolIntf> m_pools = new HashMap<String, ObjectPoolIntf>();

    public NamedPoolFactory(
        CourierLogger logger, ObjectPoolIntf threadPool, NamedConfigProvider conf
    ) {
        super(logger, threadPool);
        m_conf = conf;
        m_caseInsensitive = true;
    }

    public final void setCaseInsensitive(boolean value) {
        m_caseInsensitive = value;
    }

    public void start() {
        for (ObjectPoolIntf pool: m_pools.values()) {
            pool.start();
        }
    }

    public void stop() {
        for (ObjectPoolIntf pool: m_pools.values()) {
            pool.close();
        }
    }

    public final ObjectPoolIntf getPool(String name) throws Exception {
        if (m_caseInsensitive) name = name.toUpperCase();
        ObjectPoolIntf op = m_pools.get(name);
        if (op == null) {
            Element conf = m_conf.getNamedConfig(name);
            if (conf == null) {
                throw new RuntimeException(
                    "Configuration for pool '" + name + "' not found");
            }
            initPool(name, conf);
        }
        return op;
    }

    public final ObjectPoolIntf initPool(String name, Element conf) throws Exception {
        ObjectPoolIntf op = asynchPoolFromXml(createFactory(name, conf), name, conf);
        m_pools.put(name, op);
        return op;
    }

    protected abstract PoolObjectFactory createFactory(String name, Element conf) throws Exception;

    public void dispose() {
        for (ObjectPoolIntf pool: m_pools.values()) {
            try { pool.close(); } catch (Exception e) { m_logger.warning(e); }
        }
    }
}
