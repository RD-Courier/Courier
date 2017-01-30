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
package ru.rd.courier;

import ru.rd.courier.scripting.ScriptStatement;

import java.util.Date;

/**
 * User: AStepochkin
 * Date: 27.10.2005
 * Time: 14:00:26
 */
public class TransferRequest {
    private final String m_pipeName;
    private final String m_ruleName;
    private final Integer m_ignoreErrorCount;
    private final ScriptStatement m_launchStmt;
    private final TransferProcessSupervisor m_supervisor;
    private final Pipeline.ProcessFactory m_factory;
    private final Date m_createTime = new Date();
    private TransferResultListener m_resultHandler;
    private int m_relaunchCount = 0;

    public TransferRequest(
        String pipeName, String ruleName,
        Integer ignoreErrorCount,
        ScriptStatement launchStmt, TransferProcessSupervisor supervisor,
        Pipeline.ProcessFactory factory
    ) {
        m_pipeName = pipeName;
        m_ruleName = ruleName;
        m_ignoreErrorCount = ignoreErrorCount;
        m_launchStmt = launchStmt;
        m_supervisor = supervisor;
        m_factory = factory;
        m_resultHandler = null;
    }

    public int getRelaunchCount() {
        return m_relaunchCount;
    }

    public Date getCreateTime() {
        return m_createTime;
    }

    public void incRelaunchCount() {
        m_relaunchCount++;
    }

    public final Integer getIgnoreErrorCount() {
        return m_ignoreErrorCount;
    }

    public final ScriptStatement getLaunchStmt() {
        return m_launchStmt;
    }

    public final TransferProcessSupervisor getSupervisor() {
        return m_supervisor;
    }

    public final String getRuleName() {
        return m_ruleName;
    }

    public final String getPipeName() {
        return m_pipeName;
    }

    public final Pipeline.ProcessFactory getFactory() {
        return m_factory;
    }

    public final TransferResultListener getResultHandler() {
        return m_resultHandler;
    }

    public final void setResultHandler(TransferResultListener handler) {
        m_resultHandler = handler;
    }
}
