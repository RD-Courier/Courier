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
package ru.rd.courier.scripting.dataaccess;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.apache.xml.utils.PrefixResolver;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

import ru.rd.courier.jdbc.ResultSets.StringBufferedResultSet;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.logging.CourierLogger;

/**
 * User: Astepochkin
 * Date: 28.06.2006
 * Time: 17:27:47
 */
public class XPathAllDataSource extends XPathSourceBase {
    private final String[] m_addCols;

    public XPathAllDataSource(
        CourierLogger logger, String recordSelector,
        Map<String, String> nsPrefixes, String[] addCols
    ) {
        super(logger, recordSelector, nsPrefixes);
        m_addCols = addCols;
    }

    protected ResultSet innerRequest(final NodeIterator nl, PrefixResolver prefixResolver) {
        return new DomResultSet(null, m_addCols, nl, "");
    }

    private static class DomResultSet extends StringBufferedResultSet {
        private Element m_curRecord;
        private final String m_colPrefix;
        private static final String m_nameSeparator = "/";
        private NodeIterator m_data;

        private DomResultSet(
            Statement stmt, String[] addCols, Node firstNode, NodeIterator data, String colPrefix
        ) {
            super(stmt, collectColumns((Element)firstNode, addCols, colPrefix));
            m_colPrefix = colPrefix;
            m_data = data;
            m_curRecord = (Element)firstNode;
        }

        public DomResultSet(Statement stmt, String[] addCols, NodeIterator data, String colPrefix) {
            this(stmt, addCols, data.nextNode(), data, colPrefix);
        }

        private static IterColumnInfo[] collectColumns(
            Element data, String[] addCols, String colPrefix
        ) {
            Map<String, IterColumnInfo> cols = new HashMap<String, IterColumnInfo>();
            if (data != null) collectColumns(cols, colPrefix, data);
            if (addCols != null) {
                for (String colName: addCols) {
                    if (!cols.containsKey(colName)) {
                        cols.put(colName, new IterColumnInfo(colName, 0));
                    }
                }
            }
            return cols.values().toArray(new IterColumnInfo[cols.size()]);
        }

        private static String buildName(String prefix, Node n) {
            String name = (n.getLocalName() == null) ? n.getNodeName() : n.getLocalName();
            return prefix + (prefix.length() > 0 ? m_nameSeparator : "") + name;
        }

        private static int collectColumns(
            Map<String, IterColumnInfo> columns, String namePrefix, Node data
        ) {
            int ret = 0;
            Node n = data.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    String name = buildName(namePrefix, n);
                    if (!columns.containsKey(name)) {
                        if (collectColumns(columns, name, n) == 0) columns.put(
                            name, new IterColumnInfo(name, 0)
                        );
                    }
                    ret++;
                }
                n = n.getNextSibling();
            }
            return ret;
        }

        private int collectData(String namePrefix, Node data) throws SQLException {
            int ret = 0;
            Node n = data.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    String name = buildName(namePrefix, n);
                    if (collectData(name, n) == 0) {
                        updateString(name, DomHelper.getNodeValue(n));
                    }
                    ret++;
                }
                n = n.getNextSibling();
            }
            return ret;
        }

        protected boolean getRecord() throws SQLException {
            if (m_curRecord == null) return false;
            collectData(m_colPrefix, m_curRecord);
            nextRecord();
            return true;
        }

        protected int skipRecords(int count) throws SQLException {
            int ret = 0;
            while (m_curRecord != null) {
                nextRecord();
                ret++;
            }
            return ret;
        }

        private void nextRecord() {
            m_curRecord = (Element)m_data.nextNode();
        }
    }
}
