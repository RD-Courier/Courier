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

import ru.rd.net.*;
import ru.rd.net.synch.SynchStatedDecoder;

import java.nio.charset.Charset;

/**
 * User: STEPOCHKIN
 * Date: 30.07.2008
 * Time: 11:11:52
 */
public class ProcessResultDecoder extends StatedDecoder<ProcessResult> {
    public ProcessResultDecoder(Charset charset) throws NoSuchMethodException {
        super(getSteps(charset), new ReflectFactory<ProcessResult>(ProcessResult.class));
    }

    public static SynchStatedDecoder<ProcessResult> createSynchDecoder(Charset charset) throws NoSuchMethodException {
        return new SynchStatedDecoder<ProcessResult>(
            getSteps(charset),
            new ReflectFactory<ProcessResult>(ProcessResult.class)
        );
    }

    public static DecodeStep[] getSteps(Charset charset) {
        return new DecodeStep[] {
            new LongDecodeStep() {
                protected void setDecodedProperty(Object message, long value) {
                    ((ProcessResult)message).setId(value);
                }
            },
            new IntDecodeStep() {
                protected void setDecodedProperty(Object message, int value) {
                    ((ProcessResult)message).setRecordCount(value);
                }
            },
            new IntDecodeStep() {
                protected void setDecodedProperty(Object message, int value) {
                    ((ProcessResult)message).setErrorCount(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setError(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setErrorStack(value);
                }
            },
            new LongDecodeStep() {
                protected void setDecodedProperty(Object message, long value) {
                    ((ProcessResult)message).setStartTime(value);
                }
            },
            new LongDecodeStep() {
                protected void setDecodedProperty(Object message, long value) {
                    ((ProcessResult)message).setTotalTime(value);
                }
            },
            new LongDecodeStep() {
                protected void setDecodedProperty(Object message, long value) {
                    ((ProcessResult)message).setSourceTime(value);
                }
            },
            new LongDecodeStep() {
                protected void setDecodedProperty(Object message, long value) {
                    ((ProcessResult)message).setTargetTime(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setPipe(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setSourceDbName(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setSourceDbType(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setSourceDbUrl(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setTargetDbName(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setTargetDbType(value);
                }
            },
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((ProcessResult)message).setTargetDbUrl(value);
                }
            }
        };
    }
}
