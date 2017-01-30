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
import org.xml.sax.SAXException;
import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;

import ru.rd.utils.LogHelper;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Arrays;

import javax.xml.transform.TransformerException;
import java.util.Collection;
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
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.internal.filter.ValueNode.JsonNode;

/**
 * User: AStepochkin
 * Date: 27.06.2006
 * Time: 11:49:54
 */
public class JSONPathSource extends TimedStringReceiver implements DataSource {
    private final List<ColumnSelectInfo> m_cols;


    public static class ColumnSelectInfo {
        public final String m_name;
        public final String m_path;

        public ColumnSelectInfo(String name, String path) {
            m_name = name;
            m_path = path;
        }
    }

    private final CourierLogger m_logger;
    protected final String m_recordSelector;
    private String m_encoding = null;

    public JSONPathSource(
        CourierLogger logger, String recordSelector, List<ColumnSelectInfo> cols
    ) {
        m_logger = logger;
        m_recordSelector = recordSelector;
        m_cols = cols;
    }

    public void setEncoding(String encoding) {
        m_encoding = encoding;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        return null;
    }

    protected List<LinkWarning> timedFlush() {
        return null;
    }

    protected void timedClose() throws CourierException {}
    public void setTimeout(int timeout) throws CourierException {}
    public void cancel() throws CourierException {}

    public ResultSet request(String query) throws CourierException {
        try {
            return innerFileRequest(query);
        } catch(Exception e) {
            throw new CourierException(e);
        }
    }

    public final ResultSet getResultSet(InputStream is)
    throws IOException, SAXException {
		//try
		//LogHelper.logStdWarning("LOG0", null, null);
		ReadContext ctx = JsonPath.parse(is);
		//LogHelper.logStdWarning("LOG1", null, null);
		//List<JsonNode> arrNodes = ctx.read(m_recordSelector, List.class);
		Object items = ctx.read(m_recordSelector, Object.class);
		boolean fIsList = (items instanceof List || items instanceof List<?>);
		List<HashMap<String, Object>> lst = null;
		//LogHelper.logStdWarning("isArray: " + String.valueOf(fIsList), null, null);
		if(!fIsList)
		{
			HashMap<String, Object> hsmp = (HashMap<String, Object>)items;
			lst = Arrays.asList(hsmp);
		}
		else
			lst = (List<HashMap<String, Object>>)items;
		
		Iterator<HashMap<String, Object>> it = lst.iterator();
		//LogHelper.logStdWarning("FINISHED", null, null);
        return innerRequest(it);
    }

    private ResultSet innerFileRequest(String query)
    throws IOException, SAXException {
        return getResultSet(new BufferedInputStream(new FileInputStream(query)));
    }

    protected ResultSet innerRequest(final Iterator<HashMap<String, Object>> itr) {
        IterColumnInfo[] infos = new IterColumnInfo[m_cols.size()];
        int i = 0;
        for (ColumnSelectInfo ci: m_cols) {
            infos[i] = new IterColumnInfo(ci.m_name, 0);
            i++;
        }

        ResultSet rs;
        rs = new StringBufferedResultSet(null, infos) {
            protected boolean getRecord() throws SQLException {
                if (!itr.hasNext())
					return false;
				HashMap<String, Object> rn = itr.next();
                if (rn == null) 
					return false;

                int i = 1;
                for (ColumnSelectInfo ci: m_cols) {
                    String val = "";
                    try {
                        val = rn.get(ci.m_path).toString();
                    } catch (Exception e) {
                        SQLException sqle = new SQLException();
                        sqle.initCause(e);
                        throw sqle;
                    }
                    updateString(i, val);
                    i++;
                }
                return true;
            }

            protected int skipRecords(int count) {
                int c = 0;
                while (itr.hasNext() && (c < count))
					c++;
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
