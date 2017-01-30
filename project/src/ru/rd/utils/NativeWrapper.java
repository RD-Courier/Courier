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
package ru.rd.utils;

/**
 * User: AStepochkin
 * Date: 26.12.2005
 * Time: 18:00:32
 */
public class NativeWrapper {
    protected int m_rawObject;

    static {
        System.loadLibrary("TdJavaSupport");
        //initNativeLibrary(
        //    System.getProperty("java.wrappers.log.path", null)
        //  , System.getProperty("java.wrappers.log.debug", "").equals("true")
        //);
    }

    public static native void initNativeLibrary(
        String logPath,
        String prefix, String suffix, String dateDelimiter,
        int storeDays, String logLevelName,
        boolean checkMemory
    );
    public static native void initExceptionLogger(
        String logPath,
        String prefix, String suffix, String dateDelimiter,
        int storeDays
    );
    public static native String getExecPoolState();
    public static native void coInitialize();
    public static native void coUninitialize();
    public static native String getMemoryStatus();
    public static native String getProcessMemoryInfo();

    protected final native void destroyRawObject();

    public final void dispose() {
        //System.out.println("NativeWrapper.dispose: m_rawObject = " + m_rawObject);
        if (m_rawObject > 0) {
            destroyRawObject();
            m_rawObject = 0;
        }
    }

    public void finalize() throws Throwable {
        dispose();
        super.finalize();
    }
}
