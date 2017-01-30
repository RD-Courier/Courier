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

import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 10.08.2006
 * Time: 9:34:28
 */
public class UrlStreamConnector extends UrlStreamProperties implements StreamConnector {
    public UrlStreamConnector(UrlStreamProperties props) {
        super(props);
    }

    public void parseProperties(StringSimpleParser p) {
        p.skipBlanks();
        if (p.beyondEnd()) return;
        int pos = p.getPos();

        if (p.thisText("http", false))
            m_url = p.shiftWordOrBracketedString('\'');
        pos = p.getPos();
        Properties props = p.getProperties(null, '\'', "|");
        p.setPos(pos);

        m_url = StringHelper.stringParam(props, "url", m_url);
        m_httpMethod = StringHelper.stringParam(props, "http-method", m_httpMethod);
        m_contentType = StringHelper.stringParam(props, "content-type", m_contentType);
        m_requestContent = StringHelper.stringParam(props, "request-content", m_requestContent);
    }

    public InputStream createStream() throws IOException, CourierException {
        URL url = new URL(m_url);
        HttpURLConnection con;
        String contentType = m_contentType;

        if (m_proxyHost != null) {
            Proxy proxy = new Proxy(
                Proxy.Type.HTTP, new InetSocketAddress(m_proxyHost, m_proxyPort)
            );
            con = (HttpURLConnection)url.openConnection(proxy);
        } else {
            con = (HttpURLConnection)url.openConnection();
        }
        con.setConnectTimeout(m_connectTimeout);
        con.setReadTimeout(m_readTimeout);
        con.setRequestMethod(m_httpMethod);

        if (m_httpMethod == "POST"){
            if (contentType == null) contentType = "application/x-www-form-urlencoded";
            if (m_requestContent == null) throw new CourierException("request-content is required for http POST");
        }
        if (contentType != null) con.setRequestProperty( "Content-Type", contentType); 
        con.setRequestProperty( "Content-Encoding", "utf-8");

        if(m_requestContent != null){
            byte[] contentData       = m_requestContent.getBytes(StandardCharsets.UTF_8);
            int    contentDataLength = contentData.length;
            con.setDoOutput(true);
            con.setRequestProperty("Content-Length", Integer.toString(contentDataLength));
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(contentData);
        }

        InputStream is = con.getInputStream();

        if (m_typeFilter != null) {
            String ct = con.getContentType();
            if (ct == null) throw new RuntimeException("Resource has not returned content type for type filter");
            if (!m_typeFilter.isTrue(ct)) throw new RuntimeException("Content type " + ct + " is prohibited");
        }
        return new BufferedInputStream(is);
    }

    public void cancel() {}
}
