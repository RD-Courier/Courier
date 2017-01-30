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

import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.*;

import java.net.SocketAddress;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 9:22:30
 */
public class MockSession extends BaseIoSession {
    protected void updateTrafficMask() {

    }

    public IoService getService() {
        return null;
    }

    public IoServiceConfig getServiceConfig() {
        return null;
    }

    public IoHandler getHandler() {
        return null;
    }

    public IoSessionConfig getConfig() {
        return null;
    }

    public IoFilterChain getFilterChain() {
        return null;
    }

    public TransportType getTransportType() {
        return null;
    }

    public SocketAddress getRemoteAddress() {
        return null;
    }

    public SocketAddress getLocalAddress() {
        return null;
    }

    public SocketAddress getServiceAddress() {
        return null;
    }
}
