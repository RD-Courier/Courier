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
package ru.rd.courier.jdbc.soap;

import ru.rd.courier.jdbc.ConnectionSkeleton;
import ru.rd.courier.jdbc.ConnectionDrivenJdbcStatement;
import ru.rd.courier.jdbc.ResultSets.StringBufferedResultSet;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.utils.DomHelper;

import java.util.*;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.axis.MessageContext;
import org.apache.axis.AxisProperties;
import org.apache.axis.components.net.SocketFactory;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.transport.http.HTTPConstants;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.parsers.ParserConfigurationException;
import javax.wsdl.Port;

/**
 * User: AStepochkin
 * Date: 19.12.2005
 * Time: 15:05:54
 */
public class SoapConnection extends ConnectionSkeleton {
    private static volatile int s_lastConNumber = 0;
    private final Parser m_wsdlParser = new Parser();

    static {
        AxisProperties.setClassDefault(
            SocketFactory.class, "ru.rd.axis.PoolSocketFactory"
        );
    }

    public SoapConnection(
        String wsdlUri,
        QName serviceName, String portName, QName operationName
    ) throws SQLException {
        super(++s_lastConNumber);
        try {
            m_wsdlParser.run(wsdlUri);
        } catch (Exception e) {
            SQLException te = new SQLException(e.getMessage());
            te.initCause(e);
            throw te;
        }
        if (operationName != null) {
            createCall(serviceName, portName, operationName);
        }
    }

    private SymTabEntry getSymTabEntry(QName qname, Class cls) {
        HashMap map = m_wsdlParser.getSymbolTable().getHashMap();

        for (Object o : map.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            //QName key = (QName) entry.getKey();
            Vector v = (Vector) entry.getValue();

            if ((qname == null) || qname.equals(qname)) {
                for (int i = 0; i < v.size(); ++i) {
                    SymTabEntry symTabEntry = (SymTabEntry) v.elementAt(i);

                    if (cls.isInstance(symTabEntry)) {
                        return symTabEntry;
                    }
                }
            }
        }
        return null;
    }

    private Call createCall(
        QName serviceName, String portName, QName operationName
    ) throws SQLException {
        if (operationName == null) throw new SQLException("Operation name not specified");
        try {
            ServiceEntry serviceEntry;
            if ((serviceName == null) || (serviceName.getLocalPart() == null)) {
                serviceEntry = (ServiceEntry) getSymTabEntry(
                    null, ServiceEntry.class
                );
            } else {
                serviceEntry = m_wsdlParser.getSymbolTable().getServiceEntry(serviceName);
            }
            org.apache.axis.client.Service dpf = new org.apache.axis.client.Service(
                m_wsdlParser, serviceEntry.getQName()
            );

            if (portName == null) {
                Port port = null;
                Map ports = serviceEntry.getService().getPorts();
                if (ports.size() != 1) throw new SQLException("Port name not specified");
                for (Map.Entry e: (Set<Map.Entry>)ports.entrySet()) {
                    port = (Port)e.getValue();
                }
                portName = port.getName();
            }

            Call call = dpf.createCall(QName.valueOf(portName), operationName);
            call.setProperty(
                MessageContext.HTTP_TRANSPORT_VERSION, HTTPConstants.HEADER_PROTOCOL_V11
            );
            return call;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String cServiceName = "service-name";
    private static final String cServiceUri = "service-uri";
    private static final String cPortName = "port-name";
    private static final String cOperationName = "operation-name";
    private static final String cOperationUri = "operation-uri";

    private Element parseXml(String xml)
    throws IOException, ParserConfigurationException, SAXException {
        return DomHelper.parseString(
            "<?xml version=\"1.0\" encoding=\"windows-1251\" ?> " + xml
        ).getDocumentElement();
    }

    protected List innerRequest(ConnectionDrivenJdbcStatement stmt, String request) throws SQLException {
        List res = new LinkedList();
        try {
            Element rd = parseXml(request);
            if (!rd.hasAttribute(cOperationName)) {
                throw new SQLException("OperationName is not specified");
            }
            Call call = createCall(
                new QName(rd.getAttribute(cServiceUri), rd.getAttribute(cServiceName)),
                rd.getAttribute(cPortName),
                new QName(rd.getAttribute(cOperationUri), rd.getAttribute(cOperationName))
            );
            Object ret = call.invoke(new Object[] {request});
            if (ret != null) {
                if (!(ret instanceof String)) throw new SQLException("Return type not String");
                res.add(xmlToResultSet(stmt, parseXml((String)ret)));
            }

        } catch (Exception e) {
            SQLException te = new SQLException(e.getMessage());
            te.initCause(e);
            throw te;
        }
        return res;
    }

    private ResultSet xmlToResultSet(ConnectionDrivenJdbcStatement stmt, Element data) {
        return new SoapResultSet(stmt, data, "");
    }

    private static class SoapResultSet extends StringBufferedResultSet {
        private Element m_curRecord;
        private final String m_colPrefix;
        private static final String m_nameSeparator = "/";

        public SoapResultSet(Statement stmt, Element data, String colPrefix) {
            super(stmt, collectColumns(data, colPrefix));
            m_colPrefix = colPrefix;
            Node n = data.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.ELEMENT_NODE) break;
                n = n.getNextSibling();
            }
            m_curRecord = (Element)n;
        }

        private static IterColumnInfo[] collectColumns(Element data, String colPrefix) {
            Map<String, IterColumnInfo> cols = new HashMap<String, IterColumnInfo>();
            collectColumns(cols, colPrefix, data);
            return cols.values().toArray(new IterColumnInfo[cols.size()]);
        }

        private static String buildName(String prefix, String name) {
            return prefix + (prefix.length() > 0 ? m_nameSeparator : "") + name;
        }

        private static int collectColumns(
            Map<String, IterColumnInfo> columns, String namePrefix, Node data
        ) {
            int ret = 0;
            Node n = data.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    String name = buildName(namePrefix, n.getNodeName());
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
                    String name = buildName(namePrefix, n.getNodeName());
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
            Node n = m_curRecord;
            while (n != null) {
                n = n.getNextSibling();
                if (n.getNodeType() == Node.ELEMENT_NODE) break;
            }
            m_curRecord = (Element)n;
        }
    }
}
