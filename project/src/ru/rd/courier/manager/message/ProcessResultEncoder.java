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
package ru.rd.courier.manager.message;

import org.apache.mina.common.ByteBuffer;
import ru.rd.net.synch.ExpandedStringedSynchEncoder;

import java.nio.charset.Charset;

/**
 * User: STEPOCHKIN
 * Date: 30.07.2008
 * Time: 20:38:26
 */
public class ProcessResultEncoder extends ExpandedStringedSynchEncoder<ProcessResult> {
    public ProcessResultEncoder(Charset charset) {
        super(charset);
    }

    protected int getBufferSize(ProcessResult message) {
        return 2 * 4 + 2 * 8 + message.getPipe().length() + message.getSourceDbName().length();
    }

    public void encodeToBuffer(ProcessResult message, ByteBuffer buffer) throws Exception {
        buffer.putLong(message.getId());
        buffer.putInt(message.getRecordCount());
        buffer.putInt(message.getErrorCount());
        putString(buffer, message.getError());
        putString(buffer, message.getErrorStack());
        buffer.putLong(message.getStartTime());
        buffer.putLong(message.getTotalTime());
        buffer.putLong(message.getSourceTime());
        buffer.putLong(message.getTargetTime());
        putString(buffer, message.getPipe());
        putString(buffer, message.getSourceDbName());
        putString(buffer, message.getSourceDbType());
        putString(buffer, message.getSourceDbUrl());
        putString(buffer, message.getTargetDbName());
        putString(buffer, message.getTargetDbType());
        putString(buffer, message.getTargetDbUrl());
    }
}
