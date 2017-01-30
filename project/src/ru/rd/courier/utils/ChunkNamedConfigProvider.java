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
package ru.rd.courier.utils;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import ru.rd.pool.*;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.utils.Disposable;
import ru.rd.pool2.ObjectPool2;
import ru.rd.pool2.DefaultObjectPool2;
import ru.rd.pool2.AsynchExecutePolicy;
import ru.rd.thread.PoolExecutorAdapter;

import java.io.File;
import java.util.Timer;

/**
 * User: AStepochkin
 * Date: 22.11.2007
 * Time: 10:27:49
 */
public class ChunkNamedConfigProvider implements NamedConfigProvider, Disposable {
    private final static String c_PoolDesc = "Common Databases Config";

    private final CourierLogger m_logger;
    private final ObjectPoolIntf m_poolConf;
    private final String m_tagName;
    private final String m_attrName;

    public ChunkNamedConfigProvider(
        CourierLogger logger, Timer timer, ObjectPoolIntf threadPool, File storage, String tagName, String attrName
    ) throws PoolException {
        m_logger = logger;
        m_tagName = tagName;
        m_attrName = attrName;
        PoolObjectFactory pof = new SingletonPoolFactory(new XmlPoolObjectFactory(storage));
        ObjectPool2 poolConf;
        poolConf = new DefaultObjectPool2(logger, c_PoolDesc, pof);
        poolConf.setTimer(timer);
        poolConf.setExecutor(new PoolExecutorAdapter(threadPool));
        if (threadPool != null) {
            AsynchExecutePolicy execp = new AsynchExecutePolicy(logger, threadPool);
            execp.setAllocateTimeout(30*1000);
            execp.setCheckTimeout(10*1000);
            poolConf.setExecPolicy(execp);
        }
        poolConf.setShrinkPars(5*60*1000, -1, 5*50*1000);
        poolConf.start();
        m_poolConf = poolConf;
    }

    public Element getNamedConfig(String name) {
        Document conf = (Document)m_poolConf.getObject();
        try { m_poolConf.releaseObject(conf); } catch (Exception e) { m_logger.error(e); }
        return DomHelper.getChildInsensitive(conf.getDocumentElement(), m_tagName, m_attrName, name, false);
    }

    public void dispose() {
        try { m_poolConf.close(); } catch (Exception e) { m_logger.warning(e); }
    }
}
