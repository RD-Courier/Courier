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

import org.apache.xpath.XPathAPI;
import org.apache.xml.utils.PrefixResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;
import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.SimpleNamespaceResolver;

import javax.xml.transform.TransformerException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.LinkedHashMap;

/**
 * User: Astepochkin
 * Date: 28.06.2006
 * Time: 17:20:52
 */
public abstract class XPathSourceBase extends TimedStringReceiver implements DataSource {
    private final CourierLogger m_logger;
    protected final String m_recordSelector;
    private final Map<String, String> m_nsPrefixes;
    private String m_encoding = null;

    public XPathSourceBase(
        CourierLogger logger, String recordSelector, Map<String, String> nsPrefixes
    ) {
        m_logger = logger;
        m_recordSelector = recordSelector;
        m_nsPrefixes = nsPrefixes;
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

    public final ResultSet getResultSet(InputStream is)
    throws TransformerException, IOException, SAXException {
        DomHelper.LoggingErrorHandler eh = new DomHelper.LoggingErrorHandler(m_logger);
        Document doc = DomHelper.parseStreamEx(is, eh, m_encoding);
        if (eh.getError() != null) throw eh.getError();

        SimpleNamespaceResolver nsResolver = new SimpleNamespaceResolver();
        nsResolver.addPrefixes(doc.getDocumentElement());
        nsResolver.addPrefixes(m_nsPrefixes);
        final NodeIterator nl = XPathAPI.eval(doc, m_recordSelector, nsResolver).nodeset();
        return innerRequest(nl, nsResolver);
    }

    public void setEncoding(String encoding) {
        m_encoding = encoding;
    }

    private ResultSet innerFileRequest(String query)
    throws IOException, SAXException, TransformerException {
        return getResultSet(new BufferedInputStream(new FileInputStream(query)));
    }

    protected abstract ResultSet innerRequest(final NodeIterator nl, PrefixResolver prefixResolver);
}
