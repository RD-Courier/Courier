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
package ru.rd.courier;

import ru.rd.courier.scripting.AbstractContext;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.logging.CourierLogger;

import java.text.DateFormat;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 28.11.2007
 * Time: 17:01:10
 */
class ScriptContext extends AbstractContext {
    private final Application m_courier;
    private final CourierLogger m_logger;

    public ScriptContext(Application courier, DateFormat dateFormat) throws CourierException {
        super(courier.getLogger(), dateFormat);
        m_courier = courier;
        m_logger = m_courier.getLogger();
    }

    public void addDbWarning(List<LinkWarning> warnings) throws CourierException {
        if (warnings == null) return;
        for (LinkWarning w: warnings) {
            m_logger.warning(w.getException().getMessage());
        }
    }

    private void initDbLink(String dbName) {
        m_pooledObjects.put(dbName, m_courier.getPooledObject(dbName));
    }

    protected void initDataSource(String dbName) throws CourierException {
        initDbLink(dbName);
    }

    protected void initDataReceiver(String dbName) throws CourierException {
        initDbLink(dbName);
    }

    protected void initPooledObject(String name) throws CourierException {
        initDbLink(name);
    }
}
