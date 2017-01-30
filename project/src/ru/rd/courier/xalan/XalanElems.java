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

import org.apache.xalan.extensions.XSLProcessorContext;
import org.apache.xalan.templates.ElemExtensionCall;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;

import javax.xml.transform.TransformerException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 28.03.2008
 * Time: 16:22:49
 */
public class XalanElems {
    private static final Map<String, Object> s_props = new HashMap<String, Object>();

    public static String baseuri(XSLProcessorContext context, ElemExtensionCall elem) throws TransformerException {
        //String varname = elem.getAttribute("var", context.getContextNode(), context.getTransformer());

        System.out.println("BaseURLOfSource=" + context.getTransformer().getXPathContext().getSAXLocator().getSystemId());
        //System.out.println("BaseURLOfSource=" + context.getTransformer().getBaseURLOfSource());
        //System.out.println("StylesheetBaseURI=" + context.getTransformer().getStylesheet().getBaseURI());
        //System.out.println("CallBaseURI = " + call.getBaseURI());
        XPathContext xctxt = context.getTransformer().getXPathContext();
        DTM dtm = xctxt.getDTM(xctxt.getCurrentNode());
        //System.out.println("CurrentNode BaseURI = " + xctxt.getDTM(xctxt.getCurrentNode()).getDocumentBaseURI());
        //System.out.println("ContextNode BaseURI = " + xctxt.getDTM(xctxt.getContextNode()).getDocumentBaseURI());
        //System.out.println("CurrentExpressionNode BaseURI = " + xctxt.getDTM(xctxt.getCurrentExpressionNode()).getDocumentBaseURI());
        return "TEST";
    }

    public static String getvar(String name) {
        return (String)s_props.get(name);
    }
}
