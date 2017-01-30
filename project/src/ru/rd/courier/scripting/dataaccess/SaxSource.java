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

import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.ResultSets.StringBufferedResultSet;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.utils.DomHelper;

import javax.xml.transform.TransformerException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.io.File;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.apache.xpath.XPathAPI;

/**
 * User: AStepochkin
 * Date: 27.06.2006
 * Time: 17:33:38
 */
public class SaxSource extends TimedStringReceiver implements DataSource {
    private final String m_recordSelector;
    private final List<ColumnSelectInfo> m_cols;

    public static class ColumnSelectInfo {
        public final String m_name;
        public final String m_path;

        public ColumnSelectInfo(String name, String path) {
            m_name = name;
            m_path = path;
        }
    }

    public SaxSource(String recordSelector, List<ColumnSelectInfo> cols) {
        m_recordSelector = recordSelector;
        m_cols = cols;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        return null;
    }

    protected List<LinkWarning> timedFlush() {
        return null;
    }

    protected void timedClose() throws CourierException {}
    public void setTimeout(int timeout) throws CourierException {}
    public void cancel() throws CourierException {}

    public ResultSet request(String query) throws CourierException {
        try {
            return innerFileRequest(query);
        } catch(Exception e) {
            throw new CourierException(e);
        }
    }

    private ResultSet innerFileRequest(String query)
    throws TransformerException, IOException, ParserConfigurationException, SAXException {
        return innerRequest(DomHelper.parseXmlFile(new File(query)));
    }

    private ResultSet innerRequest(Document doc)
            throws TransformerException, ParserConfigurationException, SAXException {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        //parser.getXMLReader().

        final NodeIterator nl = XPathAPI.selectNodeIterator(doc, m_recordSelector);

        IterColumnInfo[] infos = new IterColumnInfo[m_cols.size()];
        int i = 0;
        for (ColumnSelectInfo ci: m_cols) {
            infos[i] = new IterColumnInfo(ci.m_name, 0);
            i++;
        }

        StringBufferedResultSet rs = new StringBufferedResultSet(null, infos) {
            protected boolean getRecord() throws SQLException {
                Node rn = nl.nextNode();
                if (rn == null) return false;

                int i = 1;
                for (ColumnSelectInfo ci: m_cols) {
                    Node cn;
                    try {
                        cn = XPathAPI.selectSingleNode(rn, ci.m_path);
                    } catch (TransformerException e) {
                        SQLException sqle = new SQLException();
                        sqle.initCause(e);
                        throw sqle;
                    }
                    updateString(i, DomHelper.getNodeValue(cn));
                    i++;
                }
                return true;
            }

            protected int skipRecords(int count) {
                int c = 0;
                while (nl.nextNode()!= null) c++;
                return c;
            }
        };
        return rs;
    }
}
