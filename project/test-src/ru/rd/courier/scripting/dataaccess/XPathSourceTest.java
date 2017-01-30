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

import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.XPathContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.rd.courier.CourierException;
import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.datalinks.XPathSourceFactory;
import ru.rd.courier.logging.test.NullLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.utils.DomHelper;
import ru.rd.utils.IntervalHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * User: AStepochkin
 * Date: 27.06.2006
 * Time: 13:29:06
 */
public class XPathSourceTest extends FileDataTestCase {
    public void testBasic() throws CourierException, SQLException, TransformerException {
        final String[][] expected = new String[][] {
              {"col1-1", "col2-1"}
            , {"col1-2", "col2-2"}
        };

        List<XPathSource.ColumnSelectInfo> cols = new LinkedList<XPathSource.ColumnSelectInfo>();
        Map<String, String> ns = new HashMap<String, String>();
        cols.add(new XPathSource.ColumnSelectInfo("col1", new XPathSource.XPathNodeSelector("node-val", ns)));
        cols.add(new XPathSource.ColumnSelectInfo("col2", new XPathSource.XPathNodeSelector("@attr-val", ns)));
        XPathSource src = new XPathSource(new NullLogger(), "//tag1/tag2", cols);
        ResultSet rs = src.request(getDataFile("xpath-basic.xml").getAbsolutePath());
        int r = 0;
        while (rs.next()) {
            assertEquals(expected[r][0], rs.getString("col1"));
            assertEquals(expected[r][1], rs.getString("col2"));
            /*System.out.println(
                 "col1 = " + rs.getString("col1") +
                " col2 = " + rs.getString("col2")
            );*/
            r++;
        }
    }

    public void testXpathPerf() throws TransformerException, IOException, ParserConfigurationException, SAXException {
        final String testPath = "//tag1/tag2";
        final int count = 1000;
        final Document doc = DomHelper.parseXmlFile(getDataFile("xpath-perf.xml"));

        XPath path = new XPath(testPath, null, null, XPath.SELECT);
        IntervalHelper ih = new IntervalHelper();

        ih.reset();
        for (int i = 0; i < count; i++) {
            XPathAPI.eval(doc, testPath);
        }
        System.out.println("not-prepared = " + ih.getInterval());

        ih.reset();
        for (int i = 0; i < count; i++) {
            PrefixResolverDefault prefixResolver = new PrefixResolverDefault(
              doc.getDocumentElement()
            );
            XPathContext xpathSupport = new XPathContext();
            int ctxtNode = xpathSupport.getDTMHandleFromNode(doc);
            path.execute(xpathSupport, ctxtNode, prefixResolver);
        }
        System.out.println("prepared = " + ih.getInterval());
    }

    public void testPerf() throws IOException, ParserConfigurationException, SAXException, CourierException, SQLException {
        XPathSourceFactory f = new XPathSourceFactory(
            null, null,
            DomHelper.parseXmlFile(getDataFile("xpath-perf-source.xml")).getDocumentElement()
        );
        DataSource s = (DataSource)f.getObject(null);
        String q = getDataFile("xpath-perf.xml").getAbsolutePath();

        int count = 20;
        IntervalHelper ih = new IntervalHelper();
        ih.reset();
        int docCount = 0;
        for (int i = 0; i < count; i++) {
            ResultSet rs = s.request(q);
            docCount = 0;
            while (rs.next()) {
                /*
                File of;
                of = getTempFile(rs.getString("SystemInfo/IDString") + ".doc.txt");
                FileHelper.stringToFile(rs.getString("Document"), of);
                of = getTempFile(rs.getString("SystemInfo/IDString") + ".sign.txt");
                FileHelper.stringToFile(rs.getString("Signature"), of);
                */
                docCount++;
            }
            rs.close();
        }
        System.out.println(
            "time = " + ((double)ih.getInterval()/count)
            + " docCount = " + docCount
        );
        System.out.println();
    }
}
