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

import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xalan.extensions.XSLProcessorContext;
import org.apache.xpath.NodeSet;
import org.w3c.dom.Node;

import java.util.Set;

/**
 * User: AStepochkin
 * Date: 10.12.2007
 * Time: 15:45:34
 */
public class ApplyExcept {
    private Set<String> m_attrs = null;
    private Set<String> m_elems = null;

    private ApplyExcept(String attrs, String elems) {
        m_attrs = XalanFuns.getSet(attrs);
        m_elems = XalanFuns.getSet(elems);
    }

    public static ApplyExcept createAttrs(String attrs) {
        return new ApplyExcept(attrs, null);
    }

    public static ApplyExcept create(String elems) {
        return new ApplyExcept(null, elems);
    }

    public static ApplyExcept create(String elems, String attrs) {
        return new ApplyExcept(attrs, elems);
    }

    private void addAttrs(Node cn, NodeSet ns) {
        XalanFuns.addAttrs(cn, ns, m_attrs);
    }

    private void addElems(Node cn, NodeSet ns) {
        XalanFuns.addElems(cn, ns, m_elems);
    }

    public NodeSet excludeAttrs(ExpressionContext context, Node cn) {
        NodeSet ret = new NodeSet();
        addAttrs(cn, ret);
        return ret;
    }

    public NodeSet excludeChildren(ExpressionContext context, Node cn) {
        NodeSet ret = new NodeSet();
        addElems(cn, ret);
        return ret;
    }

    public NodeSet exclude(ExpressionContext context, Node cn) {
        NodeSet ret = new NodeSet();
        addAttrs(cn, ret);
        addElems(cn, ret);
        return ret;
    }
}
