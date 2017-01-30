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

import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.logging.CourierLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;

public class FileReceiver extends FileBasedAbstractReceiver {
    private final boolean m_syncMkdir;
    private final boolean m_append;

    public FileReceiver(
        CourierLogger logger, String encoding,
        boolean overwrite, boolean overwriteWarning, boolean append,
        String prefix, String postfix,
        String dir, boolean fileNameAtFirstLine,
        String dateFormat, boolean syncMkdir
    ) {
        super(
            logger, encoding, overwrite, overwriteWarning, prefix,
            postfix, dir, fileNameAtFirstLine, dateFormat
        );
        m_append = append;
        m_syncMkdir = syncMkdir;
        File dirFile = new File(m_dir);
        if (m_syncMkdir) {
            synchronized(getClass()) {
                if (!dirFile.exists()) {
                  dirFile.mkdirs();
                }
            }
        } else {
            if (!dirFile.exists()) {
              dirFile.mkdirs();
            }
        }
    }

    protected String getParamFileName() {
        return (
            m_prefix +
            m_dateFormat.format(new Date()) +
            (m_append ? "" : "-" + ((int)(Math.random() * 100000000))) + 
            m_postfix
        );
    }

    protected void storeData(String fileName, String inputData) throws CourierException, IOException {
        File f = new File(fileName);
        File p = f.getParentFile();
        if (p != null) {
            if (m_syncMkdir) {
                synchronized(getClass()) {
                    if (!p.exists()) p.mkdirs();
                }
            } else {
                if (!p.exists()) p.mkdirs();
            }
        }
        PrintStream out = null;
        FileOutputStream fout = new FileOutputStream(f, m_append);
        try {
            out = new PrintStream(fout, false, m_encoding);
            out.print(inputData);
            out.flush();
        } finally {
            try {
                if (out != null) out.close();
            } catch(Exception e) { m_logger.warning(e); }
            try {
                fout.close();
            } catch(Exception e) { m_logger.warning(e); }
        }
    }

    protected boolean fileExists(String fileName) throws IOException, CourierException {
        return (new File(fileName)).exists();
    }

    public List<LinkWarning> timedFlush() {
        return null;
    }

    public void setTimeout(int timeout) throws CourierException {
    }

    public void cancel() throws CourierException {
    }

    public void timedClose() {
    }

}
