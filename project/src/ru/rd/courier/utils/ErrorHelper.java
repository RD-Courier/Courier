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

import java.io.File;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 13.07.2005
 * Time: 8:53:35
 */
public class ErrorHelper {

    public static Throwable getOriginalCause(Throwable e) {
        while (true) {
            if (e.getCause() == null) return e;
            e = e.getCause();
        }
    }

    public static String stackTraceToString(StackTraceElement[] st) {
        StringBuffer sb = new StringBuffer(32*st.length);
        for (int i = 0; i < st.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(
                "\tat " + st[i].getClassName() + "." + st[i].getMethodName()
                + "(" + st[i].getFileName() + ":" + st[i].getLineNumber() + ")"
            );
        }
        return sb.toString();
    }

    public static String stackTracesToString(ThreadGroup threadGroup, boolean ignoreDeamons) {
        StringBuffer sb = new StringBuffer();
        Thread[] threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        for (Thread th: threads) {
            if (th == null) continue;
            if (ignoreDeamons && th.isDaemon()) continue;
            if (sb.length() > 0) sb.append('\n');
            String group = th.getThreadGroup() == null ? "" : th.getThreadGroup().getName();
            sb.append(
                   "name='" + th.getName() + "'"
                + " id=" + th.getId()
                + " group=" + group
                + " state=" + th.getState()
                + " daemon=" + th.isDaemon()
            );
            sb.append('\n');
            sb.append(stackTraceToString(th.getStackTrace()));
        }
        return sb.toString();
    }

    public static String stackTracesToString(ThreadGroup threadGroup) {
        return stackTracesToString(threadGroup, false);
    }

    public static void showThreads(String fileName) throws IOException {
        String st = ErrorHelper.stackTracesToString(Thread.currentThread().getThreadGroup());
        System.out.println(st);
        if (fileName != null) {
            FileHelper.stringToFile(st, new File(fileName));
        }
    }

}
