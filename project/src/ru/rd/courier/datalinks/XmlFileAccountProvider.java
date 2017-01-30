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
package ru.rd.courier.datalinks;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import ru.rd.courier.utils.DomHelper;

/**
 * User: Astepochkin
 * Date: 08.05.2009
 * Time: 10:14:40
 */
public class XmlFileAccountProvider extends DefaultAccountProvider {
    public XmlFileAccountProvider() {}

    public XmlFileAccountProvider(File f, String pwd) throws IOException, SAXException {
        this();
        addAccountsFromXml(f, pwd);
    }

    public void addAccountsFromXml(File f, String pwd) throws IOException, SAXException {
        String accounts = AccountUtils.decrypt(f, pwd);
        Node doc = DomHelper.parseString(accounts).getDocumentElement();
        for (Element acce: DomHelper.elements(doc)) {
            String code = DomHelper.getNodeAttr(acce, "code", null);
            if (code == null) {
                configHost(acce);
            } else {
                addCodeAccount(code, elAccount(acce));
            }
        }
    }

    private void configHost(Element he) {
        String host = DomHelper.getNodeAttr(he, "host");
        for (Element acce: DomHelper.elements(he)) {
            String code = DomHelper.getNodeAttr(acce, "code", null);
            if (code == null) {
                addHostTypeAccount(host, DomHelper.getNodeAttr(acce, "type"), elAccount(acce));
            } else {
                addHostCodeAccount(host, code, elAccount(acce));
            }
        }
    }

    private static Account elAccount(Element e) {
        return new Account(DomHelper.getNodeAttr(e, "username"), DomHelper.getNodeAttr(e, "password"));
    }
}
