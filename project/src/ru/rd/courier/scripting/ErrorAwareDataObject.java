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
package ru.rd.courier.scripting;

import ru.rd.pool.PooledObject;

/**
 * User: AStepochkin
 * Date: 09.10.2006
 * Time: 12:32:32
 */
public class ErrorAwareDataObject implements PooledObject {
    protected Object m_object;
    protected int m_maxErrorCount;
    private boolean m_lastError = false;
    private int m_errorCount = 0;

    protected ErrorAwareDataObject() {
    }

    protected void errorOccured(Exception e) {
        m_lastError = true;
    }

    protected void errorAbsent() {
        m_lastError = false;
    }

    public void allocated() {
        m_lastError = false;
    }

    public boolean released() {
        if (m_maxErrorCount <= 0) return true;
        if (m_lastError) {
            m_errorCount++;
        } else {
            m_errorCount = 0;
        }
        return m_errorCount < m_maxErrorCount;
    }

    public Object getObject() {
        return m_object;
    }

    public void setObject(Object obj) {
        m_object = obj;
    }

    public void setMaxErrorCount(int maxErrorCount) {
        m_maxErrorCount = maxErrorCount;
    }

    public String toString() {
        return "Error aware wrapper: maxErrorCount = " + m_maxErrorCount + " of: " + m_object;
    }
}
