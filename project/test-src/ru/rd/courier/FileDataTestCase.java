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
package ru.rd.courier;

import junit.framework.TestCase;

import java.io.*;
import java.sql.SQLException;

import ru.rd.courier.utils.FileHelper;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StreamHelper;
import ru.rd.courier.utils.templates.SimplePreparedTemplate;
import ru.rd.courier.utils.templates.HashMapStringContext;
import org.w3c.dom.Element;

/**
 * User: AStepochkin
 * Date: 01.07.2005
 * Time: 14:22:35
 */
public class FileDataTestCase extends TestCase {
    private boolean m_deleteTempDir;
    private static File m_dataDir = null;
    private static File m_tempDir = null;

    protected static File getRootDataDirEx() {
        if (m_dataDir == null) {
            final String cTestDataDir = "TestDataDir";
            String prop = System.getProperty(cTestDataDir, null);
            assertNotNull("System property " + cTestDataDir + " not found", prop);
            m_dataDir = new File(prop);
        }
        return m_dataDir;
    }

    protected static File getRootDataDir() {
        File f = getRootDataDirEx();
        assertTrue(f.exists());
        return f;
    }

    private static File innerGetTempDir() {
        return new File(getRootDataDirEx(), "test-temp-dir");
    }

    private static File getTempDir() {
        if (m_tempDir == null) {
            m_tempDir = innerGetTempDir();
            if (!m_tempDir.exists()) {
                assertTrue(
                    "Could not create temp dir " + m_tempDir.getAbsolutePath(),
                    m_tempDir.mkdirs()
                );
            }
        }
        return m_tempDir;
    }

    private final File getDataFile(String name, boolean mustExist) {
        final String retName = (
            getClass().getPackage().getName().replace('.', File.separatorChar) +
            File.separatorChar + name
        );
        final File ret = new File(getRootDataDir(), retName);
        if (mustExist) assertTrue(
            "File " + ret.getAbsolutePath() + " must exist", ret.exists());
        return ret;
    }

    protected final File getDataFile(String name) {
        return getDataFile(name, true);
    }

    protected final Element getDataXml(String name) {
        try {
            return DomHelper.parseXmlFile(getDataFile(name)).getDocumentElement();
        } catch (Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    private final File getTempFile(String name, boolean mustExist) {
        final File ret = new File(getTempDir(), name);
        if (mustExist) assertTrue(
            "File " + ret.getAbsolutePath() + " must exist", ret.exists());
        return ret;
    }

    protected final File getTempFile(String name) {
        return getTempFile(name, false);
    }

    protected final File getTempDir(String name) {
        final File ret = new File(getTempDir(), name);
        ret.mkdirs();
        if (!ret.exists()) {
            assertTrue(
                "Failed to create catalog " + ret.getAbsolutePath(),
                ret.mkdirs()
            );
        }
        return ret;
    }

    protected final String getFileText(String name, String charSet)
    throws IOException {
        return FileHelper.fileToString(getDataFile(name), charSet);
    }

    protected final void setFileText(String name, String text, String charSet)
    throws IOException {
        FileHelper.stringToFile(text, getTempFile(name), charSet);
    }

    protected final String applyTemplate(File file, String[][] ctx)
    throws CourierException, IOException {
        SimplePreparedTemplate tmpl = new SimplePreparedTemplate(
            StreamHelper.streamToWinString(new FileInputStream(file))
        );
        return tmpl.calculate(new HashMapStringContext(ctx));
    }

    protected final String applyTemplate(String file, String[][] ctx)
    throws CourierException, IOException {
        return applyTemplate(getDataFile(file), ctx);
    }

    protected final void setUp() throws Exception {
        m_deleteTempDir = true;
        courierSetUp();
    }

    protected final void tearDown() throws Exception {
        courierTearDown();
        if (m_deleteTempDir && innerGetTempDir().exists()) {
            assertTrue(
                "Temp dir deletion failed",
                FileHelper.deleteDir(innerGetTempDir())
            );
        }
        m_tempDir = null;
    }

    protected final void setDeleteTempDir(boolean value) {
        m_deleteTempDir = value;
    }

    protected void courierSetUp() throws Exception {}
    protected void courierTearDown() throws Exception {}

    //****************** UTILS ************************************
    protected void checkArrays(String[] exp, String[] act) {
        assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            assertEquals(exp[i], act[i]);
        }
    }

    protected void intArrayToBool(int[] arr, boolean[] toArr) {
        for (int i = 0; i < arr.length; i++) toArr[i] = arr[i] > 0;
    }

    protected interface CombinationHandler {
        void execute(int[] arr) throws Exception;
    }

    protected void iterateArray(int dim, int limit, CombinationHandler h) {
        int[] arr = new int[dim];
        for (int i = 0; i < dim; i++) arr[i] = 0;
        while (true) {
            try {
                h.execute(arr);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (int i = 0; i < dim; i++) {
                arr[i]++;
                if (arr[i] <= limit) break;
                else {
                    if (i == dim - 1) return;
                    arr[i] = 0;
                }
            }
        }
    }
    //*************************************************************


    protected void checkArraysAsStrings(Object[] arr1, Object[] arr2) {
        assertTrue(arr1.length == arr2.length);
        for (int i = 0; i < arr1.length; i++) {
            assertEquals(arr1[i].toString(), arr2[i].toString());
        }
    }
}
