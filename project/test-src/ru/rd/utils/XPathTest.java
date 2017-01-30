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
package ru.rd.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.apache.xpath.XPathAPI;
import org.xml.sax.SAXException;
import ru.rd.courier.utils.DomHelper;

import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 27.11.2007
 * Time: 10:36:45
 */
public class XPathTest {
    public static void main(String[] args) throws IOException, SAXException, TransformerException {
        Document context = DomHelper.parseString(
            "<?xml version=\"1.0\"?>"
            + "\n<root>"
            + "\n11111111"
            + "\n<![CDATA[cdata text]]>"
            + "\n22222222"
            + "\n</root>"
        );
        NodeIterator ni = XPathAPI.selectNodeIterator(context, "/*/node()", context);
        Node n;
        while (true) {
            n = ni.nextNode();
            if (n == null) break;
            System.out.println(n.getNodeValue());
        }
    }
}
