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
package ru.rd.courier.utils;

import java.io.*;

/**
 * User: AStepochkin
 * Date: 07.07.2005
 * Time: 17:37:33
 */
public class FileHelper {
    public static boolean deleteDir(File dir) {
        for (File f: dir.listFiles()) {
            if (f.isDirectory()) {
                deleteDir(f);
            } else {
                f.delete();
            }
        }
        return dir.delete();
    }

    public static String fileToString(File f, String charSet) throws IOException {
        return StreamHelper.streamToString(new FileInputStream(f), charSet, true);
    }

    public static void stringToFile(
        String text, File f, String charSet
    ) throws IOException {
        StreamHelper.transfer(
            new StringReader(text),
            new OutputStreamWriter(new FileOutputStream(f), charSet),
            4096, true
        );
    }

    public static void stringToFile(String text, File f) throws IOException {
        StreamHelper.transfer(
            new StringReader(text),
            new OutputStreamWriter(new FileOutputStream(f)),
            4096, true
        );
    }
    
    public static void bytesToFile(byte[] bytes, File f) throws IOException {
        StreamHelper.transfer(
            new ByteArrayInputStream(bytes), new FileOutputStream(f), 4096, true
        );
    }

    public static void copyFile(File from, File to) throws IOException {
        StreamHelper.transfer(
            new FileInputStream(from), new FileOutputStream(to),
            4096, true
        );
    }

    public boolean compare(File f1, File f2) throws IOException {
        if (f1.length() != f2.length()) return false;
        return StreamHelper.compare(
            new BufferedInputStream(new FileInputStream(f1)),
            new BufferedInputStream(new FileInputStream(f2)),
            true
        );
    }

    public static byte[] fileToBytes(File f) throws IOException {
        return StreamHelper.streamToBytes(new FileInputStream(f), true);
    }

    public static void safeMkdirs(File f) {
        if (f.exists()) return;
        if (f.mkdirs()) return;
        if (f.exists()) return;
        throw new RuntimeException("Failed to create dir '" + f.getAbsolutePath() + "'");
    }
}
