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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.XPathAllDataSource;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 03.07.2006
 * Time: 13:03:02
 */
public class XmlSourceFactory extends ReceiverFactory {
    private final String m_recordSelector;
    private final String[] m_cols;
    private final Map<String, String> m_nsPrefixes;

    public XmlSourceFactory(CourierLogger logger, ObjectPoolIntf threadPool, Node conf) {
        super(logger, threadPool);
        m_recordSelector = DomHelper.getChildValue(conf, "records-path");
        List<String> cols = new LinkedList<String>();
        Element optionalCols = DomHelper.getChild(conf, "optional-columns", false);
        if (optionalCols == null) {
            m_cols = null;
        } else {
            for (Node cn: DomHelper.getChildrenByTagName(optionalCols, "column")) {
                cols.add(DomHelper.getNodeAttr(cn, "name", true));
            }
            m_cols = cols.toArray(new String[cols.size()]);
        }
        m_nsPrefixes = new HashMap<String, String>();
        Element namespaces = DomHelper.getChild(conf, "namespaces", false);
        if (namespaces != null) DomHelper.getAttrParams(namespaces, m_nsPrefixes);
    }

    public Object getObject(ObjectPoolIntf pool) {
        return new XPathAllDataSource(m_logger, m_recordSelector, m_nsPrefixes, m_cols);
    }
}
