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

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 0:23:16
 */
public class DaysLogCleaner {
    private final File m_dir;
    private final int m_days;
    private long m_maxSize;
    private final String m_prefix;
    private final String m_postfix;
    private final boolean m_deleteUnknownFiles;
    private final DateFormat m_dateFormat;
    private DayProvider m_dayProvider;
    private DateProvider m_dateProvider;
    private final CleanLogHandler m_handler;

    private static final String c_ExceededFileSuffix = "-exceeded";

    private FileSizeStreamDelegate m_stream;
    private int m_curDay;

    public static interface CleanLogHandler {
        void setOutputStream(OutputStream stream);
        void reportError(String message, Exception e);
    }

    public DaysLogCleaner(
        CleanLogHandler handler,
        File dir, String dateFormat, int days,
        String prefix, String postfix, boolean deleteUnknownFiles, boolean append,
        long maxSize
    ) {
        super();
        m_handler = handler;
        m_deleteUnknownFiles = deleteUnknownFiles;
        m_dir = dir;
        if (!m_dir.exists()) {
            m_dir.mkdirs();
        }
        m_dateFormat = new SimpleDateFormat(dateFormat);
        if (days < 1) {
            throw new IllegalArgumentException(
                "Days (" + days + ") cannot be less then 1"
            );
        }
        m_days = days;
        m_maxSize = maxSize;
        m_prefix = prefix;
        m_postfix = postfix;
        m_dayProvider = new CalendarDayProvider();
        m_dateProvider = new TrivialDateProvider();
        m_curDay = getCurDay();

        cleanUpDir();
        try { rotate(append); }
        catch (Exception e) { error(null, e); }
    }

    private void error(String message, Exception e) {
        m_handler.reportError(message, e);
    }

    public void setMaxSize(long maxSize) {
        m_maxSize = maxSize;
    }

    void setDayProvider(DayProvider dayProvider) {
        m_dayProvider = dayProvider;
    }

    void setDateProvider(DateProvider dateProvider) {
        m_dateProvider = dateProvider;
    }

    private int getCurDay() {
        return m_dayProvider.getDay();
    }

    private Date fileNameToDate(String fname) {
        Date dt = null;
        if (
               fname.startsWith(m_prefix)
            && fname.endsWith(m_postfix)
            && (fname.length() > (m_prefix.length() + m_postfix.length()))
        ) {
            String datePart = fname.substring(
                 m_prefix.length(), fname.length() - m_postfix.length()
            );

            //noinspection EmptyCatchBlock
            try {
                dt = m_dateFormat.parse(datePart);
            } catch (ParseException e) {}
        }
        return dt;
    }

    void cleanUpDir() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(m_dateProvider.getDate());
        calendar.add(Calendar.DAY_OF_MONTH, -m_days);
        calendar.clear(Calendar.HOUR);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        Date begDate = calendar.getTime();
        File[] fs = m_dir.listFiles();
        for (File f : fs) {
            if (f.isDirectory()) continue;
            String fname = f.getName();
            Date dt = fileNameToDate(fname);
            if ((dt == null) && fname.endsWith(c_ExceededFileSuffix)) {
                dt = fileNameToDate(
                    fname.substring(0, fname.length() - c_ExceededFileSuffix.length())
                );
            }
            if (dt == null) {
                if (m_deleteUnknownFiles) f.delete();
            } else {
                if (dt.before(begDate)) f.delete();
            }
        }
    }

    private String getFileName(Date date) {
        return m_prefix + m_dateFormat.format(date) + m_postfix;
    }

    File getFile(Date date) {
        if (date == null) date = m_dateProvider.getDate();
        return new File(m_dir, getFileName(date));
    }

    File getExceededFile(Date date) {
        if (date == null) date = m_dateProvider.getDate();
        return new File(m_dir, getFileName(date) + c_ExceededFileSuffix);
    }

    private static class FileSizeStreamDelegate extends OutputStream {
        private final OutputStream m_os;
        private long m_size;
        private File m_file;

        public FileSizeStreamDelegate(File f, boolean append) throws FileNotFoundException {
            m_os = new FileOutputStream(f, append);
            m_size = f.length();
            m_file = f;
        }

        public long getSize() {
            return m_size;
        }

        public File getFile() {
            return m_file;
        }

        public void write(int b) throws IOException {
            m_os.write(b);
            m_size++;
        }
        public void write(byte b[]) throws IOException {
            m_os.write(b);
            m_size += b.length;
        }
        public void write(byte b[], int off, int len) throws IOException {
            m_os.write(b, off, len);
            m_size += len;
        }
        public void flush() throws IOException {
            m_os.flush();
        }
        public void close() throws IOException {
            m_os.close();
        }
    }

    private void handleExceededSize(Date dt) {
        File ef = getExceededFile(dt);
        File f = m_stream.getFile();
        if (ef.exists()) {
            if (!ef.delete()) {
                error("Could not delete file '" + f.getAbsolutePath() +  "'", null);
                return;
            }
        }
        if (!f.renameTo(ef)) {
            error(
                "Could not rename file '" + f.getAbsolutePath() +
                "' to '" + ef.getAbsolutePath() + "'",
                null
            );
        }
    }

    private void rotate(final boolean append) {
        Date dt = null;
        try {
            dt = m_dateProvider.getDate();
        } catch (Exception e) {
            error(null, e);
        }

        if (dt == null) return;

        try {
            if (m_stream != null) {
                m_stream.close();
                if (maxSizeExceeded()) handleExceededSize(dt);
            }
        } catch (Exception e) {
            error(null, e);
        }

        try {
            m_stream = new FileSizeStreamDelegate(getFile(dt), append);
            m_handler.setOutputStream(m_stream);
        } catch (IOException e) {
            error(null, e);
        }
    }

    private boolean maxSizeExceeded() {
        return ((m_maxSize > 0) && (m_stream.getSize() > m_maxSize));
    }

    public synchronized void check() {
        int curDay = getCurDay();
        if ((curDay != m_curDay) || maxSizeExceeded()) {
            rotate(false);
            try { cleanUpDir(); }
            catch (Exception e) { error(null, e); }
            m_curDay = curDay;
        }
    }
}
