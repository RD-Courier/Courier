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

import ru.rd.courier.jdbc.ResultSets.StringBufferedResultSet;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.SimpleNamespaceResolver;
import ru.rd.courier.logging.CourierLogger;

import javax.xml.transform.TransformerException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xml.utils.PrefixResolver;

/**
 * User: AStepochkin
 * Date: 27.06.2006
 * Time: 11:49:54
 */
public class XPathSource extends XPathSourceBase {
    private final List<ColumnSelectInfo> m_cols;

    public interface NodeSelector {
        Node selectNode(Node root, PrefixResolver prefixResolver) throws TransformerException;
    }

    public static class XPathNodeSelector implements NodeSelector {
        private final XPath m_path;

        public XPathNodeSelector(String path, Map<String, String> nsPrefixes) {
            try {
                m_path = new XPath(path, null, new SimpleNamespaceResolver(nsPrefixes), XPath.SELECT, null);
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        }

        public Node selectNode(Node root, PrefixResolver prefixResolver) throws TransformerException {
            //return XPathAPI.eval(root, m_path, prefixResolver).nodeset().nextNode();
            XPathContext ctx = new XPathContext(false);
            int ctxtNode = ctx.getDTMHandleFromNode(root);
            return m_path.execute(ctx, ctxtNode, prefixResolver).nodeset().nextNode();
        }
    }

    public static class FastNodeSelector implements NodeSelector {
        private List<SubNodeFinder> m_path;

        private interface SubNodeFinder {
            Node findSubNode(Node node, PrefixResolver prefixResolver);
        }

        private static class ElementFinder implements SubNodeFinder {
            private String m_name;

            public ElementFinder(String name) {
                m_name = name;
            }

            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                return DomHelper.getChild(node, m_name, false);
            }

            public String toString() {
                return m_name + " element";
            }
        }

        private static class ElementFinderNS implements SubNodeFinder {
            private String m_nspace;
            private String m_name;

            public ElementFinderNS(String nspace, String name) {
                m_nspace = nspace;
                m_name = name;
            }

            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                return DomHelper.getChildNS(node, m_nspace, m_name);
            }

            public String toString() {
                return m_name + " element for namespace " + m_nspace;
            }
        }

        private static class ElementFinderNSP implements SubNodeFinder {
            private String m_prefix;
            private String m_name;

            public ElementFinderNSP(String prefix, String name) {
                m_prefix = prefix;
                m_name = name;
            }

            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                return DomHelper.getChildNS(node, prefixResolver.getNamespaceForPrefix(m_prefix), m_name);
            }

            public String toString() {
                return m_prefix + ":" + m_name + " element";
            }
        }

        private static class ParentFinder implements SubNodeFinder {
            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                return node.getParentNode();
            }

            public String toString() {
                return "parent";
            }
        }

        private static class SelfFinder implements SubNodeFinder {
            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                return node;
            }

            public String toString() {
                return "self";
            }
        }

        private static class DocFinder implements SubNodeFinder {
            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                return node.getOwnerDocument();
            }

            public String toString() {
                return "root";
            }
        }

        private static class AttributeFinder implements SubNodeFinder {
            private String m_name;

            public AttributeFinder(String name) {
                m_name = name;
            }

            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    throw new RuntimeException(
                        "Node " + DomHelper.getNodePath(node) + " is not an element node");
                }
                return ((Element)node).getAttributeNode(m_name);
            }

            public String toString() {
                return m_name + " attribute";
            }
        }

        private static class AttributeFinderNS implements SubNodeFinder {
            private String m_nspace;
            private String m_name;

            public AttributeFinderNS(String nspace, String name) {
                m_nspace = nspace;
                m_name = name;
            }

            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    throw new RuntimeException(
                        "Node " + DomHelper.getNodePath(node) + " is not an element node");
                }
                return ((Element)node).getAttributeNodeNS(m_nspace, m_name);
            }

            public String toString() {
                return m_name + " attribute for namespace " + m_nspace;
            }
        }

        private static class AttributeFinderNSP implements SubNodeFinder {
            private String m_prefix;
            private String m_name;

            public AttributeFinderNSP(String prefix, String name) {
                m_prefix = prefix;
                m_name = name;
            }

            public Node findSubNode(Node node, PrefixResolver prefixResolver) {
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    throw new RuntimeException(
                        "Node " + DomHelper.getNodePath(node) + " is not an element node");
                }
                return ((Element)node).getAttributeNodeNS(prefixResolver.getNamespaceForPrefix(m_prefix), m_name);
            }

            public String toString() {
                return m_prefix + ":" + m_name + " attribute";
            }
        }

        public FastNodeSelector(String path, Map<String, String> nsPrefixes) {
            m_path = new LinkedList<SubNodeFinder>();
            int bpos;
            if (path.charAt(0) == '/') {
                bpos = 1;
                m_path.add(new DocFinder());
            } else {
                bpos = 0;
            }

            String[] pathNames = StringHelper.splitString(path, '/', bpos);
            for (String name: pathNames) {
                boolean isAttr = false;
                if (name.charAt(0) == '@') {
                    isAttr = true;
                    name = name.substring(1);
                }
                String[] pName = StringHelper.splitString(name, ':');
                name = pName[pName.length - 1];
                String prefix;
                String nspace;
                if (pName.length > 1) {
                    prefix = pName[0];
                    nspace = nsPrefixes.get(prefix);
                } else {
                    prefix = null;
                    nspace = null;
                }
                SubNodeFinder snf;
                if (isAttr) {
                    if (prefix == null){
                        snf = new AttributeFinder(name);
                    } else {
                        if (nspace == null) {
                            snf = new AttributeFinderNSP(prefix, name);
                        } else {
                            snf = new AttributeFinderNS(nspace, name);
                        }
                    }
                } else if (name.equals(".")) {
                    snf = new SelfFinder();
                } else if (name.equals("..")) {
                    snf = new ParentFinder();
                } else {
                    if (prefix == null){
                        snf = new ElementFinder(name);
                    } else {
                        if (nspace == null) {
                            snf = new ElementFinderNSP(prefix, name);
                        } else {
                            snf = new ElementFinderNS(nspace, name);
                        }
                    }
                }
                m_path.add(snf);
            }
        }

        public Node selectNode(Node root, PrefixResolver prefixResolver) throws TransformerException {
            Node n = root;
            //Node nn;
            for (SubNodeFinder nf: m_path) {
                n = nf.findSubNode(n, prefixResolver);
                //if (nn == null) throw new RuntimeException(nf + " not found inside " + DomHelper.getNodePath(n));
                if (n == null) return null;
            }
            return n;
        }
    }

    public static class ColumnSelectInfo {
        public final String m_name;
        public final NodeSelector m_path;

        public ColumnSelectInfo(String name, NodeSelector path) {
            m_name = name;
            m_path = path;
        }
    }

    public XPathSource(
        CourierLogger logger, String recordSelector, List<ColumnSelectInfo> cols,
        Map<String, String> nsPrefixes
    ) {
        super(logger, recordSelector, nsPrefixes);
        m_cols = cols;
    }

    public XPathSource(CourierLogger logger, String recordSelector, List<ColumnSelectInfo> cols) {
        this(logger, recordSelector, cols, new HashMap<String, String>());
    }

    protected ResultSet innerRequest(final NodeIterator nl, final PrefixResolver prefixResolver) {
        IterColumnInfo[] infos = new IterColumnInfo[m_cols.size()];
        int i = 0;
        for (ColumnSelectInfo ci: m_cols) {
            infos[i] = new IterColumnInfo(ci.m_name, 0);
            i++;
        }

        ResultSet rs;
        rs = new StringBufferedResultSet(null, infos) {
            protected boolean getRecord() throws SQLException {
                Node rn = nl.nextNode();
                if (rn == null) return false;

                int i = 1;
                for (ColumnSelectInfo ci: m_cols) {
                    Node cn;
                    try {
                        cn = ci.m_path.selectNode(rn, prefixResolver);
                    } catch (TransformerException e) {
                        SQLException sqle = new SQLException();
                        sqle.initCause(e);
                        throw sqle;
                    }
                    updateString(i, DomHelper.getNodeValue(cn));
                    i++;
                }
                return true;
            }

            protected int skipRecords(int count) {
                int c = 0;
                while (nl.nextNode()!= null) c++;
                return c;
            }
        };

        /*
        DataBufferResultSet rs = new DataBufferResultSet();
        for (ColumnSelectInfo ci: m_cols) {
            rs.addColumn(new StringColumnInfo(ci.m_name, ci.m_size));
        }

        Node rn;
        while ((rn = nl.nextNode())!= null) {
            rs.addRecord();
            i = 1;
            for (ColumnSelectInfo ci: m_cols) {
                Node cn = XPathAPI.selectSingleNode(rn, ci.m_path);
                rs.updateString(i, DomHelper.getNodeValue(cn));
                i++;
            }
        }

        rs.beforeFirst();
        */
        return rs;
    }
}
