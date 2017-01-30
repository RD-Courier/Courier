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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.FileBasedAbstractReceiver;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.DomHelper;
import org.w3c.dom.Node;

/**
 * User: AStepochkin
 * Date: 29.06.2005
 * Time: 18:27:38
 */
public abstract class FileBasedReceiverFactory extends ReceiverFactory {
    protected String m_encoding;
    protected boolean m_overwrite;
    protected boolean m_overwriteWarning;
    protected String m_prefix;
    protected String m_postfix;
    protected String m_dir;
    protected String m_dateFormat;
    protected boolean m_fileNameAtFirstLine;

    public FileBasedReceiverFactory(
        CourierLogger logger,
        String encoding,
        boolean overwrite, boolean overwriteWarning,
        String prefix, String postfix,
        String dir, boolean fileNameAtFirstLine,
        String dateFormat
    ) {
        super(logger, null);

        if(!fileNameAtFirstLine) {
            if (prefix == null) {
                throw new IllegalArgumentException(toString() + ": prefix unspecified");
            }
            if (postfix == null) {
                throw new IllegalArgumentException(toString() + ": postfix unspecified");
            }
            if (dateFormat == null) {
                throw new IllegalArgumentException(toString() + ": dateFormat unspecified");
            }
        }

        m_encoding = encoding;
        m_overwrite = overwrite;
        m_overwriteWarning = overwriteWarning;
        m_prefix = prefix;
        m_postfix = postfix;
        if (!dir.endsWith("/")) dir += "/";
        m_dir = dir;
        m_fileNameAtFirstLine = fileNameAtFirstLine;
        m_dateFormat = dateFormat;
    }

    public FileBasedReceiverFactory(
        CourierLogger logger, Node conf
    ) {
        super(logger, null);
        initXmlProps(conf);
    }


    protected final void initXmlProps(Node conf) {
        m_encoding = DomHelper.getNodeAttr(conf, "encoding", "cp1251");
        m_overwrite = DomHelper.getBoolYesNo(conf, "overwrite", true);
        m_overwriteWarning = DomHelper.getBoolYesNo(conf, "overwrite-warning", false);
        m_prefix = DomHelper.getNodeAttr(conf, "prefix", false);
        m_postfix = DomHelper.getNodeAttr(conf, "postfix", false);
        m_dir = DomHelper.getNodeAttr(conf, "dir", "");
        if (m_dir.length() > 0 && !m_dir.endsWith("/")) m_dir += "/";
        m_fileNameAtFirstLine = DomHelper.getBoolYesNo(conf, "filename-at-first-line");
        m_dateFormat = DomHelper.getNodeAttr(conf, "date-format", false);
        if(!m_fileNameAtFirstLine) {
            if (m_prefix == null) {
                m_prefix = "";
            }
            if (m_postfix == null) {
                m_postfix = "";
            }
            if (m_dateFormat == null) {
                m_dateFormat = "yyyyMMdd-HHmmssSSS";
            }
        }
    }

    public String getDesc() {
        return (
            "Dir: " + m_dir
        );
    }
}
