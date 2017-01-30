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

import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.FileHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StreamHelper;
import ru.rd.courier.logging.test.NullLogger;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * User: AStepochkin
 * Date: 08.07.2005
 * Time: 14:50:51
 */
public class MailReceiverTest extends FileDataTestCase {
    private MailReceiver m_rec;
    private boolean m_needToDeleteprovidersFile;
    private File m_providersFile;

    // to test real smtp transport switch it off
    // but you have to disable all tests using mock transport,
    // for example all tests using checkMain
    private static final boolean m_useMockTransport = true;

    private static final String c_smtpClassPropName = "mail.smtp.class";
    private static final String c_smtpClass =
        "ru.rd.courier.scripting.dataaccess.TestTransport";

    protected void courierSetUp() throws MessagingException {
        m_count = 0;

        final String cHostName = "magician.rd.ru";
        if (m_useMockTransport) {
            System.setProperty(c_smtpClassPropName, c_smtpClass);
            m_providersFile = new File(
                System.getProperty("java.home") +
                File.separator + "lib" +
                File.separator + "javamail.providers"
            );

            /*
            if (m_providersFile.exists()) {
                fail(
                    "Mail providers file " + m_providersFile.getAbsolutePath() +
                    " already exists"
                );
            }
            */

            try {
                FileHelper.copyFile(getDataFile("javamail.providers"), m_providersFile);
            } catch (IOException e) {
                assertTrue(e.getMessage(), false);
            }
            m_needToDeleteprovidersFile = true;
        }
        m_rec = new MailReceiver(new NullLogger(), null, null, null, null, StringHelper.list(cHostName), null, null, null);
    }

    protected void courierTearDown() {
        if (m_useMockTransport) {
            System.setProperty(c_smtpClassPropName, "");
            if (m_needToDeleteprovidersFile) m_providersFile.delete();
        }
        try {
            m_rec.close();
        } catch (CourierException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        m_rec = null;
    }

    private int m_count;
    private String from, contentType;
    private String[] toAddresses, ccAddresses, bccAddresses;
    private static final String[] tagNames = new String[] {
        MailReceiver.cHeaderElem, MailReceiver.cBodyElem, MailReceiver.cAttachmentElem
    };
    private Map<String, StringBuffer> bufs = new HashMap<String, StringBuffer>();
    {
        for (int i = 0; i < tagNames.length; i++) {
            bufs.put(tagNames[i], new StringBuffer());
        }
    }

    private void clearBuffers() {
        buf.setLength(0);
        for (StringBuffer sb: bufs.values()) sb.setLength(0);
    }

    private StringBuffer getSubject() {
        return bufs.get(MailReceiver.cHeaderElem);
    }

    private StringBuffer getBody() {
        return bufs.get(MailReceiver.cBodyElem);
    }

    private StringBuffer getAttach() {
        return bufs.get(MailReceiver.cAttachmentElem);
    }

    private void appendToBuf(String name, String text) {
        buf.append('<'); buf.append(name); buf.append('>');
        buf.append(text);
        buf.append("</"); buf.append(name); buf.append(">\n");
        bufs.get(name).append(text);
    }

    private void appendSubject(String text) {
        appendToBuf(MailReceiver.cHeaderElem, text);
    }

    private void appendBody(String text) {
        appendToBuf(MailReceiver.cBodyElem, text);
    }

    private void appendAttach(String text) {
        appendToBuf(MailReceiver.cAttachmentElem, text);
    }

    private StringBuffer buf = new StringBuffer();

    private void setStdProps() {
        toAddresses = new String[] {"testemail@rdxxx.ru"};
        ccAddresses = new String[] {};
        bccAddresses = new String[] {};
        /*
        toAddresses = new String[] {"to1@rdxxx.ru", "to2@rdxxx.ru"};
        ccAddresses = new String[] {"cc1@rdxxx.ru", "cc2@rdxxx.ru"};
        bccAddresses = new String[] {"bcc1@rdxxx.ru"};
        */
        from = "testemail@rdxxx.ru";
        contentType = "text/plain";
    }

    private String getText() throws IOException, CourierException {
        String text = applyTemplate("test-mail.template", new String[][] {
              {"from", from}
            , {"to", StringHelper.glueStrings(toAddresses, ",")}
            , {"cc", StringHelper.glueStrings(ccAddresses, ",")}
            , {"bcc", StringHelper.glueStrings(bccAddresses, ",")}
            , {"content-type", contentType}
            , {"main", buf.toString()}
        });

        setFileText("request.out", text, "cp1251");
        return text;
    }

    private static final Address[] c_emptyAddressArray = new Address[0];
    private void checkAddressArrays(Object[] arr1, Object[] arr2) {
        if (arr1 == null) arr1 = c_emptyAddressArray;
        if (arr2 == null) arr2 = c_emptyAddressArray;
        checkArraysAsStrings(arr1, arr2);
    }

    private Message getLastMessage() {
        List<Message> mlist = TestTransport.getMessages();
        return mlist.get(mlist.size() - 1);
    }

    private static final String c_fileAttachFileNamePrefix = "file-";
    private File getAttachFile(String name) {
        return getTempFile(c_fileAttachFileNamePrefix + name);
    }

    private boolean isFilePart(BodyPart part) throws MessagingException {
        return part.getFileName() != null &&
            part.getFileName().startsWith(c_fileAttachFileNamePrefix);
    }

    private void checkMain() throws MessagingException, IOException {
        Message msg = getLastMessage();
        assertEquals(contentType, msg.getContentType());
        assertEquals(from, msg.getFrom()[0].toString());
        checkAddressArrays(toAddresses, msg.getRecipients(MimeMessage.RecipientType.TO));
        checkAddressArrays(ccAddresses, msg.getRecipients(MimeMessage.RecipientType.CC));
        checkAddressArrays(bccAddresses, msg.getRecipients(MimeMessage.RecipientType.BCC));

        assertEquals(getSubject().toString(), msg.getSubject());
        MimeMultipart mmp = (MimeMultipart) msg.getContent();
        //System.out.println(mmp.getBodyPart(0).getContent());
        assertEquals(getBody().toString(), mmp.getBodyPart(0).getContent());
        BodyPart attachPart = null;
        for (int i = 0; i < mmp.getCount(); i++) {
            BodyPart bp = mmp.getBodyPart(i);
            if (
                Part.ATTACHMENT.equals(bp.getDisposition()) && !isFilePart(bp)
            ) {
                attachPart = bp;
                break;
            }
        }
        if (getAttach().length() == 0) {
            assertNull(attachPart);
        } else {
            assertEquals(getAttach().toString(), attachPart.getContent());
        }
    }

    public void testFormMail()
    throws IOException, CourierException, InterruptedException, MessagingException {
        setStdProps();
        iterateArray(bufs.size()*2, 1, new CombinationHandler() {
            public void execute(int[] arr) throws Exception {
                clearBuffers();
                for (int c = 0; c < arr.length; c++) {
                    for (int i = 0; i < arr[c]; i++) {
                        String tag = tagNames[c%bufs.size()];
                        appendToBuf(tag, tag + "-" + m_count + "." + i);
                    }
                }
                m_rec.process(getText());
                checkMain();
                m_count++;
            }
        });
    }

    public void testFileAttachment()
        throws IOException, CourierException, MessagingException
    {
        setStdProps();

        File f = getAttachFile("attach.txt");
        OutputStream attachOut = new FileOutputStream(f);
        try {
            for (int i = 0; i < 100; i++) {
                attachOut.write('4');
            }
        } finally {
            attachOut.close();
        }

        clearBuffers();
        appendSubject("test subject");
        appendBody("test body");
        buf.append(
            "<" + MailReceiver.cAttachmentElem +
            " file=\"" + f.getAbsolutePath() + "\"/>"
        );
        m_rec.process(getText());
        checkMain();
        Message msg = getLastMessage();
        MimeMultipart mmp = (MimeMultipart) msg.getContent();
        InputStream fis = new FileInputStream(f);
        InputStream partIS = mmp.getBodyPart(1).getInputStream();
        try {
            assertTrue(StreamHelper.compare(partIS, fis));
        } finally {
            fis.close();
            partIS.close();
        }
    }

    public void _testAddresses() throws IOException, CourierException, InterruptedException, MessagingException {
        setStdProps();

        clearBuffers();
        toAddresses = new String[] {/*"crap@yyy.ru.", */"testemail@rdxxx.ru"};
        appendSubject("test subject");
        appendBody("test body");
        try {
            m_rec.process(getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        checkMain();

        System.out.println("1 -->");
        Thread.sleep(10*1000);
        System.out.println("2 <--");

        clearBuffers();
        toAddresses = new String[] {"testemail@rdxxx.ru"};
        appendSubject("test subject");
        appendBody("test body");
        m_rec.process(getText());
        checkMain();
    }

    public void _testConnection() throws IOException, CourierException, InterruptedException {
        setStdProps();

        final int attemptsCount = 3;
        for (int i = 0; i < attemptsCount; i++) {
            clearBuffers();
            appendSubject("test subject");
            appendBody("test body");
            try {
                m_rec.process(getText());
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(i + " sent");
            if (i < (attemptsCount - 1)) {
                System.out.println("sleep begin");
                Thread.sleep(20*1000);
                System.out.println("sleep end");
            }
        }
    }
}
