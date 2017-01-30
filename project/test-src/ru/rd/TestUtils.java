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
package ru.rd;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.SimpleFormatter;
import ru.rd.courier.logging.CourierLoggerAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.FileHandler;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 12:25:32
 */
public class TestUtils {
    private static char[] s_randomStringChars;
    static {
        s_randomStringChars = TestUtils.addCharsToArray('a', 'z', new char[0]);
        s_randomStringChars = TestUtils.addCharsToArray('A', 'Z', TestUtils.s_randomStringChars);
        s_randomStringChars = TestUtils.addCharsToArray('0', '9', TestUtils.s_randomStringChars);
    }

    public static void checkObjects(Object obj1, Object obj2) throws IllegalAccessException {
        Class cls = obj1.getClass();
        if (!cls.isInstance(obj2)) {
            throw new RuntimeException(
                "Objects have different classes: " + cls.getName() + " : " + (obj2 == null ? "null" : obj2.getClass().getName()));
        }
        for (Field f: cls.getFields()) {
            if (Modifier.isPublic(f.getModifiers())) {
                Object val1 = f.get(obj1);
                Object val2 = f.get(obj2);
                if (!val1.equals(val2)) {
                    throw new RuntimeException(
                        "Field '" + f.getName() + "'" + val1 + " != " + val2);
                }
            }
        }
    }

    public static char[] addCharsToArray(char ch1, char ch2, char[] arr) {
        int ich1 = (int)ch1;
        int count = (int)ch2 - ich1 + 1;
        char[] arr2 = new char[arr.length + count];
        System.arraycopy(arr, 0, arr2, 0, arr.length);
        for (int i = 0; i < count; i++) {
            arr2[arr.length + i] = (char)(ich1 + i);
        }
        return arr2;
    }

    public static String randomString(int length) {
        StringBuffer buf = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            int p = (int)(Math.random() * s_randomStringChars.length);
            buf.append(s_randomStringChars[p]);
        }
        return buf.toString();
    }

    public static String randomLimitedString(int maxlength) {
        return randomString((int)(Math.random() * maxlength));
    }

    public static Object createMockObject(Class cls) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object obj = cls.getConstructor(new Class[0]).newInstance(new Object[0]);
        for (Field f: cls.getFields()) {
            if (Modifier.isPublic(f.getModifiers())) {
                if (f.getType().isAssignableFrom(int.class)) {
                    f.set(obj, (int) (Math.random() * Integer.MAX_VALUE));
                } else if (f.getType().isAssignableFrom(long.class)) {
                    f.set(obj, (long) (Math.random() * Long.MAX_VALUE));
                } else if (f.getType().isAssignableFrom(byte.class)) {
                    f.set(obj, (byte) (Math.random() * Byte.MAX_VALUE));
                } else if (f.getType().isAssignableFrom(float.class)) {
                    f.set(obj, (float) (Math.random() * Float.MAX_VALUE));
                } else if (f.getType().isAssignableFrom(double.class)) {
                    f.set(obj, Math.random() * Double.MAX_VALUE);
                } else if (f.getType().isAssignableFrom(String.class)) {
                    f.set(obj, randomLimitedString(256));
                }
            }
        }
        return obj;
    }

    public static CourierLogger getTestLogger(String filename) throws IOException {
        Logger rlog = Logger.getLogger("");
        rlog.setLevel(Level.ALL);
        for (Handler h: rlog.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleFormatter("{0,time,mm:ss.SSS}"));
        }
        if (filename != null) {
            Handler h = new FileHandler(filename, true);
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleFormatter("{0,time,mm:ss.SSS}"));
            rlog.addHandler(h);
        }
        return new CourierLoggerAdapter(rlog);
    }

    public static CourierLogger getTestLogger() throws IOException {
        return getTestLogger(null);
    }
}
