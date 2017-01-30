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
package ru.rd.courier.test;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.rd.courier.utils.DomHelper;

import java.io.FileWriter;
import java.io.IOException;

public class TestObjectToXml {
    public static class TestImbeddedClass {
        public String m_strAlexField;

        public TestImbeddedClass() {
        }

        public TestImbeddedClass(String strField) {
            m_strAlexField = strField;
        }
    }

    public static class TestClass {
        public int m_intField;
        public String m_strField;
        public char[] m_charArrField;
        public TestImbeddedClass[] m_innerClassArray;

        public TestClass() {
        }

        public TestClass(int intField, String strField) {
            m_intField = intField;
            m_strField = strField;
            m_charArrField = new char[] {'ø', 'æ'};
            m_innerClassArray = new TestImbeddedClass[] {
                new TestImbeddedClass("imbed1"),
                new TestImbeddedClass("imbed2"),
                new TestImbeddedClass("imbed3"),
                new TestImbeddedClass("imbed4")
            };
        }
    }

    public static void main(String[] args) throws IOException {
        Document doc = new DocumentImpl();
        Element e = doc.createElement("data");
        DomHelper.convertToXml(new TestClass(4444, "strData"), e);
        doc.appendChild(e);


        OutputFormat format = new OutputFormat(doc, "windows-1251", true);
        //format.setLineSeparator(LineSeparator.Windows);
        //format.setIndenting(true);
        //format.setLineWidth(0);
        //format.setPreserveSpace(true);
        XMLSerializer serializer = new XMLSerializer (
            new FileWriter("output.xml"), format
        );
        serializer.asDOMSerializer();
        serializer.serialize(doc);

        //TestClass o = (TestClass)DomHelper.restoreFromXml(doc.getDocumentElement());
        //System.out.println(o.getClass().getName());
        //System.out.println(o.m_strField);
        //System.out.println(o.m_innerClassArray[0].m_strAlexField);
    }
}
