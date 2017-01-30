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

import ru.rd.courier.scripting.dataaccess.FileReceiver;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;
import org.w3c.dom.Node;

/**
 * User: AStepochkin
 * Date: 29.06.2005
 * Time: 18:19:26
 */
public class FileReceiverFactory extends FileBasedReceiverFactory {
    private final boolean m_append;
    private final boolean m_syncMkdir;

    public FileReceiverFactory(
        CourierLogger logger,
        String encoding,
        boolean overwrite, boolean overwriteWarning, boolean append,
        String prefix, String postfix, String dateFormat,
        String dir, boolean fileNameAtFirstLine, boolean syncMkdir
    ) {
        super(
            logger, encoding, overwrite, overwriteWarning,
            prefix, postfix, dir, fileNameAtFirstLine, dateFormat
        );
        m_append = append;
        m_syncMkdir = syncMkdir;
    }

    public FileReceiverFactory(CourierLogger logger, Node conf) {
        super(logger, conf);
        m_syncMkdir = DomHelper.getBoolYesNo(conf, "sync-mkdir");
        m_append = DomHelper.getBoolYesNo(conf, "append", false);
    }

    public Object getObject(ObjectPoolIntf pool) {
        return new FileReceiver(
            m_logger, m_encoding, m_overwrite, m_overwriteWarning, m_append,
            m_prefix, m_postfix, m_dir, m_fileNameAtFirstLine, m_dateFormat,
            m_syncMkdir
        );
    }

    public boolean checkObject(Object o) {
        return true;
    }
}
