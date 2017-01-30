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

import ru.rd.courier.logging.CourierLogger;

/**
 * User: AStepochkin
 * Date: 29.06.2005
 * Time: 10:00:56
 */
public class SynchObjectPool extends ObjectPool {
    public SynchObjectPool(
        String desc, CourierLogger logger, PoolObjectFactory objFactory
    ) {
        super(desc, logger, objFactory);
    }

    public SynchObjectPool(
        String desc, CourierLogger logger,
        PoolObjectFactory objFactory,
        int initialCapacity, int incCapacity, int maxCapacity,
        long shrinkInterval, int shrinkCapacity, long shrinkObjPeriod,
        long checkInterval,
        long expirePeriod
    ) {
        super(
            desc, logger, objFactory,
            initialCapacity, incCapacity, maxCapacity,
            shrinkInterval, shrinkCapacity, shrinkObjPeriod,
            checkInterval, expirePeriod
        );
    }

    public SynchObjectPool(
        String desc, CourierLogger logger,
        PoolObjectFactory objFactory,
        int initialCapacity, int incCapacity, int maxCapacity,
        long shrinkInterval, int shrinkCapacity, long shrinkObjPeriod,
        long checkInterval
    ) {
        this(
            desc, logger, objFactory,
            initialCapacity, incCapacity, maxCapacity,
            shrinkInterval, shrinkCapacity, shrinkObjPeriod,
            checkInterval,
            -1
        );
    }

    protected Object allocateObject() throws Exception {
        return m_objFactory.getObject(this);
    }

    protected boolean objectWrapperValid(Object o) {
        return m_objFactory.checkObject(o);
    }

    protected void deallocateObject(Object o) {
        m_objFactory.returnObject(o);
    }
}
