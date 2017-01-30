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
package ru.rd.net.message;

import ru.rd.net.DecodeStep;
import ru.rd.net.DelimitedStringDecodeStep;
import ru.rd.net.StatedDecoder;
import ru.rd.utils.ObjectFactory;
import ru.rd.net.message.CommonAnswer;

import java.nio.charset.Charset;

/**
 * User: STEPOCHKIN
 * Date: 30.07.2008
 * Time: 21:03:00
 */
public class CommonAnswerDecoder extends StatedDecoder<CommonAnswer> {
    public static DecodeStep[] getSteps(Charset charset) {
        return new DecodeStep[] {
            new DelimitedStringDecodeStep(charset) {
                protected void setDecodedString(Object message, String value) {
                    ((CommonAnswer)message).error = value;
                }
            }
        };
    }

    public CommonAnswerDecoder(Charset charset) {
        super(getSteps(charset), new ObjectFactory<CommonAnswer>() {
            public CommonAnswer create() throws Exception {
                return new CommonAnswer("");
            }
        });
    }
}
