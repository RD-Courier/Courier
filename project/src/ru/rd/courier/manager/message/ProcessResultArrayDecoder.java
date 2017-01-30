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

import ru.rd.net.StatedDecoder;
import ru.rd.net.DecodeStep;
import ru.rd.net.ArrayDecodeStep;
import ru.rd.net.ReflectFactory;
import ru.rd.net.synch.SynchStatedDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 24.04.2009
 * Time: 15:45:00
 */
public class ProcessResultArrayDecoder extends StatedDecoder<ProcessResultArray> {
    public ProcessResultArrayDecoder(Charset charset) throws NoSuchMethodException {
        super(getSteps(charset), new ReflectFactory<ProcessResultArray>(ProcessResultArray.class));
    }

    public static SynchStatedDecoder<ProcessResultArray> createSynchDecoder(Charset charset) throws NoSuchMethodException {
        return new SynchStatedDecoder<ProcessResultArray>(
            getSteps(charset),
            new ReflectFactory<ProcessResultArray>(ProcessResultArray.class)
        );
    }

    public static DecodeStep[] getSteps(Charset charset) throws NoSuchMethodException {
        return new DecodeStep[] {
            new ArrayDecodeStep<ProcessResult>(new ProcessResultDecoder(charset)) {
                protected void setDecodedProperty(Object message, List<ProcessResult> value) {
                    ((ProcessResultArray)message).setResults(value);
                }
            }
        };
    }
}
