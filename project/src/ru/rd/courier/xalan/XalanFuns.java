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
package ru.rd.courier.xalan;

import org.apache.xalan.lib.Extensions;
import org.apache.xalan.extensions.XSLProcessorContext;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xalan.templates.ElemExtensionCall;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.NodeVector;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.NodeSet;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Document;
import org.w3c.dom.traversal.NodeIterator;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.net.URI;
import java.net.URISyntaxException;

import ru.rd.courier.utils.FileHelper;
import ru.rd.courier.utils.DomHelper;
import ru.rd.utils.FileNameMatcher;

/**
 * User: AStepochkin
 * Date: 08.10.2007
 * Time: 15:20:10
 */
public class XalanFuns {
    public static String getFileText(String file, String charSet) throws IOException {
        return FileHelper.fileToString(new File(file), charSet);
    }

    public static String getFileText(String file) throws IOException {
        return getFileText(file, "cp1251");
    }

    public static boolean hasWord(String word, String str, String sep) {
        for (String w: str.split(sep)) {
            if (w.equals(word)) return true;
        }
        return false;
    }

    public static boolean hasWord(String word, String str) {
        return hasWord(word, str, "\\s*,\\s*");
    }

    public static String getAppPath(String appDir, String path) {
        File pf = new File(path);
        if (pf.isAbsolute()) return path;
        return (new File(appDir, path)).getAbsolutePath();
    }

    public static String getBaseUri(ExpressionContext context) throws TransformerException {
        return context.getXPathContext().getSAXLocator().getSystemId();
    }

    public static String getBaseDir(ExpressionContext context) throws URISyntaxException, TransformerException {
        String buri = getBaseUri(context);
        return (new File(new URI(buri)).getParentFile()).getAbsolutePath();
    }

    public static String getAbsPath(ExpressionContext context, String path) throws URISyntaxException, TransformerException {
        File fpath = new File(path);
        if (fpath.isAbsolute()) return fpath.getAbsolutePath();

        String buri = getBaseUri(context);
        File baseDir = new File(new URI(buri)).getParentFile();
        return (new File(baseDir, path)).getAbsolutePath();
    }

    public static String getAbsPath(ExpressionContext context) throws URISyntaxException, TransformerException {
        return getBaseDir(context);
    }

    public static String findSingleFile(String dir, String nameRegExp) {
        File fdir = new File(dir);
        String[] names = fdir.list(new FileNameMatcher(nameRegExp));
        if (names.length != 1) {
            throw new RuntimeException(
                "Invalid amount of selected files (" + names.length + ") for dir=" + dir + " RegExp=" + nameRegExp
            );
        }
        return names[0];
    }

    public static Set<String> getSet(String set) {
        Set<String> ret = new HashSet<String>();
        if (set != null && set.length() > 0) {
            for(String s: set.split("\\s*,\\s*")) {
                ret.add(s);
            }
        }
        return ret;
    }

    public static void addAttrs(Node cn, NodeSet ns, Set<String> exclude) {
        NamedNodeMap attrs = cn.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node n = attrs.item(i);
            if (!exclude.contains(n.getNodeName())) {
                ns.addNode(n);
            }
        }
    }

    public static void addElems(Node cn, NodeSet ns, Set<String> exclude) {
        for (Node n = cn.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && !exclude.contains(n.getNodeName())) {
                ns.addNode(n);
            }
        }
    }

    public static void addElems(NodeIterator  sns, NodeSet ns, Set<String> exclude) {
        Node n;
        while ((n = sns.nextNode()) != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE && !exclude.contains(n.getNodeName())) {
                ns.addNode(n);
            }
        }
    }

    public static void addAttrs(NodeIterator sns, NodeSet ns, Set<String> exclude) {
        Node n;
        while ((n = sns.nextNode()) != null) {
            if (n.getNodeType() == Node.ATTRIBUTE_NODE && !exclude.contains(n.getNodeName())) {
                ns.addNode(n);
            }
        }
    }

    public static NodeSet filter(Node cn, String excludeElems, String excludeAttrs) {
        NodeSet ret = new NodeSet();
        if (excludeAttrs != null) addAttrs(cn, ret, getSet(excludeAttrs));
        if (excludeElems != null) addElems(cn, ret, getSet(excludeElems));
        return ret;
    }

    public static NodeSet filter(Node cn, String excludeElems) {
        return filter(cn, excludeElems, null);
    }

    public static NodeSet filterAttrs(Node cn, String excludeAttrs) {
        return filter(cn, null, excludeAttrs);
    }

    public static NodeSet filterNs(NodeIterator sns, String excludeElems, String excludeAttrs) {
        NodeSet ret = new NodeSet();
        if (excludeElems != null) addElems(sns, ret, getSet(excludeElems));
        if (excludeAttrs != null) addAttrs(sns, ret, getSet(excludeAttrs));
        return ret;
    }

    public static NodeSet filterNs(NodeIterator sns, String excludeElems) {
        return filterNs(sns, excludeElems, null);
    }

    public static NodeSet filterNsAttrs(NodeIterator sns, String excludeAttrs) {
        return filterNs(sns, null, excludeAttrs);
    }

    public static String getSysVar(String name) {
        String res = System.getProperty(name, null);
        if (res == null) throw new RuntimeException("System variable '" + name + "' does not exist");
        return res;
    }

    public static String getSysVarDef(String name, String def) {
        return System.getProperty(name, def);
    }

    public static String getEnvVar(String name) {
        String res = System.getenv(name);
        if (res == null) throw new RuntimeException("Environment variable '" + name + "' does not exist");
        return res;
    }

    public static String getEnvVarDef(String name, String def) {
        String res = System.getenv(name);
        if (res == null) res = def;
        return res;
    }

    private static Element findRTreeElement(NodeIterator it) {
        Node n;
        while ((n = it.nextNode()) != null) {
            //System.out.println("findFirstElement: " + n.getNodeName());
            if (n.getNodeType() == Node.DOCUMENT_NODE) {
                return DomHelper.getFirstElement(n);
            }
            else if (n.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
                return DomHelper.getFirstElement(n);
            }
            else if (n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element)n;
            }
        }
        throw new RuntimeException("Iterator has not returned element");
    }

    public static Object ctx(ExpressionContext context) throws TransformerException {
        XObject xo = context.getVariableOrParam(new QName("ctx"));
        if (xo.bool()) {
            //System.out.println("-------- xo.TypeString=" + xo.getTypeString());
            Object o = xo.object();
            //System.out.println("-------- o.Class=" + o.getClass().getName());
            if (o instanceof NodeIterator) {
                return findRTreeElement((NodeIterator)o);
            } else if (o instanceof NodeVector) {
                return xo;
            }
            return o;
        } else {
            return context.getContextNode();
        }
    }

    public static Object ctxns(ExpressionContext context) throws TransformerException {
        XObject xo = context.getVariableOrParam(new QName("ctx"));
        if (xo.bool()) {
            //System.out.println("-------- xo.TypeString=" + xo.getTypeString());
            Object o = xo.object();
            return Extensions.nodeset(context, o);
        } else {
            return new NodeSet(context.getContextNode());
        }
    }
}
