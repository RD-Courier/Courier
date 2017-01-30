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
package ru.rd.scheduling.leveled.launchers;

import ru.rd.scheduling.leveled.StartStopListener;

import java.util.Date;
import java.util.logging.Logger;

/**
 * User: AStepochkin
 * Date: 25.03.2005
 * Time: 10:19:16
 */
public abstract class OneTimeLauncher implements StartStopListener {
    protected Logger m_log = null;
    protected final Runnable m_work;

    public OneTimeLauncher(
        Logger log, Runnable work
    ) {
        m_log = log;
        m_work = work;
    }

    protected abstract void protectedStart(Date parentStart);

    public void start(Date parentStart) {
        try {
            protectedStart(parentStart);
        } catch(Exception e) {
            throw new RuntimeException("Error in launcher :" + getDesc(), e);
        }
    }

    public void stop() {
    }

    private String getDesc() {
        return m_work.toString() + " " + getClass().getName();
    }
}
