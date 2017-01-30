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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.net.synch.SynchProtocolCodecFactory;

/**
 * User: Astepochkin
 * Date: 03.10.2008
 * Time: 18:03:18
 */
public class ConstSynchClientFactory<InputMessage, OutputMessage>
    extends SynchClientFactory<InputMessage, OutputMessage>
{
    private final String m_host;
    private final int m_port;

    public ConstSynchClientFactory(
        CourierLogger logger, String host, int port,
        SynchProtocolCodecFactory<InputMessage, OutputMessage> codecFactory,
        SocketProcessing.CheckFactory<InputMessage> checkFactory
    ) {
        super(logger, codecFactory, checkFactory);
        m_host = host;
        m_port = port;
    }

    protected String getHost() {
        return m_host;
    }

    protected int getPort() {
        return m_port;
    }
}
