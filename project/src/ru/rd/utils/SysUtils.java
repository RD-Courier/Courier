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

import ru.rd.courier.logging.CourierLogger;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;

/**
 * User: AStepochkin
 * Date: 25.09.2008
 * Time: 16:48:10
 */
public class SysUtils {
    public static boolean objEquals(Object str1, Object str2) {
        if (str1 == null) return str2 == null;
        return str1.equals(str2);
    }

    public static void dispose(Object obj, CourierLogger logger) {
        if (!(obj instanceof Disposable)) return;
        try { ((Disposable)obj).dispose(); } catch(Throwable e) { logger.warning(e); }
    }

    public static void dispose(Object obj, Logger logger) {
        if (!(obj instanceof Disposable)) return;
        try {
            ((Disposable)obj).dispose();
        } catch (Throwable e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    public static boolean dispose(Object obj, CourierLogger logger, long timeout) {
        if (!(obj instanceof TimedDisposable)) return false;
        try {
            return ((TimedDisposable)obj).dispose(timeout);
        } catch(Throwable e) {
            logger.warning(e);
            return false;
        }
    }

    public static <T> Set<T> arrayToSet(T[] arr) {
        Set<T> ret = new HashSet<T>(arr.length);
        for (T s: arr) ret.add(s);
        return ret;
    }
}
