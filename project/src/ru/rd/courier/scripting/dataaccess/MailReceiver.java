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

import org.w3c.dom.Element;
import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.ExcelXmlFormatter;
import ru.rd.courier.utils.StringContext;
import ru.rd.courier.utils.MailConnection;
import ru.rd.courier.utils.templates.HashMapStringContext;
import ru.rd.courier.utils.templates.SimpleTemplate;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownServiceException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

public class MailReceiver extends TimedStringReceiver {
    private CourierLogger m_logger;
    private MailConnection m_con;
    private String[] m_to, m_cc, m_bcc;
    private String m_from;
    private final List<String> m_smtpHosts;
    private final SimpleTemplate m_template;
    private final String m_authType;
    private final String m_username;
    private final String m_password;

    public MailReceiver(
        CourierLogger logger,
        String[] to, String[] cc, String[] bcc,
        String from, List<String> smtpHosts,
        String authType, String username, String password
    ) {
        m_logger = logger;
        m_to = to;
        m_cc = cc;
        m_bcc = bcc;
        m_from = from;
        m_smtpHosts = smtpHosts;
        m_template = new SimpleTemplate("{%", "}");
        m_authType = authType;
        m_username = username;
        m_password = password;
        connect();
    }

    private Element parseMail(String op) throws CourierException {
        try {
            return DomHelper.parseString(
                "<?xml version=\"1.0\" encoding=\"windows-1251\" ?> " +
                "<root>" + op + "</root>"
            ).getDocumentElement();
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    private static class ByteArrayDataSource implements DataSource {
        private String m_contentType;
        private byte[] m_data;

        public ByteArrayDataSource(byte[] data) {
            m_data = data;
        }

        public ByteArrayDataSource(byte[] data, String contentType) {
            this(data);
            setContentType(contentType);
        }

        public String getContentType() {
            return m_contentType == null ? "application/octet-stream" : m_contentType;
        }

        public void setContentType(String contentType) {
            m_contentType = contentType;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(m_data);
        }

        public String getName() {
            return "ByteArrayDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnknownServiceException();
        }
    }

    private void formExcelAttach(MimeBodyPart mbp, StringBuffer textAttach) throws CourierException {
        /*
        try {
            Writer = new OutputStreamWriter(new FileOutputStream("temp.xml"));
            fos.write(textAttach.toString());
            fos.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        */

        Element c = parseMail(textAttach.toString());
        c = DomHelper.getChild(c, "workbook", true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ExcelXmlFormatter formatter = new ExcelXmlFormatter();
            formatter.fromXml(c, out);

            mbp.setDataHandler(new DataHandler(
                new ByteArrayDataSource(out.toByteArray(), "application/vnd.ms-excel")
            ));
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    static final String cPropertiesElem = "properties";
    static final String cHeaderElem = "header";
    static final String cBodyElem = "body";
    static final String cAttachmentElem = "attachment";
    static final String cContentTypeAttr = "content-type";
    static final String cFromAttr = "from";
    static final String cDefaultContentType = "text/html; charset=Windows-1251";

    private Address getAddress(String addr) throws UnsupportedEncodingException {
        return new InternetAddress(addr, null);
    }

    private InternetAddress getAddressWithName(String addr) throws AddressException {
        return new InternetAddress(addr);
    }

    private void addAttachments(Element mailOp, MimeMultipart mp)
    throws MessagingException, CourierException, IOException {
        MimeBodyPart mbp;
        StringBuffer textAttach = new StringBuffer();
        String textAttachType = null;
        String textAttachMimeType = null;
        String textAttachDisplayName = null;
        Element[] attachs = DomHelper.getChildrenByTagName(mailOp, cAttachmentElem, false);
        for (int i = 0; i < attachs.length; i++) {
            Element ae = attachs[i];
            if (ae.hasAttribute("file")) {
                mbp = new MimeBodyPart();
                FileDataSource fds = new FileDataSource(
                    DomHelper.getNodeAttr(ae, "file")
                );
                mbp.setDataHandler(new DataHandler(fds));
                String displayName;
                if (ae.hasAttribute("display-name")) {
                    displayName = ae.getAttribute("display-name");
                } else {
                    displayName = fds.getName();
                }
                mbp.setFileName(displayName);
                mbp.setDisposition(Part.ATTACHMENT);
                mp.addBodyPart(mbp);
            } else {
                textAttach.append(DomHelper.getNodeValue(ae));
                if (ae.hasAttribute("type")) {
                    textAttachType = ae.getAttribute("type");
                }
                if (ae.hasAttribute("content-type")) {
                    textAttachMimeType = ae.getAttribute("content-type");
                }
                if (ae.hasAttribute("display-name")) {
                    textAttachDisplayName = ae.getAttribute("display-name");
                }
            }
        }

        if (textAttach.length() > 0) {
            if (textAttachType == null) {
                textAttachType = "html";
            }
            if (textAttachMimeType == null) {
                textAttachMimeType = "text/html; charset=Windows-1251";
            }
            if (textAttachDisplayName == null) {
                textAttachDisplayName = "attach.html";
            }

            mbp = new MimeBodyPart();
            if (textAttachType.equalsIgnoreCase("html")) {
                mbp.setContent(textAttach.toString(), textAttachMimeType);
            } else if (textAttachType.equalsIgnoreCase("excel")) {
                formExcelAttach(mbp, textAttach);
            }

            mbp.setFileName(textAttachDisplayName);
            mbp.setDisposition(Part.ATTACHMENT);
            mp.addBodyPart(mbp);
        }
    }

    private void setMailAddresses(
        final MimeMessage email, final Element propsElem
    ) throws MessagingException {
        email.addRecipients(Message.RecipientType.TO, propsElem.getAttribute("to"));
        email.addRecipients(Message.RecipientType.CC, propsElem.getAttribute("cc"));
        email.addRecipients(Message.RecipientType.BCC, propsElem.getAttribute("bcc"));
    }

    private Message formMail(String operation)  throws MessagingException, IOException,
        CourierException, UnknownHostException, UnsupportedEncodingException
    {
        
        MimeMessage email = null;
        Element mailOp = parseMail(operation);

        String from = m_from;
        final Element propsElem = DomHelper.getChild(mailOp, cPropertiesElem, false);
        String mainContentType = cDefaultContentType;
        if (propsElem != null) {
            if (propsElem.hasAttribute(cFromAttr))
                from = propsElem.getAttribute(cFromAttr);
            if (propsElem.hasAttribute(cContentTypeAttr)) {
                mainContentType = propsElem.getAttribute(cContentTypeAttr);
            }
        }
        if (mainContentType.matches(".*multipart.*")) {
            InputStream stream = new ByteArrayInputStream(DomHelper.getElementsValue(mailOp, cBodyElem).getBytes());
            email = m_con.createMessage(stream);
        }
        else {
            email = m_con.createMessage();
            MimeMultipart mp = new MimeMultipart();
            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setContent(
                DomHelper.getElementsValue(mailOp, cBodyElem), mainContentType);
            mp.addBodyPart(mbp);
            email.setContent(mp);
        }
        if(propsElem != null) setMailAddresses(email, propsElem);

        MimeMultipart mp = (MimeMultipart)email.getContent();
        addAttachments(mailOp, mp);

        //email.setFrom(getAddress(from));
        InternetAddress pAddr = getAddressWithName(from);
        email.setFrom(new InternetAddress(pAddr.getAddress(), pAddr.getPersonal(), "UTF8"));
        email.setSentDate(new Date());

        String host = InetAddress.getLocalHost().getHostName();
        StringContext ctx = new HashMapStringContext();
        ctx.setVar("courier-host", host);
        email.setSubject(m_template.process(
            DomHelper.getElementsValue(mailOp, cHeaderElem), ctx
        ));


        if (m_to != null) for (String addr: m_to)
            email.addRecipient(Message.RecipientType.TO, getAddress(addr));
        if (m_cc != null) for (String addr: m_cc)
            email.addRecipient(Message.RecipientType.CC, getAddress(addr));
        if (m_bcc != null) for (String addr: m_bcc)
            email.addRecipient(Message.RecipientType.BCC, getAddress(addr));

        email.saveChanges();

        return email;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        try {
            ensureConnected();
            Message email = formMail(operation);
            m_con.send(email);
        } catch (Exception e) {
            throw new CourierException(e);
        }
        return null;
    }

    protected List<LinkWarning> timedFlush() {
        return null;
    }

    public void ensureConnected() {
        if (!isConnected()) connect();
    }

    private void connect() {
        disconnect();
        for (String host: m_smtpHosts) {
            try {
                MailConnection con = new MailConnection(host, m_authType, m_username, m_password);
                con.ensureConnected();
                m_con = con;
                break;
            } catch (MessagingException e) { m_logger.warning(e); }
        }
        if (m_con == null) {
            throw new CourierException("Failed to connect to smtp host");
        }
    }

    private void disconnect() {
        try { if (isConnected()) m_con.close(); }
        catch (MessagingException e) { m_logger.warning(e); }
        m_con = null;
    }

    public boolean isConnected() {
        return m_con != null && m_con.isConnected();
    }

    protected void timedClose() {
        disconnect();
    }

    public void setTimeout(int timeout) throws CourierException {
    }

    public void cancel() throws CourierException {
    }
}
