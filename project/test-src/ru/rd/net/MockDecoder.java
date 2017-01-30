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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 11:22:41
 */
public class MockDecoder<Message> extends StatedDecoder<Message> {
    private static DecodeStep[] getSteps(Class cls, Charset charset) {
        LinkedList<DecodeStep> steps = new LinkedList<DecodeStep>();
        for (final Field f: cls.getFields()) {
            DecodeStep step;
            if (Modifier.isPublic(f.getModifiers())) {
                if (f.getType().isAssignableFrom(int.class)) {
                    step = new IntDecodeStep() {
                        protected void setDecodedProperty(Object message, int value) {
                            try {
                                f.set(message, value);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                } else if (f.getType().isAssignableFrom(long.class)) {
                    step = new LongDecodeStep() {
                        protected void setDecodedProperty(Object message, long value) {
                            try {
                                f.setLong(message, value);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                } else if (f.getType().isAssignableFrom(String.class)) {
                    step = new DelimitedStringDecodeStep(charset) {
                        protected void setDecodedString(Object message, String value) {
                            try {
                                f.set(message, value);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                } else {
                    throw new RuntimeException("Unsupported decode type '" + f.getType().getName() + "'");
                }
                steps.add(step);
            }
        }
        return steps.toArray(new DecodeStep[steps.size()]);
    }

    public MockDecoder(Class<Message> cls, Charset charset) throws NoSuchMethodException {
        super(getSteps(cls, charset), new ReflectFactory<Message>(cls, new Object[0]));
    }
}
