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
package ru.rd.courier.stat;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import ru.rd.net.message.CheckMessage;
import ru.rd.net.message.CommonAnswer;
import ru.rd.courier.manager.message.ProcessResult;

/**
 * User: AStepochkin
 * Date: 31.07.2008
 * Time: 13:55:26
 */
public class ServerTestHandler extends IoHandlerAdapter {
    private transient int m_counter = 0;
    private final boolean m_logEnabled;

    public ServerTestHandler(boolean logEnabled) {
        m_logEnabled = logEnabled;
    }

    public ServerTestHandler() {
        this(true);
    }

    private void log(String message) {
        if (m_logEnabled) {
            System.out.println(message);
        }
    }

    public void messageReceived(IoSession session, Object message) throws Exception {
        if (message instanceof CheckMessage) {
            log("messageReceived: CheckMessage = " + ((CheckMessage)message).getId());
            session.write(message);
            return;
        }
        ProcessResult pr = (ProcessResult) message;
        System.out.println("messageReceived: " + session.getRemoteAddress() + " ProcessId = " + pr.getId());
        String error = "TestError" + m_counter++;
        /*
        try {

            log(
                "Pipe = " + pr.getPipe()
                + " RC = " + pr.getRecordCount()
                + " EC = " + pr.getErrorCount()
            );

        } catch (Exception e) {
            error = e.getMessage();
        }
        */
        session.write(new CommonAnswer(error));
        //if (m_counter % 5 == 0) session.close();
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        log(
            "messageSent: " + session.getRemoteAddress()
            + " MessageClass = " + message.getClass().getName()
        );
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        log("exceptionCaught: " + cause);
        cause.printStackTrace(System.err);
        session.close();
    }

    public void sessionOpened(IoSession session) throws Exception {
        log("sessionOpened: " + session.getRemoteAddress());
    }

    public void sessionClosed(IoSession session) throws Exception {
        log("sessionClosed: " + session.getRemoteAddress());
    }
}
