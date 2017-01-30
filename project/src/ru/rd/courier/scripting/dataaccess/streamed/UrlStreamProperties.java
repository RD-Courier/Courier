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
package ru.rd.courier.scripting.dataaccess.streamed;

import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringChecker;
import ru.rd.courier.utils.ReqExpChecker;
import ru.rd.courier.utils.InverseStringChecker;

import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 10.08.2006
 * Time: 10:03:56
 */
public class UrlStreamProperties {
    protected String m_url;
    protected final StringChecker m_typeFilter;
    protected final String m_proxyHost;
    protected final int m_proxyPort;
    protected final int m_connectTimeout;
    protected final int m_readTimeout;

    //COUR-9
    //2016-12-14	Lobanov
    protected String m_httpMethod;
    protected String m_contentType;
    protected String m_requestContent;

    public UrlStreamProperties(
        String url, StringChecker typeFilter, String proxyHost, int proxyPort,
        int connectTimeout, int readTimeout,
        String httpMethod, String contentType, String requestContent 
    ) {
        m_url = url;
        m_typeFilter = typeFilter;
        m_proxyHost = proxyHost;
        m_proxyPort = proxyPort;
        m_connectTimeout = connectTimeout;
        m_readTimeout = readTimeout;

        m_httpMethod = httpMethod;
        m_contentType = contentType;
        m_requestContent = requestContent;
    }

    private static StringChecker confStringChecker(Properties props) {
        String re = StringHelper.stringParam(props, "type-regex", null);
        if (re == null) return null;
        StringChecker sc = new ReqExpChecker(re);
        if (StringHelper.boolParam(props, "inverse-type-check", false)) sc = new InverseStringChecker(sc);
        return sc;
    }

    public UrlStreamProperties(Properties props) {
        this(
            StringHelper.stringParam(props, "url", null),
            confStringChecker(props),
            StringHelper.stringParam(props, "proxy-host", null),
            StringHelper.intParam(props, "proxy-port", 0),
            StringHelper.intParam(props, "connect-timeout", 0),
            StringHelper.intParam(props, "read-timeout", 0),

            StringHelper.stringParam(props, "http-method", "GET"),
            StringHelper.stringParam(props, "content-type", null),
            StringHelper.stringParam(props, "request-content", null)
        );
    }

    public UrlStreamProperties(UrlStreamProperties props) {
        this(
            props.m_url, props.m_typeFilter, props.m_proxyHost, props.m_proxyPort,
            props.m_connectTimeout, props.m_readTimeout, 
            props.m_httpMethod, props.m_contentType, props.m_requestContent 
        );
    }
}
