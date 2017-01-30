/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.mail;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An HTML multipart email.
 *
 * <p>This class is used to send HTML formatted email.  A text message
 * can also be set for HTML unaware email clients, such as text-based
 * email clients.
 *
 * <p>This class also inherits from MultiPartEmail, so it is easy to
 * add attachents to the email.
 *
 * <p>To send an email in HTML, one should create a HtmlEmail, then
 * use the setFrom, addTo, etc. methods.  The HTML content can be set
 * with the setHtmlMsg method.  The alternate text content can be set
 * with setTextMsg.
 *
 * <p>Either the text or HTML can be omitted, in which case the "main"
 * part of the multipart becomes whichever is supplied rather than a
 * multipart/alternative.
 *
 * @author <a href="mailto:unknown">Regis Koenig</a>
 * @author <a href="mailto:sean@informage.net">Sean Legassick</a>
 * @version $Id: HtmlEmail.java,v 1.7 2003/10/12 09:41:36 rdonkin Exp $
 */
public class HtmlEmail extends MultiPartEmail
{
    /**
     * Text part of the message.  This will be used as alternative text if
     * the email client does not support HTML messages.
     */
    private String text;

    /** Html part of the message */
    private String html;

    /** Embeded images */
    private List inlineImages = new ArrayList();

    /**
     * Set the text content.
     *
     * @param text A String.
     * @return An HtmlEmail.
     */
    public HtmlEmail setTextMsg(String text)
    {
        this.text = text;
        return this;
    }

    /**
     * Set the HTML content.
     *
     * @param html A String.
     * @return An HtmlEmail.
     */
    public HtmlEmail setHtmlMsg(String html)
    {
        this.html = html;
        return this;
    }

    /**
     * Set the message.
     *
     * <p>This method overrides the MultiPartEmail setMsg() method in
     * order to send an HTML message instead of a full text message in
     * the mail body. The message is formatted in HTML for the HTML
     * part of the message, it is let as is in the alternate text
     * part.
     *
     * @param msg A String.
     * @return An Email.
     */
    public Email setMsg(String msg)
    {
        setTextMsg(msg);

        setHtmlMsg(new StringBuffer()
                .append("<html><body><pre>")
                .append(msg)
                .append("</pre></body></html>")
                .toString());

        return this;
    }

    /**
     * Embeds an URL in the HTML.
     *
     * <p>This method allows to embed a file located by an URL into
     * the mail body.  It allows, for instance, to add inline images
     * to the email.  Inline files may be referenced with a
     * <code>cid:xxxxxx</code> URL, where xxxxxx is the Content-ID
     * returned by the embed function.
     *
     * <p>Example of use:<br><code><pre>
     * HtmlEmail he = new HtmlEmail();
     * he.setHtmlMsg("&lt;html&gt;&lt;img src=cid:"+embed("file:/my/image.gif","image.gif")+"&gt;&lt;/html&gt;");
     * // code to set the others email fields (not shown)
     * </pre></code>
     *
     * @param url The URL of the file.
     * @param name The name that will be set in the filename header
     * field.
     * @return A String with the Content-ID of the file.
     * @exception MessagingException
     */
    public String embed(URL url, String name) throws MessagingException
    {
        MimeBodyPart mbp = new MimeBodyPart();

        mbp.setDataHandler (new DataHandler(new URLDataSource(url)));
        mbp.setFileName(name);
        mbp.setDisposition("inline");
        String cid = RandomStringUtils.randomAscii(10);
        mbp.addHeader("Content-ID", cid);

        inlineImages.add(mbp);
        return mbp.getContentID();
    }

    /**
     * Does the work of actually sending the email.
     *
     * @exception MessagingException if there was an error.
     */
    public void send() throws MessagingException
    {
        MimeMultipart container = this.getContainer();
        container.setSubType("related");

        BodyPart msgText = null;
        BodyPart msgHtml = null;

        if (StringUtils.isNotEmpty(html))
        {
            msgHtml = this.getPrimaryBodyPart();
            if (charset != null)
            {
                msgHtml.setContent(html, TEXT_HTML + ";charset=" + charset);
            }
            else
            {
                msgHtml.setContent(html, TEXT_HTML);
            }

            for (Iterator iter = inlineImages.iterator(); iter.hasNext();)
            {
                container.addBodyPart((BodyPart)iter.next());
            }

        }

        if (StringUtils.isNotEmpty(text))
        {
            // if the html part of the message was null, then the text part
            // will become the primary body part
            if (msgHtml == null)
            {
                msgText = getPrimaryBodyPart();
            }
            else
            {
                msgText = new MimeBodyPart();
                container.addBodyPart(msgText);
            }

            if (charset != null)
            {
                msgText.setContent(text, TEXT_PLAIN + ";charset=" + charset);
            }
            else
            {
                msgText.setContent(text, TEXT_PLAIN);
            }

        }

        super.send();
    }

    /**
     * Validates that the supplied string is neither <code>null</code>
     * nor the empty string.
     *
     * @param foo The text to check.
     * @return Whether valid.
     * @deprecated use StringUtils.isNotEmpty instead
     */
    public static final boolean isValid(String foo)
    {
        return StringUtils.isNotEmpty(foo);
    }
}
