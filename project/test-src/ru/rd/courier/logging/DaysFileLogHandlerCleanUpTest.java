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

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * User: AStepochkin
 * Date: 28.09.2005
 * Time: 17:27:08
 */
public class DaysFileLogHandlerCleanUpTest extends DaysFileLogHandlerTest {
    private List<File> m_filesToDelete = new LinkedList<File>();
    private List<File> m_filesToPersist = new LinkedList<File>();

    private void createFile(File f, boolean toDelete) throws IOException {
        f.createNewFile();
        if (toDelete) m_filesToDelete.add(f);
        else m_filesToPersist.add(f);
    }

    private void testCleanUpDirWithStep(
        int days, boolean deleteUnknownFiles, int filesBefore, int step, int dateField
    ) throws IOException {
        createHandler(days, deleteUnknownFiles, -1);

        Calendar now = new GregorianCalendar();
        Calendar cal = new GregorianCalendar();
        for (int i = 0; i < days; i++) {
            createFile(m_handler.getCleaner().getFile(cal.getTime()), false);
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        for (int i = 0; i < filesBefore; i++) {
            Date dt = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, days);
            createFile(m_handler.getCleaner().getFile(dt), !cal.after(now));
            cal.setTime(dt);
            cal.add(dateField, -step);
        }
        createFile(new File(m_dir, "aaaa"), deleteUnknownFiles);

        m_handler.getCleaner().cleanUpDir();

        for (File f: m_filesToDelete) assertFalse(f.exists());
        for (File f: m_filesToPersist) assertTrue(f.exists());

        m_filesToDelete.clear();
        m_filesToPersist.clear();
    }

    public void testCleanUpDir() throws IOException {
        final int days = 4;
        final int daysBefore = 6;
        testCleanUpDirWithStep(days, false, daysBefore, 1, Calendar.DAY_OF_MONTH);
        testCleanUpDirWithStep(days, true, daysBefore, 1, Calendar.DAY_OF_MONTH);
        testCleanUpDirWithStep(days, false, daysBefore, 2, Calendar.DAY_OF_MONTH);
        testCleanUpDirWithStep(days, false, daysBefore, 2, Calendar.MONTH);
    }

}
