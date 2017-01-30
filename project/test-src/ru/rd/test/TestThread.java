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
package ru.rd.test;

import java.util.concurrent.FutureTask;

/**
 * User: AStepochkin
 * Date: 18.05.2005
 * Time: 15:08:54
 */
public class TestThread extends Thread {
    private String m_desc;
    private boolean m_loggable;
    private String m_error;

    public TestThread(String desc, boolean loggable) {
        m_error = null;
        init(desc, loggable);
    }

    protected TestThread() {
        this("", true);
    }

    protected void init(String desc, boolean loggable) {
        m_desc = desc;
        m_loggable = loggable;
    }

    protected void testRun() throws Exception {
    }

    protected void log(String msg) {
        if (m_loggable) System.out.println(m_desc + " --> " + msg);
    }

    public boolean hasError() {
        return m_error != null;
    }

    public String getError() {
        return m_error;
    }

    public void run() {
        try {
            testRun();
        } catch (Exception e) {
            m_error = e.getMessage();
            e.printStackTrace(System.err);
        }
    }
}
