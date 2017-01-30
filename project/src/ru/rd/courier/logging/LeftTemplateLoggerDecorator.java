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
package ru.rd.courier.logging;

import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringContext;
import ru.rd.courier.utils.templates.SimpleTemplate;

public class LeftTemplateLoggerDecorator extends AbstractLoggerDecorator {
    private final String m_decorStr;

    public LeftTemplateLoggerDecorator(CourierLogger logger, String decorStr) {
        super(logger);
        m_decorStr = decorStr;
    }

    public LeftTemplateLoggerDecorator(
        CourierLogger logger, String logDecoratingTemplate, StringContext ctx
    ) throws CourierException {
        this(logger, templateToString(logDecoratingTemplate, ctx));
    }

    private static String templateToString(
        String template, StringContext ctx
    ) throws CourierException {
        SimpleTemplate tmpl = new SimpleTemplate();
        return tmpl.process(template, ctx);
    }

    protected String getDecoratedMessage(String msg) {
        return m_decorStr + (msg == null ? "" : msg);
    }

    public void stop() {}
}
