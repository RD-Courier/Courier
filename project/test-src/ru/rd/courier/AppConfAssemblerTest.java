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
package ru.rd.courier;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringHelper;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.FileInputStream;

/**
 * User: AStepochkin
 * Date: 23.10.2007
 * Time: 18:04:52
 */
public class AppConfAssemblerTest extends FileDataTestCase {
    private AppConfAssembler m_as;

    protected void courierSetUp() throws Exception {
        m_as = new AppConfAssembler(getFR(), null);
    }

    protected void courierTearDown() throws Exception {
        m_as = null;
    }

    private AppConfAssembler.FileResolver getFR() {
        return new AppConfAssembler.FileResolver() {
            public File getFile(String name) {
                return getDataFile(name);
            }
        };
    }

    private static void showXml(Node n) throws IOException {
        StringWriter w = new StringWriter(4*1024);
        DomHelper.serialize(n, w);
        System.out.println(w.toString());
    }

    public void testExternalTags() throws IOException, SAXException, TransformerException {
        Document doc = DomHelper.parseXmlFile(getDataFile("AppConfAssemblerTest.xml"));
        m_as.resolveExternal(doc.getDocumentElement());
        showXml(doc);
    }

    public void _testResolveExternalConf() throws IOException, SAXException, TransformerException {
        Document target = DomHelper.parseXmlFile(getDataFile("AppConfAssemblerTestExtConf.xml"));
        m_as.resolveExternalConfig(
            target.getDocumentElement(), "external-config", "file",
            StringHelper.list("db-profiles pipelines", " ")
        );
        showXml(target);
    }

    public void _testBaseConfig() throws IOException, SAXException {
        Document xml = DomHelper.parseXmlFile(getDataFile("AppConfInheritedConfig.xml"));
        m_as.inheritConfig(
            new FileInputStream(getDataFile("distr/base-structure.xml")), xml.getDocumentElement()
        );
        showXml(xml);
    }
}
