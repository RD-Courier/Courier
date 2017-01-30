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
package ru.rd.courier.scripting;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.DomHelper;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class XmlStatementFactory {
    private ObjectRepositories m_repositories = null;
    private CustomTagProcessor m_tagProc = null;
    private Map<String, TagInfo> m_tagInfos = new HashMap<String, TagInfo>();
    private Map<String, NodeCracker> m_crackers = new HashMap<String, NodeCracker>();

    private static class TagInfo {
        public final String m_name;
        public final String m_type;
        public final Map<String, String> m_params = new HashMap<String, String>();
        public static final Map<String, Class> s_primitiveClasses = new HashMap<String, Class>();
        static {
            s_primitiveClasses.put("int", int.class);
            s_primitiveClasses.put("boolean", boolean.class);
        }

        private static final String c_scriptingPackageName = "ru.rd.courier.scripting";

        private static final String c_attrTagName = "name";
        private static final String c_attrTagType = "type";
        private static final String c_attrParamName = "name";
        private static final String c_attrParamValue = "value";
        private static final String c_attrParamClass = "class";
        private static final String c_attrParamSignature = "signature";

        public TagInfo(final Element conf) {
            m_name = conf.getAttribute(c_attrTagName);
            m_type = conf.getAttribute(c_attrTagType);
            for (Node n = conf.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    final Element e = (Element)n;
                    m_params.put(
                        e.getAttribute(c_attrParamName),
                        e.getAttribute(c_attrParamValue)
                    );
                }
            }
        }

        private Class getTagClass() throws CourierException {
            try {
                String cname = m_params.get(c_attrParamClass);
                if (cname.charAt(0) == '*') {
                    cname = c_scriptingPackageName + cname.substring(1);
                }
                return Class.forName(cname);
            } catch (Exception e) {
                throw new CourierException(e.getMessage(), e);
            }
        }

        private Class getSignatureClass() throws CourierException {
            try {
                String sName = m_params.get(c_attrParamSignature);
                if (s_primitiveClasses.containsKey(sName))
                    return s_primitiveClasses.get(sName); else {
                    if (sName.charAt(0) == '*') {
                        sName = c_scriptingPackageName + sName.substring(1);
                    }
                    return Class.forName(sName);
                }
            } catch (Exception e) {
                throw new CourierException(e.getMessage(), e);
            }
        }
    }

    public interface CustomTagProcessor {
        ScriptStatement process(
            XmlStatementFactory sf, Element n) throws CourierException;
    }

    public static class ChainedTagProcessor implements CustomTagProcessor {
        private final CustomTagProcessor m_parent;
        private final CustomTagProcessor m_child;

        public ChainedTagProcessor(
            CustomTagProcessor parent, CustomTagProcessor child
        ) {
            m_parent = parent;
            m_child = child;
        }

        public ScriptStatement process(
            XmlStatementFactory sf, Element n) throws CourierException
        {
            ScriptStatement ret = m_child.process(sf, n);
            if (ret != null || m_parent == null) return ret;
            return m_parent.process(sf, n);
        }

        public CustomTagProcessor getParent() {
            return m_parent;
        }
    }

    public interface ObjectRepositories {
        Object getObject(String repositoryName, String objectName);
    }

    private interface NodeCracker {
        Object process(Node n) throws CourierException;
    }

    private class StockCracker implements NodeCracker {
        public Object process(final Node n) throws CourierException {
            if (m_repositories == null) {
                throw new CourierException(
                    "Attempt to get a stock object while repositories is null"
                );
            }
            return m_repositories.getObject(
                n.getAttributes().getNamedItem("rep").getNodeValue(),
                n.getAttributes().getNamedItem("obje").getNodeValue()
            );
        }
    }

    private class PrimitiveCracker implements NodeCracker {
        public Object process(final Node n) throws CourierException {
            try {
                final TagInfo ti = getTagInfo(n.getNodeName());
                final Class cl = Class.forName(
                    ti.m_params.get(TagInfo.c_attrParamClass));
                if (DomHelper.getBoolYesNo(n, "null", false)) return null;
                final String value = DomHelper.getNodeValue(n);
                final Constructor cons = cl.getDeclaredConstructor(new Class[] {String.class});
                return cons.newInstance(new Object[] {value});
            } catch (Exception e) {
                throw new CourierException(e.getMessage(), e);
            }
        }
    }

    private class ConstructorCracker implements NodeCracker {
        public Object process(final Node n) throws CourierException {
            final String nodeName = n.getNodeName();

            TagInfo ti = getTagInfo(nodeName);
            final Class stmtClass = ti.getTagClass();

            final List<Class> parTypes = new LinkedList<Class>();
            final List<Object> parValues = new LinkedList<Object>();
            int parQnt = 0;
            for (Node cn = n.getFirstChild(); cn != null; cn = cn.getNextSibling()) {
                if (cn.getNodeType() == Node.ELEMENT_NODE) {
                    parValues.add(processNode(cn));
                    ti = getTagInfo(cn.getNodeName());
                    if (ti == null) {
                        parTypes.add(ScriptStatement.class);
                    } else {
                        parTypes.add(ti.getSignatureClass());
                    }
                    parQnt++;
                }
            }

            try {
                Constructor c = stmtClass.getConstructor(
                    parTypes.toArray(new Class[parQnt])
                );
                return c.newInstance(parValues.toArray());
            } catch (Exception e) {
                throw new CourierException(
                    "Error creating statement for tag: " + n.getNodeName(), e
                );
            }
        }
    }

    private class ArrayCracker implements NodeCracker {
        public Object process(final Node n) throws CourierException {
            final List<Object> parValues = new LinkedList<Object>();
            Node node = n.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    parValues.add(processNode(node));
                }
                node = node.getNextSibling();
            }
            return parValues.toArray();
        }
    }

    private class MapCracker implements NodeCracker {
        public Object process(final Node n) throws CourierException {
            final Map<String, Object> parValues = new HashMap<String, Object>();
            Node node = n.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    final Element cn = (Element)node;
                    final Element stat = DomHelper.getFirstElement(cn);
                    parValues.put(cn.getAttribute("key"), processNode(stat));
                }
                node = node.getNextSibling();
            }
            return parValues;
        }
    }

    private class ListCracker implements NodeCracker {
        public Object process(final Node n) throws CourierException {
            final List<Object> values = new LinkedList<Object>();
            Node node = n.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    values.add(processNode(node));
                }
                node = node.getNextSibling();
            }
            return values;
        }
    }

    private class XmlNodeCracker implements NodeCracker {
        public Object process(final Node n) throws CourierException {
            return n;
        }
    }

    private Object processNode(final Node n) throws CourierException {
        Object res = null;
        final String nodeName = n.getNodeName();

        final TagInfo ti = getTagInfo(nodeName);
        if (ti == null) {
            if (m_tagProc != null) {
                res =  m_tagProc.process(this, (Element)n);
            }
            if (res == null) {
                throw new CourierException(
                    "Cannot generate statement for tag '" + DomHelper.getNodePath(n) + "'"
                );
            }
        } else {
            final NodeCracker nc = m_crackers.get(ti.m_type);
            res = nc.process(n);
        }
        return res;
    }

    private TagInfo getTagInfo(final String tagName) {
        return m_tagInfos.get(tagName);
    }

    public XmlStatementFactory(
        final Document config, final ObjectRepositories repositories
    ) {
        m_repositories = repositories;
        final NodeList nl = config.getDocumentElement().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final TagInfo ti = new TagInfo((Element)nl.item(i));
                m_tagInfos.put(ti.m_name, ti);
            }
        }

        m_crackers.put("constructor", new ConstructorCracker());
        m_crackers.put("primitive", new PrimitiveCracker());
        m_crackers.put("array", new ArrayCracker());
        m_crackers.put("list", new ListCracker());
        m_crackers.put("map", new MapCracker());
        m_crackers.put("node", new XmlNodeCracker());
        m_crackers.put("stock", new StockCracker());
    }

    private void setTagProcessor(final CustomTagProcessor tagProc) {
        m_tagProc = tagProc;
    }

    public CustomTagProcessor getTagProcessor() {
        return m_tagProc;
    }

    public ScriptStatement getStatement(
        final Node n, final CustomTagProcessor tagProc
    ) throws CourierException {
        setTagProcessor(tagProc);
        return (ScriptStatement)processNode(n);
    }

    public static ScriptStatement getThroughChainedProc(
        XmlStatementFactory sf, Element n, CustomTagProcessor tagProc
    ) throws CourierException {
        ChainedTagProcessor newProc = new ChainedTagProcessor(
            sf.getTagProcessor(), tagProc
        );
        sf.setTagProcessor(newProc);
        ScriptStatement ret = null;
        try {
            ret = (ScriptStatement)sf.processNode(n);
        } finally {
            sf.setTagProcessor(newProc.getParent());
        }
        return ret;
    }

    public Object getObject(final Node n, final CustomTagProcessor tagProc)
    throws CourierException {
        setTagProcessor(tagProc);
        return processNode(n);
    }
}
