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
import ru.rd.courier.scripting.dataaccess.JSONPathSource;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 27.06.2006
 * Time: 13:16:20
 */
public class JSONPathSourceFactory extends ReceiverFactory {
    private final String m_recordSelector;
    private final List<JSONPathSource.ColumnSelectInfo> m_cols;
    private final String m_encoding;

    public JSONPathSourceFactory(
        CourierLogger logger, ObjectPoolIntf threadPool, Node conf
    ) {
        super(logger, threadPool);
        m_recordSelector = DomHelper.getChildValue(conf, "records-path");
        m_encoding = DomHelper.getNodeAttr(conf, "encoding", null);
        m_cols = new LinkedList<JSONPathSource.ColumnSelectInfo>();

        Node colsNode = DomHelper.getChild(conf, "columns", true);
        for (Node cn: DomHelper.getChildrenByTagName(colsNode, "column")) {
            String path = DomHelper.getNodeValue(cn);
            m_cols.add(new JSONPathSource.ColumnSelectInfo(DomHelper.getNodeAttr(cn, "name", true), path));
        }
    }

    public Object getObject(ObjectPoolIntf pool) {
        return createSource();
    }

    public final JSONPathSource createSource() {
        JSONPathSource r = new JSONPathSource(m_logger, m_recordSelector, m_cols);
        r.setEncoding(m_encoding);
        return r;
    }
}
