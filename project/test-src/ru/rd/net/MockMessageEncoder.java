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

import org.apache.mina.common.ByteBuffer;
import ru.rd.net.synch.ExpandedStringedSynchEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 10:50:15
 */
public class MockMessageEncoder extends ExpandedStringedSynchEncoder<Object> {
    public MockMessageEncoder(Charset charset) {
        super(charset);
    }

    public void encodeToBuffer(Object message, ByteBuffer buffer) throws Exception {
        for (Field f: message.getClass().getFields()) {
            if (Modifier.isPublic(f.getModifiers())) {
                if (f.getType().isAssignableFrom(int.class)) {
                    buffer.putInt(f.getInt(message));
                } else if (f.getType().isAssignableFrom(long.class)) {
                    buffer.putLong(f.getLong(message));
                } else if (f.getType().isAssignableFrom(byte.class)) {
                    buffer.put(f.getByte(message));
                } else if (f.getType().isAssignableFrom(float.class)) {
                    buffer.putFloat(f.getFloat(message));
                } else if (f.getType().isAssignableFrom(double.class)) {
                    buffer.putDouble(f.getDouble(message));
                } else if (f.getType().isAssignableFrom(String.class)) {
                    putString(buffer, (String)f.get(message));
                }
            }
        }
    }
}
