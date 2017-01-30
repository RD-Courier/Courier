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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * User: AStepochkin
 * Date: 01.02.2008
 * Time: 11:14:16
 */
public class MethodInvoker<T> implements Invokee {
    private final T m_object;
    private final Method m_method;

    public MethodInvoker(T object, Method method) {
        m_object = object;
        m_method = method;
    }

    public MethodInvoker(T object, Class cl, String methodName) throws NoSuchMethodException {
        this(object, findMethod(cl, methodName));
    }

    public void invoke() throws IllegalAccessException, InvocationTargetException {
        m_method.invoke(m_object);
    }

    private static Method findMethod(Class cl, String methodName) throws NoSuchMethodException {
        return cl.getMethod(methodName);
    }

    public static Invokee tryCreate(Object object, Class cl, String methodName) {
        Method method;
        try {
            method = findMethod(cl, methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
        return new MethodInvoker<Object>(object, method);
    }
}
