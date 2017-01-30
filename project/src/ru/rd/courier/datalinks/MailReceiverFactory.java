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
package ru.rd.courier.datalinks;

import ru.rd.courier.scripting.dataaccess.MailReceiver;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.pool.ObjectPoolIntf;
import org.w3c.dom.Node;

import java.util.List;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 29.06.2005
 * Time: 18:01:00
 */
public class MailReceiverFactory extends ReceiverFactory {
    private String[] m_to, m_cc, m_bcc;
    private String m_from;
    private List<String> m_smtpHosts;
    private String m_authType;
    private String m_username;
    private String m_password;

    public MailReceiverFactory(
        CourierLogger logger, List<String> smtpHosts,
        String[] to, String[] cc, String[] bcc, String from,
        String authType, String username, String password
    ) {
        super(logger, null);
        m_to = to;
        m_cc = cc;
        m_bcc = bcc;
        m_from = from;
        m_smtpHosts = smtpHosts;
        m_authType = authType;
        m_username = username;
        m_password = password;
    }

    public MailReceiverFactory(CourierLogger logger, Node conf) {
        this(
            logger,
            confHosts(conf),
            StringHelper.splitStringAndTrim(
                DomHelper.getNodeAttr(conf, "to", ""), ','),
            StringHelper.splitStringAndTrim(
                DomHelper.getNodeAttr(conf, "cc", ""), ','),
            StringHelper.splitStringAndTrim(
                DomHelper.getNodeAttr(conf, "bcc", ""), ','),
            DomHelper.getNodeAttr(conf, "from", ""),
            DomHelper.getNodeAttr(conf, "authtype", ""),
            DomHelper.getNodeAttr(conf, "username", ""),
            DomHelper.getNodeAttr(conf, "password", "")
        );
    }

    private static List<String> confHosts(Node conf) {
        List<String> ret = new LinkedList<String>();
        String host = DomHelper.getNodeAttr(conf, "smtp-host", null);
        if (host != null) ret.add(host);
        String hosts = DomHelper.getNodeAttr(conf, "smtp-hosts", null);
        if (hosts != null) {
            for (String h: StringHelper.splitStringAndTrim(hosts, ',')) ret.add(h);
        }
        return ret;
    }

    public Object getObject(ObjectPoolIntf pool) {
        try {
            return new MailReceiver(m_logger, m_to, m_cc, m_bcc, m_from, m_smtpHosts, m_authType, m_username, m_password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkObject(Object o) {
        MailReceiver r = (MailReceiver)o;
        r.ensureConnected();
        return true;
    }

    public String getDesc() {
        return (
               "From: " + m_from
            + " To:" + m_to
            + " SMTP Host:" + m_smtpHosts
        );
    }
}
