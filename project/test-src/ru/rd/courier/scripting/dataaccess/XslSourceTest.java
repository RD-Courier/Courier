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

import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.logging.test.NullLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.utils.IntervalHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 13.10.2006
 * Time: 14:42:03
 */
public class XslSourceTest extends FileDataTestCase {
    public void atestMemoryLeaks() throws SQLException {
        List<XslSource.XPathInfo> xpathConf = new LinkedList<XslSource.XPathInfo>();
        xpathConf.add(new XslSource.XPathInfo("col1", "/*/@prop1"));
        XslSource src = new XslSource(
            new NullLogger(),
            getDataFile("xsl-source-test.xsl").getAbsolutePath(),
            "var", null, xpathConf, null, true
        );

        IntervalHelper ih = new IntervalHelper();
        System.out.println(
               "memory = " + Runtime.getRuntime().freeMemory()
            + " time = " + ih.getInterval()
        );
        for (int i = 0; i <= 3000; i++) {
            /*ResultSet rs =*/ src.request(
                "<?xml version=\"1.0\"?><root prop1=\"aaaa\"/>"
            );

            if (i % 1000 == 0) {
                System.out.println(
                       "memory = " + Runtime.getRuntime().freeMemory()
                    + " time = " + ih.getInterval()
                );
            }
            /*
            while (rs.next()) {
                String v = rs.getString("col1");
                System.out.println(v);
            }
            */
        }
    }

    private static class NullPrefixResolver implements PrefixResolver {
        public String getNamespaceForPrefix(String prefix) {
            return prefix;
        }

        public String getNamespaceForPrefix(String prefix, Node context) {
            return prefix;
        }

        public String getBaseIdentifier() {
            return null;
        }

        public boolean handlesNullPrefixes() {
            return true;
        }
    }

    private Node MyXPath(String path, Node ctx) throws TransformerException {
        XPath xpath = new XPath(
            path, null, null /*prefixResolver*/, XPath.SELECT, null
        );
        XPathContext xpathSupport = new XPathContext();
        int ctxHandle = xpathSupport.getDTMHandleFromNode(ctx);
        //PrefixResolver prefixResolver = new PrefixResolverDefault(ctx instanceof Document ? ((Document)ctx).getDocumentElement() : ctx);
        PrefixResolver prefixResolver = new NullPrefixResolver();
        //PrefixResolver prefixResolver = null;
        XObject xo = xpath.execute(xpathSupport, ctxHandle,  prefixResolver);
        return xo.nodeset().nextNode();
    }

    private Node StdXPath(String path, Node ctx) throws TransformerException {
        return XPathAPI.selectSingleNode(ctx, path);
    }

    public void atestMemoryLeaks2() throws SQLException, TransformerException, IOException, ParserConfigurationException, SAXException {
        //final String docFile = "C:\\Projects\\IT\\3D_Projects\\Courier\\project\\test-src\\ru\\rd\\courier\\scripting\\dataaccess\\xsl-source-test-namespace.xml";
        Document doc = DomHelper.parseString("<?xml version=\"1.0\"?><root prop1=\"aaaa\"/>");
        IntervalHelper ih = new IntervalHelper();
        System.out.println(
               "memory = " + Runtime.getRuntime().freeMemory()
            + " time = " + ih.getInterval()
        );
        final String path = "/*/@prop1";
        for (int i = 0; i <= 10000; i++) {
            //MyXPath(path, doc);
            StdXPath(path, doc);

            if (i % 1000 == 0) {
                System.out.println(
                       "memory = " + Runtime.getRuntime().freeMemory()
                    + " time = " + ih.getInterval()
                );
            }
        }
    }

    public void testNamespace() throws SQLException, IOException {
        List<XslSource.XPathInfo> xpathConf = new LinkedList<XslSource.XPathInfo>();
        xpathConf.add(new XslSource.XPathInfo(
            "col1", "/*/exch:SystemInfo/exch:IDString"
        ));
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("exch", "Pythoness.Exchange");
        XslSource src = new XslSource(
            new NullLogger(),
            getDataFile("xsl-source-test.xsl").getAbsolutePath(),
            "var", null, xpathConf, ns, true
        );

        ResultSet rs = src.request(
            getFileText("xsl-source-test-namespace2.xml", "cp1251")
        );

        assertTrue(rs.next());
        //System.out.println("col1 = '" + rs.getString("col1") + "'");
        assertEquals(
            //"560867",
            "730D80AA-B988-44C6-82CD-E1AC57764968",
            rs.getString("col1")
        );
    }
}
