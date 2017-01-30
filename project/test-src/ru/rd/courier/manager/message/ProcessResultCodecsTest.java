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

import junit.framework.TestCase;
import org.apache.mina.common.ByteBuffer;

import java.nio.charset.Charset;

/**
 * User: AStepochkin
 * Date: 13.10.2008
 * Time: 17:30:03
 */
public class ProcessResultCodecsTest extends TestCase {
    public void test() throws Exception {
        Charset charset = Charset.forName("cp1251");
        ProcessResultEncoder encoder = new ProcessResultEncoder(charset);
        ProcessResultDecoder decoder = new ProcessResultDecoder(charset);
        ProcessResult pr = createProcessResult();
        ByteBuffer buffer = ByteBuffer.allocate(0);
        buffer.setAutoExpand(true);
        encoder.encodeToBuffer(pr, buffer);
        buffer.flip();
        Object pr2 = decoder.decode(null, buffer);
        assertEquals(pr, pr2);
    }

    private static long s_processId = 1;
    public static ProcessResult createProcessResult() {
        ProcessResult pr = new ProcessResult();
        pr.setId(s_processId++);
        pr.setPipe("TestPipe");
        pr.setSourceDbName("TestSource");
        pr.setTargetDbName("TestTarget");
        pr.setErrorCount(1);
        pr.setError("TestError");
        pr.setSourceTime(4444);
        pr.setTargetTime(8888);
        return pr;
    }
}
