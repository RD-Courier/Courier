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
package ru.rd.pool2;

import ru.rd.pool.PoolObjectFactory;
import ru.rd.courier.logging.CourierLogger;

/**
 * User: AStepochkin
 * Date: 23.10.2008
 * Time: 11:59:26
 */
public class Pool2ListTest extends Test {

    protected PoolObjectFactory createObjectFactory(CourierLogger logger) {
        ObjFactory f = new ObjFactory(-1);
        f.setGetSleepInterval(0);
        f.setCheckSleepInterval(0);
        return f;
    }

    protected void setPoolProps() {
        super.setPoolProps();
        m_pool.setShrinkObjPeriod(1);
        m_pool.setCloseTimeout(1000);
    }

    public void test() throws Exception {
        Object obj0 = m_pool.getObject();
        Object obj1 = m_pool.getObject();
        Object obj2 = m_pool.getObject();
        m_pool.releaseObject(obj0);
        m_pool.releaseObject(obj2);
        Thread.sleep(100);
        m_pool.shrink();
        assertEquals(0, m_pool.freeSize());
        assertEquals(1, m_pool.busySize());
        assertEquals(1, m_pool.size());
        m_pool.releaseObject(obj1);
        assertEquals(0, m_pool.busySize());
    }
}
