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

import ru.rd.courier.logging.CourierLogger;

import java.io.*;

/**
 * User: AStepochkin
 * Date: 21.03.2005
 * Time: 15:28:12
 */
public class StreamHelper {
    public static void transfer(
        InputStream r, OutputStream w, int bufSize, Condition isCancelled,
        boolean closeStreams, CourierLogger logger) throws IOException
    {
        try {
            byte[] buf = new byte[bufSize];
            int c;
            while ((c = r.read(buf)) != -1) {
                if (isCancelled != null && isCancelled.isTrue()) break;
                if (c == 0) {
                    c = r.read();
                    if (c == -1) break;
                    w.write(c);
                }
                else w.write(buf, 0, c);
            }
        } finally {
            if (closeStreams) {
                try { r.close(); }
                catch (Exception e) {
                    if (logger == null) e.printStackTrace();
                    else logger.warning(e);
                }
                try { w.close(); }
                catch (Exception e) {
                    if (logger == null) e.printStackTrace();
                    else logger.warning(e);
                }
            }
        }
    }

    public static void transfer(
        InputStream r, OutputStream w, int bufSize, Condition isCancelled,
        boolean closeStreams) throws IOException
    {
        transfer(r, w, bufSize, isCancelled, closeStreams, null);
    }

    public static void transfer(
        InputStream r, OutputStream w, int bufSize, boolean closeStreams) throws IOException
    {
        transfer(r, w, bufSize, null, closeStreams);
    }

    public static void transfer(
        InputStream r, OutputStream w, int bufSize) throws IOException
    {
        transfer(r, w, bufSize, false);
    }

    public static void transfer(
        Reader r, Writer w, int bufSize, Condition isCancelled,
        boolean closeStreams
    ) throws IOException {
        char[] buf = new char[bufSize];
        int c;
        while ((c = r.read(buf)) != -1) {
            if (c == 0) {
                throw new RuntimeException("c = 0");
            } else {
                w.write(buf, 0, c);
            }
            if (isCancelled != null && isCancelled.isTrue()) break;
        }
        if (closeStreams) {
            try { r.close(); }
            finally { w.close(); }
        }
    }

    public static void transfer(
        Reader r, Writer w, int bufSize, boolean closeStreams
    ) throws IOException {
        transfer(r, w, bufSize, null, closeStreams);
    }

    public static void transfer(Reader r, Writer w, int bufSize) throws IOException {
        transfer(r, w, bufSize, false);
    }

    public static String streamToWinString(InputStream is) throws IOException {
        return streamToString(is, "cp1251");
    }

    public static String streamToString(
        InputStream is, String charSet, int bufSize, boolean closeStreams
    ) throws IOException {
        StringWriter res = new StringWriter();
        StreamHelper.transfer(
            new InputStreamReader(is, charSet),
            res, bufSize, closeStreams
        );
        return res.toString();
    }

    public static String streamToString(
        InputStream is, String charSet, int bufSize
    ) throws IOException {
        return streamToString(is, charSet, bufSize, false);
    }

    public static String streamToString(
        InputStream is, String charSet, boolean closeStreams) throws IOException
    {
        return streamToString(is, charSet, 4*1024, closeStreams);
    }

    public static String streamToString(
        InputStream is, String charSet) throws IOException
    {
        return streamToString(is, charSet, false);
    }

    public static String readerToString(
        Reader r, int bufSize, boolean closeStreams
    ) throws IOException {
        StringWriter res = new StringWriter();
        StreamHelper.transfer(r, res, bufSize, closeStreams);
        return res.toString();
    }

    public static String readerToString(Reader r, boolean closeStreams) throws IOException {
        return readerToString(r, 4*1024, closeStreams);
    }

    public static boolean compare(
        InputStream r1, InputStream r2, boolean closeStreams) throws IOException
    {
        try {
            int c1, c2;
            while (true) {
                c1 = r1.read();
                c2 = r2.read();
                if (c1 != c2) return false;
                if (c1 < 0) return true;
            }
        } finally {
            if (closeStreams) {
                try { r1.close(); }
                finally { r2.close(); }
            }
        }
    }


    public static boolean compare(
        InputStream r1, InputStream r2) throws IOException
    {
        return compare(r1, r2, false);
    }

    public static byte[] streamToBytes(InputStream is, boolean closeStreams) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        transfer(is, bytes, 4*1024, closeStreams);
        return bytes.toByteArray();
    }

    public static byte[] streamToBytes(InputStream is) throws IOException {
        return streamToBytes(is, false);
    }
}