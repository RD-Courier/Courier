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
import ru.rd.courier.scripting.XmlStatementFactory;
import ru.rd.courier.utils.DomHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SourceProfile {
    private Application m_appl;
    private CourierLogger m_msgh;
    private String m_name;
    private String m_desc;
    private int m_waitTimeout;
    private Map m_rules = new HashMap();

    public SourceProfile(final Application appl, final CourierLogger msgh, final Node n) throws CourierException {
        setApplication(appl);
        m_msgh = msgh;
        setName(DomHelper.getNodeAttr(n, "name"));
        setWaitTimeout(Integer.parseInt(DomHelper.getNodeAttr(n, "wait-timeout")));
        setDesc(DomHelper.getChildrenByTagName(n, "description", true)[0].getNodeValue());
        final Element[] nl;
        nl = DomHelper.getChildrenByTagName(DomHelper.getChild(n, "rules"), "rule");
        for (int i = 0; i < nl.length; i++) {
            addRule(new SourceRule(this, msgh, nl[i]));
        }
    }

    public void register(final String pipeName) throws CourierException {
        for(Iterator it = m_rules.values().iterator(); it.hasNext(); ) {
            final SourceRule rule = (SourceRule)it.next();
            rule.register(pipeName);
        }
    }

    public CourierContext getApplication() {
        return m_appl;
    }

    public void setApplication(final Application appl) {
        m_appl = appl;
    }

    public SourceRule getSourceRule(final String name) {
        SourceRule ret = (SourceRule)m_rules.get(name);
        if (ret == null) {
            throw new RuntimeException(
                "Source rule '" + name + "' of profile '" + getName() + "' not found"
            );
        }
        return ret;
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

    private void setDesc(final String desc) {
        m_desc = desc;
    }

    public int getWaitTimeout() {
        return m_waitTimeout;
    }

    private void setWaitTimeout(final int waitTimeout) {
        m_waitTimeout = waitTimeout;
    }

    private void addRule(final SourceRule r) {
        m_rules.put(r.getName(), r);
    }

    public XmlStatementFactory getStmtFactory() {
        return m_appl.getStmtFactory();
    }
}
