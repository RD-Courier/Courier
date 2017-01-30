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

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.utils.LogHelper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DomHelper {
    public static boolean isTextNode(Node n)  {
      if (n == null) return false;
      short nodeType = n.getNodeType();
      return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE;
    }

    public static class SimpleErrorHandler implements org.xml.sax.ErrorHandler {
        public SAXParseException error = null;

        private void setError(SAXParseException exception) {
            if (error == null) error = exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            setError(exception);
        }

        public void error(SAXParseException exception) throws SAXException {
            setError(exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            setError(exception);
        }
    }

    public static class LoggingErrorHandler implements org.xml.sax.ErrorHandler {
        private SAXParseException m_error;
        private final CourierLogger m_logger;

        public LoggingErrorHandler(CourierLogger logger) {
            m_logger = logger;
            reset();
        }

        public void warning(SAXParseException e) {
            m_logger.warning(e);
        }

        public void error(SAXParseException e) {
            if (m_error == null) m_error = e;
            else m_logger.error(e);
        }

        public void fatalError(SAXParseException e) {
            if (m_error == null) m_error = e;
            else m_logger.error(e);
        }

        public SAXParseException getError() {
            return m_error;
        }

        public void reset() {
            m_error = null;
        }
    }

    public static void serialize(final Node node, final OutputStream os) throws IOException {
        final Serializer ser = SerializerFactory.getSerializer(
            OutputPropertiesFactory.getDefaultMethodProperties("xml")
        );
        ser.setOutputStream(os);
        Properties props = new Properties();
        props.setProperty("indent", "yes");
        ser.setOutputFormat(props);
        ser.asDOMSerializer().serialize(node);
    }

    public static void serialize(final Node node, final Writer os) throws IOException {
        final Serializer ser = SerializerFactory.getSerializer(
            OutputPropertiesFactory.getDefaultMethodProperties("xml")
        );
        ser.setWriter(os);
        Properties props = new Properties();
        props.setProperty("indent", "yes");
        ser.setOutputFormat(props);
        ser.asDOMSerializer().serialize(node);
    }

    public static void serialize(final Node node, final String file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        try {
            serialize(node, os);
        } finally {
            try {
                os.close();
            } catch (Exception e) {
                LogHelper.logStdWarning("Failed to close file '{0}'", file, e);
            }
        }
    }

    public static String serializeToString(Node node) throws IOException {
        StringWriter w = new StringWriter();
        serialize(node, w);
        return w.toString();
    }

    public static DocumentBuilder getParser(File schemaFile, ErrorHandler eh, boolean namespaceAware) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(namespaceAware);
            if (schemaFile != null) {
                Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaFile);
                dbf.setSchema(schema);
            }
            DocumentBuilder parser = dbf.newDocumentBuilder();
            if (eh != null) parser.setErrorHandler(eh);
            return parser;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DocumentBuilder getParser(org.xml.sax.ErrorHandler eh) {
        return getParser(null, eh, true);
    }

    public static DocumentBuilder getParser() {
        return getParser(null);
    }

    public static Validator getValidator(String schemaFile, org.xml.sax.ErrorHandler eh) {
        try {
            Schema schema = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI
            ).newSchema(new File(schemaFile));

            Validator validator = schema.newValidator();
            validator.setErrorHandler(eh);

            return validator;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Document parseXmlFile(final File f) throws IOException, SAXException {
        SimpleErrorHandler eh = new SimpleErrorHandler();
        Document doc = parseXmlFile(f, null, eh);
        if (eh.error != null) throw eh.error;
        return doc;
    }

    public static Document parseXmlFile(
        final File f, final File schemaFile, final ErrorHandler er
    ) throws IOException, SAXException {
        return getParser(schemaFile, er, true).parse(f);
    }

    public static Document parseReader(final Reader is) throws IOException, SAXException {
        return getParser().parse(new InputSource(is));
    }

    public static Document parseStream(final InputStream is) throws IOException, SAXException {
        return parseStreamEx(is);
    }

    public static Document parseStreamEx(
        final InputStream is, final ErrorHandler errHandler, String encoding
    ) throws IOException, SAXException {
        InputSource xis = new InputSource(is);
        if (encoding != null) xis.setEncoding(encoding);
        return getParser(errHandler).parse(is);
    }

    public static Document parseStreamEx(final InputStream is) throws IOException, SAXException {
        SimpleErrorHandler eh = new SimpleErrorHandler();
        Document doc = parseStreamEx(is, eh);
        if (eh.error != null) throw eh.error;
        return doc;
    }

    public static Document parseStreamEx(
        final InputStream is, final ErrorHandler errHandler
    ) throws IOException, SAXException {
        return parseStreamEx(is, errHandler, null);
    }

    public static Document parseString(final String xml) throws IOException, SAXException {
        SimpleErrorHandler eh = new SimpleErrorHandler();
        Document doc = getParser(eh).parse(new InputSource(new StringReader(xml)));
        if (eh.error != null) throw eh.error;
        return doc;
    }

    public static Element[] getChildrenByTagName(
        final Node p, final String tagName, final boolean needRes
    ) {
        final List<Element> ret = new LinkedList<Element>();
        Node n = p.getFirstChild();
        while (n != null) {
            if (
                (n.getNodeType() == Node.ELEMENT_NODE) &&
                (n.getNodeName().equals(tagName))
            ) {
                ret.add((Element)n);
            }
            n = n.getNextSibling();
        }
        if (needRes && (ret.size() == 0)) {
            throw new RuntimeException("DOM elements '" + tagName + "' not found");
        }
        return ret.toArray(new Element[ret.size()]);
    }

    public static Element[] getChildrenByTagName(final Node p, final String tagName) {
        return getChildrenByTagName(p, tagName, true);
    }

    public static String[] getNodeAttrList(final Node n, final String name) {
        String av = getNodeAttr(n, name, null);
        if (av == null || av.length() == 0) {
            return new String[0];
        } else {
            return av.split("\\s*,\\s*");
        }
    }

    public static Set<String> getNodeAttrSet(final Element n, final String name) {
        Set<String> ret = new HashSet<String>();
        String av = n.getAttribute(name);
        if (av != null && av.length() > 0) {
            for(String s: av.split("\\s*,\\s*")) {
                ret.add(s);
            }
        }
        return ret;
    }

    public static String getNodeAttr(final Node n, final String name, final boolean mustExist) {
        final Node attr = n.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (mustExist) {
                throw new RuntimeException("Attribute '" + name + "' not found");
            }
            return null;
        }
        return attr.getNodeValue();
    }

    public static String getNodeAttr(Node n, String name, String def) {
        final Node attr = n.getAttributes().getNamedItem(name);
        if (attr == null) return def;
        return attr.getNodeValue();
    }

    public static boolean hasAttr(Node n, String name) {
        return ((Element)n).hasAttribute(name);
    }

    public static long getLongNodeAttr(Node n, String name, boolean mustExist) {
        String strValue = getNodeAttr(n, name, mustExist);
        if (strValue == null) return 0;
        else return Long.parseLong(strValue);
    }

    public static long getLongNodeAttr(Node n, String name, long def) {
        String strValue = getNodeAttr(n, name, false);
        if (strValue == null) return def;
        else return Long.parseLong(strValue);
    }

    public static long getUpLongNodeAttr(Node n, String name, String stopTag, long def) {
        String strValue = getUpNodeAttr((Element)n, name, stopTag, null);
        if (strValue == null) return def;
        else return Long.parseLong(strValue);
    }

    public static long getLongNodeAttr(Node n, String name) {
        return getLongNodeAttr(n, name, true);
    }

    public static int getIntNodeAttr(Node n, String name, int def) {
        String strValue = getNodeAttr(n, name, false);
        if (strValue == null) {
            return def;
        } else {
            return Integer.parseInt(strValue);
        }
    }

    public static int getUpIntNodeAttr(Node n, String name, String stopTag, int def) {
        String strValue = getUpNodeAttr((Element)n, name, stopTag, null);
        if (strValue == null) {
            return def;
        } else {
            return Integer.parseInt(strValue);
        }
    }

    public static int getIntNodeAttr(Node n, String name, boolean mustExist) {
        String strValue = getNodeAttr(n, name, mustExist);
        if (strValue == null) {
            return 0;
        } else {
            return Integer.parseInt(strValue);
        }
    }

    public static int getIntNodeAttr(Node n, String name) {
        return getIntNodeAttr(n, name, true);
    }

    public static long getTimeNodeAttr(Node n, String name, long def) {
        String strValue = getNodeAttr(n, name, false);
        if (strValue == null) {
            return def;
        } else {
            return StringHelper.parseTime(strValue);
        }
    }

    public static long getTimeNodeAttr(Node n, String name, long def, String defUnit) {
        String strValue = getNodeAttr(n, name, false);
        if (strValue == null) {
            return def;
        } else {
            return StringHelper.parseTime(strValue, defUnit);
        }
    }

    public static long getUpTimeNodeAttr(Node n, String name, String stopTag, long def) {
        String strValue = getUpNodeAttr((Element)n, name, stopTag, null);
        if (strValue == null) {
            return def;
        } else {
            return StringHelper.parseTime(strValue);
        }
    }

    public static String getNodeAttr(final Node n, final String name) {
        return getNodeAttr(n, name, true);
    }

    public static Element getChildNS(final Node p, final String ns, final String tagName) {
        for (Node n = p.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                final Element e = (Element)n;
                if (e.getLocalName().equals(tagName) && e.getNamespaceURI().equals(ns)) {
                    return e;
                }
            }
        }
        return null;
    }

    public static Element getChild(final Node p, final String tagName) {
        return getChild(p, tagName, true);
    }

    public static Element getChild(final Node p, final String tagName, Element def) {
        Element e = getChild(p, tagName, false);
        if (e == null) return def;
        return e;
    }

    public static Element getChild(final Node p, final String[] tagNames) {
        Element n = (Element)p;
        for (String tag : tagNames) {
            n = getChild(n, tag, true);
        }
        return n;
    }

    public static Element getChild(
        final Node p, final String tagName, final boolean mustExist
    ) {
        final NodeList nl = p.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                final Element e = (Element)nl.item(i);
                if (e.getTagName().equals(tagName)) {
                    return e;
                }
            }
        }
        if (mustExist) {
            throw new RuntimeException("No '" + tagName + "' element found in " + getNodePath(p));
        }
        return null;
    }

    public static Element getChild(
        final Node p, final String tagName, String attrName, String attrValue, final boolean mustExist
    ) {
        for (Element e: elements(p)) {
            if (
                (tagName == null || e.getTagName().equals(tagName)) &&
                e.hasAttribute(attrName) &&
                e.getAttribute(attrName).equals(attrValue)
            ) return e;
        }
        if (mustExist) {
            throw new RuntimeException(
                "Element '" + tagName + "' with attribute '" + attrName + "' = '" + attrValue + "' not found");
        }
        return null;
    }

    public static Element getChildInsensitive(
        final Node p, final String tagName, String attrName, String attrValue, final boolean mustExist
    ) {
        for (Element e: elements(p)) {
            if (
                (tagName == null || e.getTagName().equalsIgnoreCase(tagName)) &&
                e.hasAttribute(attrName) &&
                e.getAttribute(attrName).equalsIgnoreCase(attrValue)
            ) return e;
        }
        if (mustExist) {
            throw new RuntimeException(
                "Element '" + tagName + "' with attribute '" + attrName + "' = '" + attrValue + "' not found");
        }
        return null;
    }

    public static Element getUpChild(
        Element fromElem, final String tagName, String stopTag, final boolean mustExist
    ) {
        if (fromElem == null) throw new IllegalArgumentException();
        Element p = fromElem;
        while (p != null) {
            Element e = getChild(p, tagName, false);
            if (e != null) return e;
            if ((stopTag != null) && p.getTagName().equals(stopTag)) break;
            p = (Element)p.getParentNode();
        }
        if (mustExist) {
            throw new RuntimeException(
                "No '" + tagName + "' element found inside" +
                " of or above '" + fromElem.getTagName() + "'"
            );
        }
        return null;
    }

    public static Element getUpChild(
        Element p, final String tagName, final boolean mustExist
    ) {
        return getUpChild(p, tagName, null, mustExist);
    }

    public static List<Element> getUpChilds(
        Element fromElem, final String tagName, String stopTag, final boolean mustExist
    ) {
        if (fromElem == null) throw new IllegalArgumentException();
        List<Element> ret = new LinkedList<Element>();
        Element p = fromElem;
        while (p != null) {
            Element e = getChild(p, tagName, false);
            if (e != null) ret.add(0, e);
            if ((stopTag != null) && p.getTagName().equals(stopTag)) break;
            p = (Element)p.getParentNode();
        }
        if (mustExist && (ret.size() == 0)) {
            throw new RuntimeException(
                "No '" + tagName + "' element found inside" +
                " of or above '" + fromElem.getTagName() + "'"
            );
        }
        return ret;
    }

    public static Element getFirstElement(final Node e) {
        Element ret = null;
        Node node = e.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                ret = (Element)node;
                break;
            }
            node = node.getNextSibling();
        }
        return ret;
    }

    public static String getNodeValue(final Node n) {
        if (n == null) return "";
        if (n.getNodeType() != Node.ELEMENT_NODE) return n.getNodeValue();

        final StringBuffer sb = new StringBuffer();
        for (Node cur = n.getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (
                    (cur.getNodeType() == Node.TEXT_NODE) ||
                    (cur.getNodeType() == Node.CDATA_SECTION_NODE)
            ) {
                sb.append(cur.getNodeValue());
            }
        }
        return sb.toString();
    }

    public static String getChildValue(Node n, String name, boolean mustExists) {
        Node chn = getChild(n, name, mustExists);
        if (chn == null) return null;
        return getNodeValue(chn);
    }

    public static String getChildValue(Node n, String name) {
        return getChildValue(n, name, true);
    }

    public static String getChildValue(Node n, String name, String def) {
        Node chn = getChild(n, name, false);
        if (chn == null) return def;
        return getNodeValue(chn);
    }

    public static List<String> getChildrenValues(Node n, String tagName) {
        List<String> res = new LinkedList<String>();
        for (Element e: getChildrenByTagName(n, tagName, false)) {
            res.add(getNodeValue(e));
        }
        return res;
    }

    public static String getElementsValue(final Node p, final String tagName) {
        StringBuffer ret = new StringBuffer();
        for (Element e : getChildrenByTagName(p, tagName, false)) {
            ret.append(getNodeValue(e));
        }
        return ret.toString();
    }

    public static Map<String, String> buffersToStrings(Map<String, StringBuffer> buffers) {
        Map<String, String> ret = new HashMap<String, String>();
        Iterator<Map.Entry<String, StringBuffer>> it = buffers.entrySet().iterator();
        for (; it.hasNext(); ) {
            Map.Entry<String, StringBuffer> e = it.next();
            ret.put(e.getKey(), e.getValue().toString());
        }
        return ret;
    }

    public static ElementIterable elements(Node node, String tagName) {
        return new ElementIterable(node, tagName);
    }

    public static ElementIterable elements(Node node) {
        return new ElementIterable(node);
    }

    public static class ElementIterable implements Iterable<Element> {
        private final Node m_node;
        private final String m_tagName;

        public ElementIterable(Node node, String tagName) {
            m_node = node;
            m_tagName = tagName;
        }

        public ElementIterable(Node node) {
            this(node, null);
        }

        public Iterator<Element> iterator() {
            return new ElementIterator(m_node, m_tagName);
        }
    }

    public static class ElementIterator implements Iterator<Element> {
        private Node m_n;
        private String m_tagName;

        private void findNext() {
            for (; m_n != null; m_n = m_n.getNextSibling()) {
                if (m_n.getNodeType() == Node.ELEMENT_NODE) {
                    if (m_tagName == null || m_tagName.equals(m_n.getNodeName())) return;
                }
            }
        }

        public ElementIterator(Node e, String tagName) {
            m_tagName = tagName;
            m_n = e.getFirstChild();
            findNext();
        }

        public ElementIterator(Node e) {
            this(e, null);
        }

        public boolean hasNext() {
            return m_n != null;
        }

        public Element next() {
            Element ret = (Element)m_n;
            m_n = m_n.getNextSibling();
            findNext();
            return ret;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static Map<String, String> xmlToSections(Element e) {
        Map<String, StringBuffer> ret = new HashMap<String, StringBuffer>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (!ret.containsKey(n.getNodeName())) {
                    ret.put(n.getNodeName(), new StringBuffer());
                }
                ret.get(n.getNodeName()).append(DomHelper.getNodeValue(n));
            }
        }
        return buffersToStrings(ret);
    }

    public static Properties getElementParams(Node n) {
        Properties ret = new Properties();
        for (Element e : getChildrenByTagName(n, "param", false)) {
            String value = getNodeAttr(e, "value", false);
            if (value == null) value = getNodeValue(e);
            ret.setProperty(getNodeAttr(e, "name"), value);
        }
        return ret;
    }

    public static Map<String, String> getElementParams(Node n, Map<String, String> props) {
        for (Element e : getChildrenByTagName(n, "param", false)) {
            String value = getNodeAttr(e, "value", false);
            if (value == null) value = getNodeValue(e);
            props.put(getNodeAttr(e, "name"), value);
        }
        return props;
    }

    private static Properties getAttrParams(Node n, Properties props) {
        NamedNodeMap attrs =  n.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            props.setProperty(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
        }
        return props;
    }

    public static Map<String, String> getAttrParams(Node n, Map<String, String> props) {
        NamedNodeMap attrs =  n.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            props.put(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
        }
        return props;
    }

    public static Properties getAttrParams(Node n) {
        return getAttrParams(n, new Properties());
    }

    public static Properties getAllParams(Node n) {
        Properties ret = getElementParams(n);
        getAttrParams(n, ret);
        return ret;
    }

    public static Map<String, String> getAllParams(Node n, Map<String, String> props) {
        getElementParams(n, props);
        getAttrParams(n, props);
        return props;
    }

    public static Map<String, String> getAllParamMap(Node n) {
        Map<String, String> res = new HashMap<String, String>();
        return getAllParams(n, res);
    }

    public static final String c_NodeTagName = "field";
    public static final String c_ClassNameAttrName = "class-name";

    public static void convertToXml(Object o, Element to) {
        try {
            if (o == null) {
                to.setAttribute("null", "yes");
                return;
            }
            to.setAttribute(c_ClassNameAttrName, o.getClass().getName());
            Document doc = to.getOwnerDocument();
            if (o.getClass().isArray()) {
                for (int ai = 0; ai < Array.getLength(o); ai++) {
                    Element ae = doc.createElement(c_NodeTagName);
                    Object ao = Array.get(o, ai);
                    convertPrimitiveToXml(o.getClass().getComponentType(), ao, ae);
                    to.appendChild(ae);
                }
            } else {
                for (Field f : o.getClass().getFields()) {
                    Element e = doc.createElement(c_NodeTagName);
                    e.setAttribute("name", f.getName());
                    convertPrimitiveToXml(f.getType(), f.get(o), e);
                    to.appendChild(e);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void convertPrimitiveToXml(Class declClass, Object o, Element e) {
        if (o == null) {
            e.setAttribute("null", "yes");
            return;
        }
        if (declClass.isPrimitive() || String.class.isAssignableFrom(declClass)) {
            //e.appendChild(e.getOwnerDocument().createTextNode(o.toString()));
            e.setAttribute("value", o.toString());
        } else {
            convertToXml(o, e);
        }
    }

    public static Object restoreFromXml(Element e) throws CourierException {
        return restoreFromXml(e, getNodeAttr(e, c_ClassNameAttrName));
    }

    private static final Map<Class, Class> m_primitiveToWrapper = new HashMap<Class, Class>();
    static {
        m_primitiveToWrapper.put(Boolean.TYPE, Boolean.class);
        m_primitiveToWrapper.put(Character.TYPE, Character.class);
        m_primitiveToWrapper.put(Byte.TYPE, Byte.class);
        m_primitiveToWrapper.put(Short.TYPE, Short.class);
        m_primitiveToWrapper.put(Integer.TYPE, Integer.class);
        m_primitiveToWrapper.put(Long.TYPE, Long.class);
        m_primitiveToWrapper.put(Float.TYPE, Float.class);
        m_primitiveToWrapper.put(Double.TYPE, Double.class);
        m_primitiveToWrapper.put(String.class, String.class);
    }

    public static Object restoreFromXml(Element e, String className) throws CourierException {
        Object ret;
        try {
            Class cl = Class.forName(className);
            ret = cl.newInstance();
            for (Element fe : getChildrenByTagName(e, c_NodeTagName, false)) {
                Field f = cl.getDeclaredField(getNodeAttr(fe, "name"));
                String fClassName = getNodeAttr(fe, c_ClassNameAttrName, false);
                if (fClassName == null) {
                    restoreArrayFromXml(f, fe, ret);
                } else {
                    f.set(ret, restoreFromXml(fe, fClassName));
                }
            }
        } catch (Exception e1) {
            throw new CourierException(e1);
        }
        return ret;
    }

    private static void restoreArrayFromXml(Field f, Element fe, Object ret) throws CourierException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        if (f.getType().isArray()) {
            Element[] arrnl = getChildrenByTagName(fe, c_NodeTagName, false);
            if (arrnl.length == 0) {
                f.set(ret, null);
            } else {
                Object arr = Array.newInstance(f.getType().getComponentType(), arrnl.length);
                for (int j = 0; j < arrnl.length; j++) {
                    Element ae = arrnl[j];
                    String afClassName = getNodeAttr(ae, c_ClassNameAttrName, false);
                    if (afClassName == null) {
                        Array.set(arr, j, restoreValueFromXml(f.getType().getComponentType(), ae));
                    } else {
                        Array.set(arr, j, restoreFromXml(ae, afClassName));
                    }
                }
                f.set(ret, arr);
            }
        } else {
            f.set(ret, restoreValueFromXml(f.getType(), fe));
        }
    }

    private static Object restoreValueFromXml(Class cl, Element e) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class primClass = m_primitiveToWrapper.get(cl);
        if (primClass == Character.class) {
            return primClass.getConstructor(char.class).newInstance(
                    e.getAttribute("value").charAt(0));
        } else {
            return primClass.getConstructor(String.class).newInstance(
                    e.getAttribute("value"));
        }
    }

    public static boolean getBoolYesNo(Node n, String attrName, boolean defaultValue) {
        if (!((Element)n).hasAttribute(attrName)) return defaultValue;
        return getBoolYesNo(n, attrName);
    }

    public static boolean getUpBoolYesNo(
        Node n, String attrName, String stopTag, boolean defaultValue
    ) {
        String val = getUpNodeAttr((Element)n, attrName, stopTag, null);
        if (val == null) return defaultValue;
        return val.equalsIgnoreCase("yes");
    }

    public static boolean getBoolYesNo(Node n, String attrName) {
        String val = getNodeAttr(n, attrName, false);
        return ((val != null) && val.equalsIgnoreCase("yes"));
    }

    public static boolean getUpBoolYesNo(Node n, String attrName, String stopTag) {
        String val = getUpNodeAttr((Element)n, attrName, stopTag, null);
        return ((val != null) && val.equalsIgnoreCase("yes"));
    }

    public static String nodeToString(Node n) {
        StringBuffer out = new StringBuffer();
        nodeToString(n, out, false);
        return out.toString();
    }

    public static void nodeToString(Node n, StringBuffer out, boolean showSelf) {
        if (showSelf) {
            out.append("<").append(n.getNodeName());
            NamedNodeMap as = n.getAttributes();
            for (int ai = 0; ai < as.getLength(); ai++) {
                Node an = as.item(ai);
                out.append(" ");
                out.append(an.getNodeName());
                out.append("=\"");
                out.append(an.getNodeValue());
                out.append("\"");
            }
            out.append(">");
        }
        for (Node cur = n.getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (
                (cur.getNodeType() == Node.TEXT_NODE) ||
                (cur.getNodeType() == Node.CDATA_SECTION_NODE)
            ) {
                out.append(cur.getNodeValue());
            } else if (cur.getNodeType() == Node.ELEMENT_NODE) {
                nodeToString(cur, out, true);
            }
        }
        if (showSelf) {
            out.append("</");
            out.append(n.getNodeName());
            out.append(">");
        }
    }

    public static String nodeToEscString(Node n) {
        StringBuffer out = new StringBuffer();
        nodeToEscString(n, out, 0, "");
        return out.toString();
    }

    public static void nodeToEscString(Node n, StringBuffer out, int level, String amps) {
        boolean showSelf = level > 0;
        String curAmps = "&" + amps;
        String nextAmps;
        if (showSelf) nextAmps = amps + "amp;";
        else nextAmps = amps;

        if (showSelf) {
            out.append(curAmps).append("lt;").append(n.getNodeName());
            NamedNodeMap as = n.getAttributes();
            for (int ai = 0; ai < as.getLength(); ai++) {
                Node an = as.item(ai);
                out.append(" ");
                out.append(an.getNodeName());
                out.append("=\"");
                out.append(an.getNodeValue());
                out.append("\"");
            }
            out.append(curAmps).append("gt;");
        }
        for (Node cur = n.getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (
                (cur.getNodeType() == Node.TEXT_NODE) ||
                (cur.getNodeType() == Node.CDATA_SECTION_NODE)
            ) {
                out.append(cur.getNodeValue());
            } else if (cur.getNodeType() == Node.ELEMENT_NODE) {
                nodeToEscString(cur, out, level + 1, nextAmps);
            }
        }
        if (showSelf) {
            out.append(curAmps);
            out.append("lt;/");
            out.append(n.getNodeName());
            out.append(curAmps);
            out.append("gt;");
        }
    }

    public static String nodeToTwoLevelEscString(Node n) {
        StringBuffer out = new StringBuffer();
        nodeToTwoLevelEscString(n, out, 0);
        return out.toString();
    }

    public static void nodeToTwoLevelEscString(Node n, StringBuffer out, int level) {
        boolean showSelf = level > 0;
        String lt, gt;
        if (level <= 1) {
            lt = "<";
            gt = ">";
        } else /*if (level == 2)*/ {
            lt = "&lt;";
            gt = "&gt;";
        }
        /*
        else {
            lt = "&amp;lt;";
            gt = "&amp;gt;";
        }
        */
        if (showSelf) {
            out.append(lt).append(n.getNodeName());
            NamedNodeMap as = n.getAttributes();
            for (int ai = 0; ai < as.getLength(); ai++) {
                Node an = as.item(ai);
                out.append(" ");
                out.append(an.getNodeName());
                out.append("=\"");
                out.append(an.getNodeValue());
                out.append("\"");
            }
            out.append(gt);
        }
        for (Node cur = n.getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (cur.getNodeType() == Node.TEXT_NODE) {
                out.append(cur.getNodeValue());
            } else if (cur.getNodeType() == Node.CDATA_SECTION_NODE) {
                out.append("<![CDATA[");
                out.append(cur.getNodeValue());
                out.append("]]>");
            } else if (cur.getNodeType() == Node.ELEMENT_NODE) {
                if (((Element)cur).hasAttribute("as-text")) {
                    nodeToString(cur);
                } else {
                    nodeToTwoLevelEscString(cur, out, level + 1);
                }
            }
        }
        if (showSelf) {
            out.append(lt);
            out.append("/");
            out.append(n.getNodeName());
            out.append(gt);
        }
    }

    public static Set<String> initSet(Node conf) {
        Set<String> res = new HashSet<String>();
        for (Node n = conf.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            res.add(n.getTextContent());
        }
        return res;
    }

    public static void initDbLinks(Node conf, Set<String> sources, Set<String> targets) {
        for (Node n = conf.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            final String type = ((Element)n).getAttribute("type");
            if (type.equals("source")) sources.add(n.getTextContent());
            else targets.add(n.getTextContent());
        }
    }

    public static String getUpNodeAttr(Element e, String name, String stopTag, String def) {
        while (e != null) {
            if (e.hasAttribute(name)) return e.getAttribute(name);
            if ((stopTag != null) && e.getTagName().equals(stopTag)) break;
            e = (Element)e.getParentNode();
        }
        return def;
    }

    public static String getReqUpNodeAttr(Element e, String name, String stopTag) {
        String ret = getUpNodeAttr(e, name, stopTag, null);
        if (ret == null) throw new RuntimeException("Up Attribute '" + name + "' not found");
        return ret;
    }

    public static String getNodePath(Node node) {
        StringBuffer sb = new StringBuffer(16);
        Node n = node;
        while (n != null) {
            sb.insert(0, n.getNodeName() + (sb.length() > 0 ? "/" : ""));
            n = n.getParentNode();
        }
        int np = 0;
        n = node;
        while (n != null) {
            np++;
            n = n.getPreviousSibling();
        }
        sb.append("#").append(np);
        return sb.toString();
    }

    public static void importChildren(Node parent1, Node parent2) {
        Document doc2 = parent2.getOwnerDocument();
        for (Node n = parent1.getFirstChild(); n != null; n = n.getNextSibling()) {
            parent2.appendChild(doc2.importNode(n, true));
        }
    }

    public static void insertChildren(Node parent1, Node parent2, Node before) {
        Document doc2 = parent2.getOwnerDocument();
        for (Node n = parent1.getFirstChild(); n != null; n = n.getNextSibling()) {
            parent2.insertBefore(doc2.importNode(n, true), before);
        }
    }

    public static void insertChildrenAtStart(Node parent1, Node parent2) {
        insertChildren(parent1, parent2, parent2.getFirstChild());
    }

    public static void importChildrenInstead(Node parent1, Node parent2, String replacement) {
        Document doc2 = parent2.getOwnerDocument();
        Node replNode = getChild(parent2, replacement, true);
        for (Node n = parent1.getFirstChild(); n != null; n = n.getNextSibling()) {
            parent2.insertBefore(doc2.importNode(n, true), replNode);
        }
        parent2.removeChild(replNode);
    }

    public static void importChildrenElements(Node parent1, Node parent2) {
        Document doc2 = parent2.getOwnerDocument();
        for (Node n = parent1.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                parent2.appendChild(doc2.importNode(n, true));
            }
        }
    }

    public static void propogateAttrs(Node source, Element target) {
        NamedNodeMap sas = source.getAttributes();
        for (int i = 0; i < sas.getLength(); i++) {
            Attr a = (Attr)sas.item(i);
            final String name = a.getName();
            if (!target.hasAttribute(name)) {
                target.setAttribute(name, a.getValue());
            }
        }
    }

    public static void attrsToChildren(Element e) {
        Node n = e.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                propogateAttrs(e, (Element)n);
            }
            n = n.getNextSibling();
        }
    }

    private static final String s_escChars = "\"<";
    private static final String[] s_escStrings = new String[] {"&quot;", "&lt;"};
    public static String escString(String s) {
        return StringHelper.replaceChars(s, s_escChars, s_escStrings);
    }

    public static void collectNamespaces(Element e, Map<String, String> namespaces) {
        NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node an = attrs.item(i);
            if (an.getNodeName().startsWith("xmlns:")) {
                namespaces.put(an.getNodeName().substring(0, 6), an.getNodeValue());
            }
        }
    }
}
