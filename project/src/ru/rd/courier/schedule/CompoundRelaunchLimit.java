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
package ru.rd.courier.schedule;

import ru.rd.courier.schedule.RelaunchLimit;
import ru.rd.courier.LocalSystemDb;

import java.util.Collection;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 07.06.2008
 * Time: 10:33:49
 */
public class CompoundRelaunchLimit implements RelaunchLimit {
    private final Collection<RelaunchLimit> m_limits;

    public CompoundRelaunchLimit() {
        m_limits = new LinkedList<RelaunchLimit>();
    }

    public CompoundRelaunchLimit(Collection<RelaunchLimit> limits) {
        m_limits = limits;
    }

    public void addLimit(RelaunchLimit limit) {
        m_limits.add(limit);
    }

    public boolean canBeRelaunched(LocalSystemDb.ProcessInfo pinfo) {
        for (RelaunchLimit m_limit : m_limits) {
            if (!m_limit.canBeRelaunched(pinfo)) return false;
        }
        return true;
    }

    public String getRejectReason(LocalSystemDb.ProcessInfo pinfo) {
        for (RelaunchLimit limit : m_limits) {
            if (!limit.canBeRelaunched(pinfo)) return limit.getRejectReason(pinfo);
        }
        return null;
    }

    public void finish() {
        for (RelaunchLimit m_limit : m_limits) {
            m_limit.finish();
        }
    }
}
