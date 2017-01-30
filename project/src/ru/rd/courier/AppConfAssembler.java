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

import org.apache.xpath.XPathAPI;
import org.w3c.dom.*;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;
import ru.rd.courier.utils.*;
import ru.rd.utils.SysUtils;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.lang.reflect.Array;

/**
 * User: AStepochkin
 * Date: 23.10.2007
 * Time: 13:13:08
 */
public class AppConfAssembler {
    private final FileResolver m_fr;
    private final String m_defaultsProp;

    private List<File> m_files = new LinkedList<File>();

    private static final String INSERT_TAG = "insert-tag";
    private static final String INSERT_FILE_TAG = "insert-file";
    private static final String EXTERNAL_FILE_TAG = "external-file";
    private static final String EXTERNAL_FILE_TAGS_TAG = "external-file-tags";
    private static final String DOC_ATTR = "doc";
    private static final String XPATH_ATTR = "xpath";

    public interface FileResolver {
        File getFile(String name);
    }

    public AppConfAssembler(FileResolver fr, String defaultsProp) {
        m_fr = fr;
        m_defaultsProp = defaultsProp;
    }

    private File getAppFile(String name) {
        return m_fr.getFile(name);
    }

    private void cleanupFiles() {
        m_files.clear();
    }

    private File getXmlFile(String file) {
        File f = new File(file);
        if (f.isAbsolute()) return f;
        if (m_files.size() == 0) {
            return getAppFile(file);
        } else {
            return new File(m_files.get(0).getParent(), file);
        }
    }

    private void pushFile(File f) {
        m_files.add(0, f);
    }

    private File popFile() {
        return m_files.remove(0);
    }

    private static List<Node> iteratorToList(NodeIterator ni) {
        List<Node> r = new LinkedList<Node>();
        Node n;
        while ((n = ni.nextNode()) != null) {
            r.add(n);
        }
        return r;
    }

    private static NodeIterator doXPath(Node context, String xpath) throws TransformerException {
        Document doc = context instanceof Document ? (Document)context : context.getOwnerDocument();
        return new XPathAllTextNodeIterator(XPathAPI.selectNodeIterator(context, xpath, doc.getDocumentElement()));
    }

    private void replaceNode(Node rn, NodeIterator ni) throws TransformerException, IOException, SAXException {
        Node pn = rn.getParentNode();
        Node n;
        while ((n = ni.nextNode()) != null) {
            Node nim = rn.getOwnerDocument().importNode(n, true);
            pn.insertBefore(nim, rn);
            if (nim.getNodeType() == Node.ELEMENT_NODE) {
                resolveExternalElement(nim);
            }
        }
        pn.removeChild(rn);
    }

    private void replaceNode(Node rn, Node ctx, String xpath) throws TransformerException, IOException, SAXException {
        replaceNode(rn, doXPath(ctx, xpath));
    }

    private void resolveDocXPath(Element rn, String file, String xpath) throws IOException, SAXException, TransformerException {
        File f = getXmlFile(file);
        Document d = DomHelper.parseXmlFile(f);
        pushFile(f);
        replaceNode(rn, d, xpath);
        popFile();
    }

    public void resolveExternalElement(Node cn) throws IOException, SAXException, TransformerException {
        String doc = null;
        String xpath = null;
        String name = cn.getNodeName();
        Element ce = (Element)cn;
        if (name.equals(INSERT_TAG)) {
            doc = ce.getAttribute(DOC_ATTR);
            if (doc != null && doc.length() == 0) doc = null;
            xpath = ce.getAttribute(XPATH_ATTR);
            if (xpath == null || xpath.length() == 0) {
                xpath = "/*";
            }
        } else if (name.equals(EXTERNAL_FILE_TAG)) {
            doc = DomHelper.getNodeAttr(ce, "file", true);
            xpath = "/*";
        } else if (name.equals(EXTERNAL_FILE_TAGS_TAG)) {
            doc = DomHelper.getNodeAttr(ce, "file", true);
            xpath = "/*/*";
        } else if (name.equals(INSERT_FILE_TAG)) {
            resolveInsertFile(ce);
        } else {
            resolveExternal(ce);
        }
        if (doc != null) {
            resolveDocXPath(ce, doc, xpath);
        }
    }

    public void resolveExternal(Element rn) throws IOException, SAXException, TransformerException {
        Node cn = rn.getFirstChild();
        while (cn != null) {
            Node nn = cn.getNextSibling();
            if (cn.getNodeType() == Node.ELEMENT_NODE) {
                resolveExternalElement(cn);
            }
            cn = nn;
        }
    }

    private void resolveInsertFile(Node cn) throws IOException {
        String text = FileHelper.fileToString(
            getXmlFile(DomHelper.getNodeAttr(cn, "file")),
            DomHelper.getNodeAttr(cn, "encoding", "cp1251")
        );
        Node tn = cn.getOwnerDocument().createTextNode(text);
        cn.getParentNode().replaceChild(tn, cn);
    }

    public void resolveInternal(Element rn) throws IOException, SAXException, TransformerException {
        Node cn = rn.getFirstChild();
        while (cn != null) {
            Node nn = cn.getNextSibling();
            if (cn.getNodeType() == Node.ELEMENT_NODE) {
                String name = cn.getNodeName();
                if (name.equals(INSERT_TAG)) {
                    if (DomHelper.getNodeAttr(cn, DOC_ATTR, false) != null) {
                        throw new RuntimeException("External " + INSERT_TAG + " is prohibited");
                    }
                    replaceNode(cn, cn, DomHelper.getNodeAttr(cn, XPATH_ATTR));
                } else {
                    resolveInternal((Element)cn);
                }
            }
            cn = nn;
        }
    }

    private void copyConfChildren(Element source, Node target) {
        Node cn = source.getFirstChild();
        while (cn != null) {
            if (cn.getNodeType() == Node.ELEMENT_NODE) {
                Node nn = target.getOwnerDocument().importNode(cn, true);
                DomHelper.propogateAttrs(source, (Element)nn);
                target.appendChild(nn);
            }
            cn = cn.getNextSibling();
        }
    }

    public void resolveExternalConfig(
        final Node target, final List<Document> externals, final List<String> tags
    ) {
        Map<String, Element> tagsMap = new HashMap<String, Element>(tags.size());
        for (String tag: tags) tagsMap.put(tag, DomHelper.getChild(target, tag, false));

        for (Document doc: externals) {
            Node cn = doc.getDocumentElement().getFirstChild();
            while (cn != null) {
                if (cn.getNodeType() == Node.ELEMENT_NODE) {
                    String name = cn.getNodeName();
                    if (tagsMap.containsKey(name)) {
                        Element r = tagsMap.get(name);
                        if (r == null) {
                            r = target.getOwnerDocument().createElement(name);
                            target.appendChild(r);
                            tagsMap.put(name, r);
                        }
                        copyConfChildren((Element)cn, r);
                    } else {
                        target.appendChild(target.getOwnerDocument().importNode(cn, true));
                    }
                }
                cn = cn.getNextSibling();
            }
        }
    }

    public void resolveExternalConfig(
        Node target, String ecNodeName, String ecAttrName, List<String> tags
    ) throws TransformerException, IOException, SAXException {
        List<Document> docs = new LinkedList<Document>();
        for (Node n : DomHelper.getChildrenByTagName(target, ecNodeName, false)) {
            docs.add(DomHelper.parseXmlFile(getAppFile(DomHelper.getNodeAttr(n, ecAttrName))));
            n.getParentNode().removeChild(n);
        }
        resolveExternalConfig(target, docs, tags);
    }

    public void resolveExternalConfig(
        Node target, String xpath, List<String> tags
    ) throws TransformerException, IOException, SAXException {
        List<Document> docs = new LinkedList<Document>();
        for (Node n : iteratorToList(doXPath(target, xpath))) {
            docs.add(DomHelper.parseXmlFile(getAppFile(n.getNodeValue())));
        }
        resolveExternalConfig(target, docs, tags);
    }

    public void resolve(InputStream defStructData, Element e) throws TransformerException, IOException, SAXException {
        cleanupFiles();
        if (defStructData != null) inheritConfig(defStructData, e);
        resolveExternalConfig(
            e, "external-config", "file",
            StringHelper.list("pipelines db-profiles source-profiles target-profiles", " ")
        );
        resolveExternal(e);
        resolveInternal(e);
    }

    public void propogateAttrs(Node source, Element target, Set<String> fattrs) {
        NamedNodeMap sas = source.getAttributes();
        for (int i = 0; i < sas.getLength(); i++) {
            Attr a = (Attr)sas.item(i);
            final String name = a.getName();
            if (!target.hasAttribute(name)) {
                String v = a.getValue();
                if (fattrs != null && m_curDefaultFile != null && fattrs.contains(name)) {
                    File f = new File(v);
                    if (!f.isAbsolute()) {
                        v = (new File(m_curDefaultFile.getParent(), v)).getAbsolutePath();
                    }
                }
                target.setAttribute(name, v);
            }
        }
    }

    private static Set<String> arrayToSet(String[] arr) {
        Set<String> ret = new HashSet<String>(arr.length);
        for (String s: arr) {
            ret.add(s);
        }
        return ret;
    }

    private void inheritNode(Node struct, Node base, Element inherited) {
        /*System.out.println(
            "Struct = " + struct.getNodeName() + " Base = " + base.getNodeName()
            + " Inherited = " + DomHelper.getNodePath(inherited) + " URI=" + inherited.getBaseURI()
        );*/

        Set<String> fattrs = null;
        String fsattrs = DomHelper.getNodeAttr(struct, "fattrs", null);
        if (fsattrs != null) {
            fattrs = arrayToSet(StringHelper.splitString(fsattrs, ','));
        }
        propogateAttrs(base, inherited, fattrs);
        for (Node sn = struct.getFirstChild(); sn != null; sn = sn.getNextSibling()) {
            if (sn.getNodeType() != Node.ELEMENT_NODE) continue;
            Element be = DomHelper.getChild(base, sn.getNodeName(), null);
            if (be == null) continue;
            StringChecker echeck = null;
            if (sn.getNodeName().equals("multiple-element")) {
                String as;
                if (DomHelper.getBoolYesNo(sn, "any")) {
                    echeck = new ConstStringChecker(true);
                } else if ((as = DomHelper.getNodeAttr(sn, "list", null)) != null) {
                    echeck = new SetStringChecker(as);
                } else if ((as = DomHelper.getNodeAttr(sn, "re", null)) != null) {
                    echeck = new ReqExpChecker(as);
                }
            } else if (DomHelper.getNodeAttr(sn, "multiple", "").equals("y")) {
                echeck = new EqualsStringChecker(sn.getNodeName());
            }
            if (echeck == null) {
                Element ie = DomHelper.getChild(inherited, sn.getNodeName(), false);
                if (ie == null) {
                    ie = (Element) inherited.getOwnerDocument().importNode(be, false);
                    inherited.appendChild(ie);
                }
                if (!sn.hasChildNodes()) DomHelper.insertChildrenAtStart(be, ie);
                inheritNode(sn, be, ie);
            } else {
                for (Node in = inherited.getFirstChild(); in != null; in = in.getNextSibling()) {
                    if (in.getNodeType() == Node.ELEMENT_NODE && echeck.isTrue(in.getNodeName())) {
                        inheritNode(sn, be, (Element)in);
                    }
                }
            }
        }
    }

    public void inheritConfig(InputStream structData, Element inherited) throws IOException, SAXException {
        Document struct = DomHelper.parseStream(structData);
        inheriteConfig(struct, null, inherited);
    }

    private File m_curDefaultFile;
    private void inheriteConfig(Document struct, File inheritFile, Element inherited) throws IOException, SAXException {
        File df = getBaseFile(inheritFile, inherited);
        if (df == null) return;
        Element dd = DomHelper.parseXmlFile(df).getDocumentElement();
        inheriteConfig(struct, df, dd);
        File oldDefFile = m_curDefaultFile;
        m_curDefaultFile = df;
        try {
            inheritNode(struct.getDocumentElement(), dd, inherited);
        } finally {
            m_curDefaultFile = oldDefFile;
        }
    }

    private static final String sDefaultConfAttr = "default-config";
    private File getBaseFile(File f, Element ir) {
        String defAttr = null;
        if (ir.hasAttribute(sDefaultConfAttr)) {
            defAttr = ir.getAttribute(sDefaultConfAttr);
            ir.removeAttribute(sDefaultConfAttr);
        } else {
            if (f == null && m_defaultsProp != null) {
                defAttr = System.getProperty(m_defaultsProp);
                if (defAttr == null) defAttr = System.getenv(m_defaultsProp);
            }
        }
        if (defAttr == null) return null;
        File df;
        if (f == null) {
            df = m_fr.getFile(defAttr);
        } else {
            df = new File(defAttr);
            if (!df.isAbsolute()) df = new File(f.getParentFile(), defAttr);
        }
        return df;
    }
}
