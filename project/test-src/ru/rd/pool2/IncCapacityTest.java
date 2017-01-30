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
 * User: STEPOCHKIN
 * Date: 11.09.2008
 * Time: 17:58:50
 */
public class IncCapacityTest extends Test {
    private static final int c_sleepInterval = 300;
    private static final int c_incCapacity = 10;

    protected PoolObjectFactory createObjectFactory(CourierLogger logger) {
        ObjFactory f = new ObjFactory(-1);
        f.setGetSleepInterval(0);
        f.setCheckSleepInterval(0);
        return f;
    }

    protected void setPoolProps() {
        super.setPoolProps();
        m_pool.setRinseInterval(c_sleepInterval / 3);
        m_pool.setMinCapacity(0);
        m_pool.setIncCapacity(c_incCapacity);
        m_pool.setRecreateInterval(c_sleepInterval / 3);
    }

    public void test() throws Exception {
        sleep(c_sleepInterval);
        assertEquals(0, m_pool.freeSize());
        m_pool.releaseObject(m_pool.getObject(c_sleepInterval / 2));
        sleep(c_sleepInterval);
        assertEquals(c_incCapacity, m_pool.freeSize());
    }
}
