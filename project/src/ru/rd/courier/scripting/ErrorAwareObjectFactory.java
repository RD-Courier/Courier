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

import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.courier.CourierException;

/**
 * User: AStepochkin
 * Date: 06.10.2006
 * Time: 17:30:23
 */
public class ErrorAwareObjectFactory implements PoolObjectFactory {
    private final PoolObjectFactory m_realFactory;
    private final int m_maxErrorCount;

    public ErrorAwareObjectFactory(PoolObjectFactory realFacrory, int maxErrorCount) {
        m_realFactory = realFacrory;
        m_maxErrorCount = maxErrorCount;
    }

    public Object getObject(ObjectPoolIntf pool) throws Exception {
        Object obj = m_realFactory.getObject(pool);
        ErrorAwareDataObject ret;
        if (obj instanceof DataReceiver) {
            ret = new ErrorAwareDataSource();
        } else if (obj instanceof MapDataSource) {
            ret = new ErrorAwareMapDataSource();
        } else {
            throw new CourierException("No error aware implementation found");
        }

        ret.setObject(m_realFactory.getObject(pool));
        ret.setMaxErrorCount(m_maxErrorCount);
        return ret;
    }

    public void returnObject(Object o) {
        m_realFactory.returnObject(((ErrorAwareDataObject)o).getObject());
    }

    public boolean checkObject(Object o) {
        return m_realFactory.checkObject(((ErrorAwareDataObject)o).getObject());
    }
}
