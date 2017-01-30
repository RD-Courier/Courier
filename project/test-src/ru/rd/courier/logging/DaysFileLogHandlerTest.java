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

import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.utils.FileHelper;

import java.util.*;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.io.File;

/**
 * User: AStepochkin
 * Date: 26.07.2005
 * Time: 13:56:12
 */
public class DaysFileLogHandlerTest extends FileDataTestCase {
    private static final String c_prefix = "prefix-";
    private static final String c_postfix = "-postfix";
    private static final String c_dateFormat = "yyyy-MM-dd";
    private static final String c_dir = "";

    protected File m_dir;
    protected DaysFileLogHandler m_handler;

    protected void courierSetUp() {
    }

    protected void courierTearDown() {
        if (m_handler != null) {
            m_handler.close();
        }
        m_handler = null;
    }

    protected final void createHandler(int days, boolean deleteUnknownFiles, int maxSize) {
        if (m_handler != null) m_handler.close();
        m_dir = getTempDir(c_dir);
        if (m_dir.list().length > 0) {
            FileHelper.deleteDir(m_dir);
            assertFalse(m_dir.exists());
            m_dir = getTempDir(c_dir);
        }
        m_handler = new DaysFileLogHandler(
            m_dir, c_dateFormat, days, c_prefix, c_postfix,
            deleteUnknownFiles, true, maxSize
        );
        m_handler.setLevel(Level.ALL);
    }

    public void testMaxSize() throws InterruptedException {
        final int chunkSize = 1024;
        final int maxSizeBytes = 5000;

        createHandler(2, true, 0);
        m_handler.getCleaner().setMaxSize(maxSizeBytes);

        class TestDayProvider implements DayProvider {
            private int m_day = 0;

            public int getDay() {
                return m_day;
            }

            public void incDay() {
                m_day++;
            }
        }

        TestDayProvider dp = new TestDayProvider();
        m_handler.getCleaner().setDayProvider(dp);

        class TestDateprovider implements DateProvider {
            private Date m_date;

            public TestDateprovider() {
                m_date = new Date();
            }

            public Date getDate() {
                return m_date;
            }

            public void setDate(Date date) {
                m_date = date;
            }

            public void incDate() {
                Calendar c = new GregorianCalendar();
                c.setTime(getDate());
                c.add(Calendar.DAY_OF_YEAR, 1);
                setDate(c.getTime());
            }
        }

        TestDateprovider dateProv = new TestDateprovider();
        m_handler.getCleaner().setDateProvider(dateProv);

        StringBuffer sb = new StringBuffer(chunkSize);
        for (int i = 0; i < chunkSize; i++) sb.append('*');
        String chunkString = sb.toString();
        LogRecord lr = new LogRecord(Level.SEVERE, chunkString);
        final int realChunkSize = m_handler.getFormatter().format(lr).length();
        int bytesWritten = 0;
        while (bytesWritten < maxSizeBytes) {
            m_handler.publish(new LogRecord(Level.SEVERE, chunkString));
            bytesWritten += realChunkSize;
        }
        m_handler.flush();


        File logFile1 = m_handler.getCleaner().getFile(null);
        File exceededFile1 = m_handler.getCleaner().getExceededFile(null);

        assertTrue(logFile1.exists());
        assertFalse(exceededFile1.exists());

        m_handler.publish(lr);

        assertTrue(logFile1.exists());
        logFile1 = m_handler.getCleaner().getFile(null);
        //assertEquals(realChunkSize, logFile1.length());
        assertTrue(exceededFile1.exists());
        assertEquals(bytesWritten, exceededFile1.length());

        dp.incDay();
        dateProv.incDate();
        m_handler.publish(new LogRecord(Level.SEVERE, chunkString));

        File logFile2 = m_handler.getCleaner().getFile(null);
        File exceededFile2 = m_handler.getCleaner().getExceededFile(null);

        assertTrue(logFile1.exists());
        assertEquals(realChunkSize, logFile1.length());
        assertTrue(exceededFile1.exists());

        assertTrue(logFile2.exists());
        //assertEquals(realChunkSize, logFile2.length());
        assertFalse(exceededFile2.exists());

        dp.incDay();
        dateProv.incDate();
        m_handler.publish(new LogRecord(Level.SEVERE, chunkString));

        File logFile3 = m_handler.getCleaner().getFile(null);
        File exceededFile3 = m_handler.getCleaner().getExceededFile(null);

        assertFalse(logFile1.exists());
        assertFalse(exceededFile1.exists());

        assertTrue(logFile2.exists());
        assertEquals(realChunkSize, logFile2.length());
        assertFalse(exceededFile2.exists());

        assertTrue(logFile3.exists());
        assertFalse(exceededFile3.exists());

        m_handler.close();
        assertEquals(realChunkSize, logFile3.length());
    }
}
