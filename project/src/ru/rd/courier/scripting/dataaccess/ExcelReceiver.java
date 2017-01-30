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

import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.ExcelXmlFormatter;
import ru.rd.courier.utils.DomHelper;

import java.util.List;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * User: AStepochkin
 * Date: 05.06.2008
 * Time: 17:54:18
 */
public class ExcelReceiver extends TimedStringReceiver {
    private final CourierLogger m_logger;

    public ExcelReceiver(CourierLogger logger) {
        m_logger = logger;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        try {
            Element c = DomHelper.parseString(operation).getDocumentElement();
            OutputStream out = new FileOutputStream(DomHelper.getNodeAttr(c, "file"));
            ExcelXmlFormatter formatter = new ExcelXmlFormatter();
            try {
                formatter.fromXml(c, out);
            } finally {
                try { out.close(); } catch (Exception e) { m_logger.warning(e); }
            }
        } catch (Exception e) {
            throw new CourierException(e);
        }

        return null;
    }

    public List<LinkWarning> timedFlush() { return null; }
    public void setTimeout(int timeout) {}
    public void cancel() {}
    public void timedClose() {}
}
