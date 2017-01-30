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
package ru.rd.net;

import ru.rd.utils.ObjectFactory;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;

/**
 * User: AStepochkin
 * Date: 24.09.2008
 * Time: 16:20:39
 */
public class ReflectFactory<CreateClass> implements ObjectFactory<CreateClass> {
    private final Constructor<? extends CreateClass> m_constructor;
    private final Object[] m_params;

    private static final Class[] cEmptySig = new Class[0];
    private static final Object[] cEmptyParams = new Object[0];

    public ReflectFactory(Class<? extends CreateClass> cls, Class[] sig, Object[] params) throws NoSuchMethodException {
        m_constructor = cls.getConstructor(sig);
        m_params = params;
    }

    private static Class[] formSig(Object[] params) {
        Class[] sig = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            sig[i] = params[i].getClass();
        }
        return sig;
    }

    public ReflectFactory(Class<? extends CreateClass> cls, Object[] params) throws NoSuchMethodException {
        this(cls, formSig(params), params);
    }

    public ReflectFactory(Class<? extends CreateClass> cls) throws NoSuchMethodException {
        this(cls, cEmptySig, cEmptyParams);
    }

    public CreateClass create() throws Exception {
        return m_constructor.newInstance(m_params);
    }

    public static <T> ObjectFactory<? extends T> factoryOnCharset(Class<? extends T> cls, Charset charset) throws NoSuchMethodException {
        return new ReflectFactory<T>(cls, new Class[] {Charset.class}, new Object[] {charset});
    }
}
