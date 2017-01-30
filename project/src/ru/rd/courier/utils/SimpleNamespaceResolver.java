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
package ru.rd.courier.utils;

import org.apache.xml.utils.PrefixResolver;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.Properties;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * User: AStepochkin
 * Date: 02.06.2008
 * Time: 12:05:45
 */
public class SimpleNamespaceResolver implements PrefixResolver {
    private final Map<String, String> m_namespaces;

    public SimpleNamespaceResolver(Map<String, String> namespaces) {
        m_namespaces = namespaces;
    }

    public SimpleNamespaceResolver() {
        this(new LinkedHashMap<String, String>());
    }

    public void addPrefix(String prefix, String namespace) {
        m_namespaces.put(prefix, namespace);
    }

    public void addPrefixes(Element e) {
        DomHelper.collectNamespaces(e, m_namespaces);
    }

    public void addPrefixes(Map<String, String> map) {
        m_namespaces.putAll(map);
    }

    public String getNamespaceForPrefix(String prefix) {
        return getNamespaceForPrefix(prefix, null);
    }

    public String getNamespaceForPrefix(String prefix, Node context) {
        return m_namespaces.get(prefix);
    }

    public String getBaseIdentifier() { return null; }
    public boolean handlesNullPrefixes() { return false; }
}
