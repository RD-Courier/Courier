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

import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.Node;
import org.w3c.dom.DOMException;
import ru.rd.courier.utils.DomHelper;

/**
 * User: AStepochkin
 * Date: 27.11.2007
 * Time: 17:59:41
 */
public class XPathNodeIterator implements NodeIterator {
    private final NodeIterator m_iter;
    private Node m_lastText = null;

    public XPathNodeIterator(NodeIterator iter) {
        m_iter = iter;
    }

    public Node getRoot() {
        return m_iter.getRoot();
    }

    public int getWhatToShow() {
        return m_iter.getWhatToShow();
    }

    public NodeFilter getFilter() {
        return m_iter.getFilter();
    }

    public boolean getExpandEntityReferences() {
        return m_iter.getExpandEntityReferences();
    }

    public Node nextNode() throws DOMException {
        Node n;
        if (m_lastText == null) {
            n = m_iter.nextNode();
            if (DomHelper.isTextNode(n)) {
                m_lastText = n;
            }
        } else {
            n = m_lastText.getNextSibling();
            if (n == null || !DomHelper.isTextNode(n)) {
                m_lastText = null;
                n = m_iter.nextNode();
            } else {
                m_lastText = n;
            }
        }
        return n;
    }

    public Node previousNode() throws DOMException {
        throw new UnsupportedOperationException();
    }

    public void detach() {
        m_iter.detach();
    }
}
