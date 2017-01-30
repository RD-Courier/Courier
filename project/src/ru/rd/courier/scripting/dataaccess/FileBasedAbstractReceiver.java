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
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.scripting.LinkWarning;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 20.01.2005
 * Time: 20:41:39
 */
public abstract class FileBasedAbstractReceiver extends TimedStringReceiver {
    protected final CourierLogger m_logger;
    protected final String m_encoding;
    protected boolean m_overwrite;
    protected boolean m_overwriteWarning;
    protected final String m_prefix;
    protected final String m_postfix;
    protected final String m_dir;
    protected final boolean m_fileNameAtFirstLine;
    protected final DateFormat m_dateFormat;

    protected FileBasedAbstractReceiver(
        CourierLogger logger, String encoding,
        boolean overwrite, boolean overwriteWarning,
        String prefix, String postfix,
        String dir, boolean fileNameAtFirstLine,
        String dateFormat
    ) {
        m_logger = logger;
        m_encoding = encoding;
        m_overwrite = overwrite;
        m_overwriteWarning = overwriteWarning;
        m_prefix = prefix == null ? "" : prefix;
        m_postfix = postfix == null ? "" : postfix;
        if (dir.length() > 0 && !dir.endsWith("/")) dir += "/";
        m_dir = dir;
        m_fileNameAtFirstLine = fileNameAtFirstLine;
        if (!fileNameAtFirstLine) {
            m_dateFormat = new SimpleDateFormat(dateFormat);
        } else m_dateFormat = null;
    }

    protected abstract String getParamFileName();

    protected String getFullFileName(String fileName) {
        return (m_dir + fileName);
    }

    protected abstract void storeData(String fileName, String inputData) throws CourierException, IOException;

    public List<LinkWarning> timedProcess(String operation) throws CourierException {
        String fileName;
        String sendData;
        if (m_fileNameAtFirstLine) {
            SendInfo se = parse(operation);
            fileName = se.m_fileName;
            sendData = se.m_operation;
        } else {
            fileName = getParamFileName();
            sendData = operation;
        }

        try {
            boolean storeFile = true;
            if (needCheckFileExistance() && fileExists(fileName)) {
                storeFile = m_overwrite;
                if (m_overwriteWarning) {
                    m_logger.warning(
                        "Receiver (" + toString() + ") is overwriting file: " + fileName
                    );
                }
            }
            if (storeFile) {
                storeData(getFullFileName(fileName), sendData);
            }
        } catch (IOException e) {
            throw new CourierException(toString() + ": error sending data", e);
        }
        return null;
    }

    protected class SendInfo {
        public final String m_fileName;
        public final String m_operation;

        public SendInfo(String fileName, String operation) {
            m_fileName = fileName;
            m_operation = operation;
        }
    }

    protected SendInfo parse(String operation) throws CourierException {
        String sendData;
        final String fileNameIndicator = "file-name=";
        if (!operation.startsWith(fileNameIndicator)) {
            throw new CourierException(
                toString() + ": operation must start with '" + fileNameIndicator + "'");
        }
        String fileName;
        int firstLineEndPos = operation.indexOf('\n', fileNameIndicator.length());
        if (firstLineEndPos >= 0) {
            int endFileNamePos;
            if (operation.charAt(firstLineEndPos - 1) == '\r') endFileNamePos = firstLineEndPos - 1;
            else endFileNamePos = firstLineEndPos;
            fileName = operation.substring(fileNameIndicator.length(), endFileNamePos);
            sendData = operation.substring(firstLineEndPos + 1);
        } else {
            fileName = operation.substring(fileNameIndicator.length());
            sendData = "";
        }
        return new FtpReceiver.SendInfo(fileName, sendData);
    }

    protected boolean needCheckFileExistance() {
        return !m_overwrite || m_overwriteWarning;
    }

    protected abstract boolean fileExists(String fileName) throws IOException, CourierException;

}
