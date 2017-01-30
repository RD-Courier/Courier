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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.*;
import ru.rd.courier.scripting.expressions.string.Const;
import ru.rd.courier.scripting.statements.Nothing;
import ru.rd.courier.utils.DomHelper;

import java.util.*;

public class TargetProfile implements PortionFormatterProvider, StatementProvider {
    private CourierContext m_appl;
    private CourierLogger m_msgh;
    private String m_name;
    private String m_desc;
    private int m_waitTimeout;
    private int m_recLimit;
    private int m_bytesLimit;
    private final Set<String> m_sourceLinks;
    private final Set<String> m_targetLinks;

    private ScriptStatement m_beforePortionScript = null;
    private ScriptStatement m_afterPortionScript = null;
    private ScriptExpression m_beforePortion;
    private ScriptExpression m_afterPortion;

    private Map<String, ScriptStatement> m_stats = new HashMap<String, ScriptStatement>();

    public static final String c_bytesLimit = "$bytes-limit";
    public static final String c_recordsLimit = "$records-limit";

    public TargetProfile(final CourierContext appl, final CourierLogger msgh, final Node p) throws CourierException {
        m_msgh = msgh;
        setApplication(appl);
        setName(DomHelper.getNodeAttr(p, "name"));
        setWaitTimeout(Integer.parseInt(DomHelper.getNodeAttr(p, "wait-timeout")));
        setDesc(DomHelper.getChild(p, "description").getNodeValue());

        Element e;

        m_recLimit = -1;
        m_bytesLimit = -1;
        e = DomHelper.getChild(p, "portion", false);
        if (e != null) {
            String attrName;
            attrName = "records";
            if (e.hasAttribute(attrName)) {
                m_recLimit = Integer.parseInt(e.getAttribute(attrName));
            }
            attrName = "bytes";
            if (e.hasAttribute(attrName)) {
                m_bytesLimit = Integer.parseInt(e.getAttribute(attrName));
            }
            Element be = DomHelper.getChild(e, "before", false);
            if (be != null) {
                m_beforePortion = new PreparedTemplate(DomHelper.getNodeValue(be));
                //m_beforePortion = (ScriptExpression)getApplication().getStmtFactory().getObject(be, null);
            }
            be = DomHelper.getChild(e, "after", false);
            if (be != null) {
                m_afterPortion = new PreparedTemplate(DomHelper.getNodeValue(be));
                //m_afterPortion = (ScriptExpression)getApplication().getStmtFactory().getObject(be, null);
            }
            be = DomHelper.getChild(e, "before-script", false);
            if (be != null) m_beforePortionScript = createStatement(be.getFirstChild(), null);
            be = DomHelper.getChild(e, "after-script", false);
            if (be != null) m_afterPortionScript = createStatement(be.getFirstChild(), null);
        }
        if (m_beforePortionScript == null) m_beforePortionScript = new Nothing();
        if (m_afterPortionScript == null) m_afterPortionScript = new Nothing();
        if (m_beforePortion == null) m_beforePortion = new Const("");
        if (m_afterPortion == null) m_afterPortion = new Const("");

        ScriptStatement stmt;

        e = DomHelper.getChild(p, "before", false);
        stmt = (e == null) ? new Nothing() : createStatement(e.getFirstChild(), null);
        m_stats.put("before", stmt);

        e = DomHelper.getChild(p, "after", false);
        stmt = (e == null) ? new Nothing() : createStatement(e.getFirstChild(), null);
        m_stats.put("after", stmt);

        XmlStatementFactory.CustomTagProcessor tagProcessor = new XmlStatementFactory.CustomTagProcessor() {
            public ScriptStatement process(XmlStatementFactory sf, Element e) throws CourierException {
                ScriptStatement res = null;
                String tagName = getApplication().getScriptParam("std-update-operation-tag");
                if (e.getNodeName().equals(tagName)) {
                    res = new StandardUpdateOperation(m_appl, m_msgh, TargetProfile.this, e);
                }
                return res;
            }
        };

        e = (Element)DomHelper.getChild(p, "transform").getFirstChild();
        m_stats.put("data", createStatement(e, tagProcessor));

        m_sourceLinks = new HashSet<String>();
        m_targetLinks = new HashSet<String>();
        DomHelper.initDbLinks(
            DomHelper.getChild(p, "db-links", true), m_sourceLinks, m_targetLinks
        );
    }

    private CourierContext getApplication() {
        return m_appl;
    }

    private void setApplication(final CourierContext appl) {
        m_appl = appl;
    }

    private ScriptStatement createStatement(
        Node n, XmlStatementFactory.CustomTagProcessor tagProc
    ) throws CourierException {
        return getApplication().getStmtFactory().getStatement(n, tagProc);
    }

    public String getName() {
        return m_name;
    }

    public void setName(final String name) {
        m_name = name;
    }

    public String getDesc() {
        return m_desc;
    }

    public void setDesc(final String desc) {
        m_desc = desc;
    }

    public int getWaitTimeout() {
        return m_waitTimeout;
    }

    private void setWaitTimeout(final int waitTimeout) {
        m_waitTimeout = waitTimeout;
    }

    public ScriptStatement getStatement(final String name) {
        return m_stats.get(name);
    }

    public void start(Context ctx) throws CourierException {
        for (ScriptStatement scriptStatement : m_stats.values()) {
            scriptStatement.start(ctx);
        }
    }

    public void finish(Context ctx) throws CourierException {
        for (ScriptStatement scriptStatement : m_stats.values()) {
            scriptStatement.finish(ctx);
        }
    }

    public int getBytesLimit() {
        return m_bytesLimit;
    }

    public int getRecordsLimit() {
        return m_recLimit;
    }

    public PortionFormatter getPortionFormatter(final Context ctx) {
        return new PortionFormatter() {
            Context m_ctx = ctx;

            public String format(List<String> portion) {
                StringBuffer sb = new StringBuffer(portion.size());
                Iterator it = portion.iterator();
                boolean addReturn = false;
                while (it.hasNext()) {
                    if (addReturn) sb.append('\n');
                    sb.append(it.next());
                    addReturn = true;
                }
                return sb.toString();
            }
        };
    }

    public ScriptExpression getBeforePortion() {
        return m_beforePortion;
    }

    public ScriptExpression getAfterPortion() {
        return m_afterPortion;
    }

    public ScriptStatement getBeforePortionHandler() {
        return m_beforePortionScript;
    }

    public ScriptStatement getAfterPortionHandler() {
        return m_afterPortionScript;
    }

    public Set<String> getSourceLinks() {
        return m_sourceLinks;
    }

    public Set<String> getTargetLinks() {
        return m_targetLinks;
    }
}
