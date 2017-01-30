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
package ru.rd.pool;

public class ReleaseHelper implements PooledObjectHolder {
    private boolean m_stale = false;
    private final ObjectPoolIntf m_pool;
    private Object m_obj;

    public ReleaseHelper(ObjectPoolIntf pool, Object o) {
        m_pool = pool;
        m_obj = o;
    }
    
    public ReleaseHelper(ObjectPoolIntf pool) {
        this(pool, null);
    }

    public PoolObjectFactory getFactory() {
        return m_pool.getObjectFactory();
    }

    public Object getObject() {
        if (m_obj == null) m_obj = m_pool.getObject();
        return m_obj;
    }

    public boolean hasObject() {
        return m_obj != null;
    }

    public void release() {
        if (m_obj instanceof ReleaseAwareObject) {
            ((ReleaseAwareObject)m_obj).release();
        }
        if (m_stale) releaseAndRemoveObject(); else releaseObject();
    }

    public void markStale() {
        m_stale = true;
    }

    public void setObject(Object o) throws PoolException {
        if (m_obj != null) throw new PoolException("Object already set");
        m_obj = o;
    }

    public void releaseObject() {
        if (m_obj != null) {
            m_pool.releaseObject(m_obj);
        }
    }

    public void releaseAndRemoveObject() {
        if (m_obj != null) {
            m_pool.releaseAndRemoveObject(m_obj);
        }
    }
}
