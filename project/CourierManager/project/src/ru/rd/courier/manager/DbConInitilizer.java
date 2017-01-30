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
import org.w3c.dom.Node;
import ru.rd.courier.datalinks.JdbcSourceFactory;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.NamedConfigProvider;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolException;
import ru.rd.pool.PoolObjectFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Astepochkin
 * Date: 30.09.2008
 * Time: 14:35:01
 */
public class DbConInitilizer extends NamedPoolFactory {
    private Map<String, String> m_receiverTypes;
    private Map<String, Map<String, String>> m_driverInfos;
    private String m_host = null;
    private String m_programName = null;

    public DbConInitilizer(CourierLogger logger, ObjectPoolIntf threadPool, NamedConfigProvider conf) {
        super(logger, threadPool, conf);
    }

    public void setTypes(Map<String, String> types) {
        m_receiverTypes = types;
    }

    public void setDriverInfo(Map<String, Map<String, String>> driverInfo) {
        m_driverInfos = driverInfo;
    }

    public static Map<String, String> confTypes(Element n) {
        Map<String, String> ret = new HashMap<String, String>();
        for (Element n1 : DomHelper.getChildrenByTagName(n, "type", false)) {
            ret.put(
                DomHelper.getNodeAttr(n1, "name"),
                DomHelper.getNodeAttr(n1, "class")
            );
        }
        return ret;
    }

    public static Map<String, Map<String, String>> confDriverInfo(Node n) {
        Map<String, Map<String, String>> ret = new HashMap<String, Map<String, String>>();
        Element[] nl = DomHelper.getChildrenByTagName(n, "driver", false);
        if (nl != null) {
            for (Element node : nl) {
                ret.put(
                    DomHelper.getNodeAttr(node, "class"),
                    DomHelper.getAllParamMap(node)
                );
            }
        }
        return ret;
    }

    private static final String s_datalinksPackage = "ru.rd.courier.datalinks";

    protected PoolObjectFactory createFactory(String name, Element conf) throws Exception{
        final String tagName = conf.getTagName();
        PoolObjectFactory pof;
        if (tagName.equals("database")) {
            pof = initJdbcSourcePool(name, conf);
        } else {
            final Class cl = getDbClass(conf);
            //try {
            //    Constructor cntr = cl.getConstructor(CourierLogger.class, ObjectPoolIntf.class, Node.class);
            //    pof = (PoolObjectFactory) cntr.newInstance(m_logger, getThreadPool(), n);
            //} catch (NoSuchMethodException e) {
                Constructor cntr = cl.getConstructor(CourierLogger.class, Node.class);
                pof = (PoolObjectFactory) cntr.newInstance(m_logger, conf);
            //}
        }
        return pof;
    }

    public final ObjectPoolIntf initDbProfile(Element n) throws Exception {
        final String name = DomHelper.getNodeAttr(n, "name");
        return initPool(name, n);
    }

    private Class getDbClass(Element n) throws ClassNotFoundException {
        String className = DomHelper.getNodeAttr(n, "class", null);
        if (className == null) {
            if (m_receiverTypes == null) throw new RuntimeException("Class not specified");
            final String type = DomHelper.getNodeAttr(n, "type");
            className = m_receiverTypes.get(type);
            if (className == null) {
                throw new RuntimeException("Class name for pool type '" + type + "' not specified");
            }
            className = s_datalinksPackage + "." + className;
        }
        return Class.forName(className);
    }

    private PoolObjectFactory initJdbcSourcePool(String dbName, final Node n) throws PoolException {
        return new JdbcSourceFactory(m_logger, n, dbName, m_host, m_programName, m_driverInfos, null);
    }
}
