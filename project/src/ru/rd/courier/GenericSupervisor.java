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

import ru.rd.courier.schedule.RelaunchLimit;

/**
 * User: AStepochkin
 * Date: 07.06.2008
 * Time: 10:35:54
 */
public class GenericSupervisor implements TransferProcessSupervisor {
    private final long m_timeout;
    private final RelaunchChecker m_checker;
    private RelaunchLimit m_limit;

    public GenericSupervisor(
        long timeout, RelaunchChecker checker, RelaunchLimit limit
    ) {
        m_timeout = timeout;
        m_checker = checker;
        m_limit = limit;
    }

    public boolean canBeRelaunched(LocalSystemDb.ProcessInfo pinfo) {
        if (m_limit == null) return true;
        return m_limit.canBeRelaunched(pinfo);
    }

    public String getRejectReason(LocalSystemDb.ProcessInfo pinfo) {
        if (m_limit == null) return null;
        return m_limit.getRejectReason(pinfo);
    }

    public long getRelaunchTimeout(TransferProcessResult result) {
        if (m_checker.needToRelaunch(result)) return m_timeout;
        return 0;
    }

    public void finish() {
        m_limit.finish();
    }

    public void setLimit(RelaunchLimit limit) {
        m_limit = limit;
    }
}
