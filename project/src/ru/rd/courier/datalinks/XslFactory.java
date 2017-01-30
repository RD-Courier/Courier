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
import ru.rd.courier.scripting.dataaccess.XslSource;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 03.03.2006
 * Time: 17:09:25
 */
public class XslFactory extends ReceiverFactory {
    private final String m_xsl;
    private final String m_varName;
    private final String m_schema;
    private final List<XslSource.XPathInfo> m_xpath;
    private final boolean m_preparedPaths;
    private final Map<String, String> m_namespaces;

    public XslFactory(CourierLogger logger, ObjectPoolIntf threadPool, Node conf) {
        super(logger, threadPool);
        m_xsl = DomHelper.getNodeAttr(conf, "xsl");
        m_varName = DomHelper.getNodeAttr(conf, "target-var");
        m_schema = DomHelper.getNodeAttr(conf, "schema", null);
        Node xpathConf = DomHelper.getChild(conf, "xpath-columns", false);
        if (xpathConf == null) {
            m_xpath = null;
            m_preparedPaths = false;
            m_namespaces = null;
        } else {
            m_xpath = xpathFromConf(xpathConf);
            m_preparedPaths = !DomHelper.getBoolYesNo(xpathConf, "not-prepared", false);
            Node nsConf = DomHelper.getChild(xpathConf, "namespaces", false);
            if (nsConf == null) {
                m_namespaces = null;
            } else {
                m_namespaces = DomHelper.getAttrParams(nsConf, new LinkedHashMap<String, String>());
            }
        }
    }

    private static List<XslSource.XPathInfo> xpathFromConf(Node conf) {
        List<XslSource.XPathInfo> res = new LinkedList<XslSource.XPathInfo>();
        for (Node pn: DomHelper.getChildrenByTagName(conf, "column", false)) {
            res.add(new XslSource.XPathInfo(
                DomHelper.getNodeAttr(pn, "name"),
                DomHelper.getNodeAttr(pn, "path")
            ));
        }
        return res;
    }

    public Object getObject(ObjectPoolIntf pool) {
        return new XslSource(
            m_logger, m_xsl, m_varName, m_schema,
            m_xpath, m_namespaces, m_preparedPaths
        );
    }
}
