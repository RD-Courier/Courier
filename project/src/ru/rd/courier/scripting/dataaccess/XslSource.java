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

import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.jdbc.ResultSets.StringBufferListResultSet;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.SimpleNamespaceResolver;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 03.03.2006
 * Time: 16:50:18
 */
public class XslSource extends TimedStringReceiver implements DataSource {
    private final CourierLogger m_logger;
    private final Transformer m_transformer;
    private final DocumentBuilder m_parser;
    private final Validator m_validator;
    private final String m_varName;
    private SAXParseException m_parseError;
    private final XPaths m_xpaths;

    private static class XPathErrorListener implements ErrorListener {
        private TransformerException m_error;

        private void handleError(TransformerException exception) {
            if (m_error == null) m_error = exception;
        }

        public void warning(TransformerException exception) throws TransformerException {
            handleError(exception);
        }

        public void error(TransformerException exception) throws TransformerException {
            handleError(exception);
        }

        public void fatalError(TransformerException exception) throws TransformerException {
            handleError(exception);
        }

        public TransformerException getError() {
            return m_error;
        }
    }

    private static XPath compileXPath(String path, PrefixResolver prefixResolver) throws TransformerException {
        XPathErrorListener el = new XPathErrorListener();
        XPath ret = new XPath(path, null, prefixResolver, XPath.SELECT, el);
        if (el.getError() != null) throw el.getError();
        return ret;
    }

    public static class XPathInfo {
        public final String colName;
        public final String xpath;

        public XPathInfo(String _colName, String _xpath) {
            colName = _colName;
            xpath = _xpath;
        }
    }

    private abstract static class XPaths {
        private final IterColumnInfo[] m_cols;

        public XPaths(String xslVar, List<XPathInfo> conf) {
            m_cols = new IterColumnInfo[conf.size() + 1];
            m_cols[0] = new IterColumnInfo(xslVar);
            int i = 1;
            for (XPathInfo xi: conf) {
                m_cols[i] = new IterColumnInfo(xi.colName);
                i++;
            }
        }

        public IterColumnInfo[] getCols() {
            return m_cols;
        }

        abstract public void setDocument(Document doc);
        abstract public Node execPath(int i);
    }

    private static class PreparedPaths extends XPaths {
        private final XPath[] m_paths;
        private XPathContext m_xpathSupport;
        private int m_ctxtNode;
        private PrefixResolver m_prefixResolver;
        Document m_doc;

        public PreparedPaths(
            String xslVar, List<XPathInfo> conf, PrefixResolver pr
        ) {
            super(xslVar, conf);
            m_paths = new XPath[conf.size()];
            int i = 0;
            for (XPathInfo xi: conf) {
                try {
                    m_paths[i] = compileXPath(xi.xpath, pr);
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Error compiling '" + xi.colName + "' path", e
                    );
                }
                i++;
            }
        }

        private void initDoc() {
            m_xpathSupport = new XPathContext();
            m_ctxtNode = m_xpathSupport.getDTMHandleFromNode(m_doc);
            m_prefixResolver = new PrefixResolverDefault(m_doc/*.getDocumentElement()*/);
        }

        public void setDocument(Document doc) {
            m_doc = doc;
        }

        public Node execPath(int i) {
            try {

                //System.out.println("PreparedPaths.execPath");

                initDoc();
                XObject xo = m_paths[i].execute(
                    m_xpathSupport, m_ctxtNode, m_prefixResolver
                );
                return xo.nodeset().nextNode();
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class NotPreparedPaths extends XPaths {
        private final String[] m_paths;
        private Document m_doc;

        public NotPreparedPaths(String xslVar, List<XPathInfo> conf) {
            super(xslVar, conf);
            m_paths = new String[conf.size()];
            int i = 0;
            for (XPathInfo xi: conf) {
                m_paths[i] = xi.xpath;
                i++;
            }
        }

        public void setDocument(Document doc) {
            m_doc = doc;
        }

        public Node execPath(int i) {
            try {
                return XPathAPI.selectSingleNode(
                    m_doc, m_paths[i], m_doc.getDocumentElement()
                );
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public XslSource(
        CourierLogger logger, String xsl, String varName, String schemaFile,
        List<XPathInfo> xpathConf, Map<String, String> namespaces, boolean preparedPaths
    ) {
        m_logger = logger;
        m_varName = varName;
        try {
            m_transformer = (new TransformerFactoryImpl()).newTransformer(
                new StreamSource(new File(xsl))
            );

            ErrorHandler eh = new ErrorHandler() {
                public void warning(SAXParseException e) {
                    if (m_parseError == null) m_parseError = e;
                    else m_logger.warning(e);
                }
                public void error(SAXParseException e) {
                    if (m_parseError == null) m_parseError = e;
                    else m_logger.error(e);
                }
                public void fatalError(SAXParseException e) {
                    if (m_parseError == null) m_parseError = e;
                    else m_logger.error(e);
                }
            };

            m_parser = DomHelper.getParser(eh);
            if (schemaFile == null) {
                m_validator = null;
            } else {
                m_validator = DomHelper.getValidator(schemaFile, eh);
            }

            if (xpathConf != null) {
                if (preparedPaths) {
                    PrefixResolver pr = null;
                    if (namespaces != null) {
                        pr = new SimpleNamespaceResolver(namespaces);
                    }
                    m_xpaths = new PreparedPaths(m_varName, xpathConf, pr);
                } else {
                    m_xpaths = new NotPreparedPaths(m_varName, xpathConf);
                }
            } else {
                m_xpaths = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<LinkWarning> timedProcess(String operation) { return null; }
    protected List<LinkWarning> timedFlush() { return null; }
    protected void timedClose() throws CourierException {}
    public void setTimeout(int timeout) throws CourierException {}
    public void cancel() throws CourierException {}

    public ResultSet request(String query) throws CourierException {
        try {
            StringWriter res = new StringWriter();

            m_parseError = null;
            DOMSource ds = new DOMSource(
                m_parser.parse(new InputSource(new StringReader(query)))
            );
            if (m_validator != null && m_parseError == null) m_validator.validate(ds);
            if (m_parseError != null) {
                SAXParseException e = m_parseError;
                m_parseError = null;
                throw e;
            }

            m_transformer.transform(ds, new StreamResult(res));
            String resStr = res.toString();

            if (m_xpaths == null) {
                return new StringBufferListResultSet(null, m_varName, resStr);
            }

            IterColumnInfo[] cols = m_xpaths.getCols();
            StringBuffer[] xpathData = new StringBuffer[cols.length];

            xpathData[0] = new StringBuffer(resStr);

            m_xpaths.setDocument((Document)ds.getNode());
            for (int i = 1; i < xpathData.length; i++) {
                xpathData[i] = new StringBuffer(
                    DomHelper.getNodeValue(m_xpaths.execPath(i - 1))
                );
            }

            return new StringBufferListResultSet(null, cols, xpathData);
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }
}
