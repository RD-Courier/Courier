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
package ru.rd.courier.scripting.statements;

import org.apache.commons.mail.SimpleEmail;
import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.ScriptExpression;

import javax.mail.MessagingException;

public class SendMail implements ScriptStatement {
    private final ScriptExpression m_smtpHost;
    private final ScriptExpression m_from;
    private final ScriptExpression m_fromName;
    private final ScriptExpression m_to;
    private final ScriptExpression m_subject;
    private final ScriptExpression m_text;

    public SendMail(
        ScriptExpression smtpHost,
        ScriptExpression from, ScriptExpression fromName,
        ScriptExpression to, ScriptExpression subject,
        ScriptExpression text
    ) {
        m_smtpHost = smtpHost;
        m_from = from;
        m_fromName = fromName;
        m_to = to;
        m_subject = subject;
        m_text = text;
    }

    public void start(Context ctx) throws CourierException {
    }

    public void finish(final Context ctx) throws CourierException {
    }

    public void exec(Context ctx) throws CourierException {
        SimpleEmail email = new SimpleEmail();
        try {
            email.setHostName(m_smtpHost.calculate(ctx));
            if (m_fromName == null) email.setFrom(m_from.calculate(ctx));
            else email.setFrom(m_from.calculate(ctx), m_fromName.calculate(ctx));
            email.addTo(m_to.calculate(ctx));
            email.setSubject(m_subject.calculate(ctx));
            email.setMsg(m_text.calculate(ctx));
            email.send();
        } catch(MessagingException e) {
            throw new CourierException("Error while sending e-mail", e);
        }

    }

}
