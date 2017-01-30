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

import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.courier.utils.StreamHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.FileHelper;
import ru.rd.courier.jdbc.ResultSets.StringBufferListResultSet;
import ru.rd.courier.jdbc.EmptyResultSet;
import ru.rd.courier.logging.CourierLogger;

import java.sql.ResultSet;
import java.io.*;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 14.08.2006
 * Time: 11:59:37
 */
public class MoveToFileParser implements StreamParser {
    private final CourierLogger m_logger;
    private String m_fieldName;
    private File m_file;
    private InputStream m_is = null;
    private OutputStream m_os = null;

    public MoveToFileParser(CourierLogger logger, String fieldName, String file) {
        m_logger = logger;
        m_fieldName = fieldName;
        if (file != null) m_file = new File(file);
    }

    public void parseProperties(StringSimpleParser p) {
        Properties props = p.getProperties(null, '\'', "|");
        m_fieldName = StringHelper.stringParam(props, "field-name", m_fieldName);
        if (props.containsKey("file")) {
            m_file = new File(props.getProperty("file"));
        }
    }

    public ResultSet parse(InputStream is) throws IOException {
        FileHelper.safeMkdirs(m_file.getParentFile());
        OutputStream os = new FileOutputStream(m_file);
        synchronized(this) {
            m_is = is;
            m_os = os;
        }
        try {
            StreamHelper.transfer(is, m_os, 4*1024, null, false);
            if(m_fieldName != null) {
                return new StringBufferListResultSet(null, m_fieldName, m_file.getAbsolutePath());
            } else {
                return new EmptyResultSet();
            }
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        OutputStream os = null;
        synchronized(this) {
            if (m_os != null) {
                os = m_os;
                m_os = null;
            }
        }
        if (os != null) {
            try { os.close(); }
            catch (Exception e) {m_logger.warning(e);}
        }

        InputStream is = null;
        synchronized(this) {
            if (m_is != null) {
                is = m_is;
                m_is = null;
            }
        }
        if (is != null) {
            try { is.close(); }
            catch (Exception e) {m_logger.warning(e);}
        }
    }

    public void cancel() {
        cleanup();
    }
}
