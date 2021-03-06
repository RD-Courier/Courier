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
import ru.rd.pool.ObjectPoolIntf;

/**
 * User: STEPOCHKIN
 * Date: 26.08.2008
 * Time: 12:32:31
 */
public class SynchExecPolicy implements ExecutePolicy {
    public SynchExecPolicy() {
    }

    public Object createThreadContext() {
        return null;
    }

    public void closeThreadContext(Object threadContext) {}

    public Object allocateObject(ObjectPoolIntf pool, Object bulkContext, PoolObjectFactory factory) throws Exception {
        return factory.getObject(pool);
    }

    public void deallocateObject(Object bulkContext, Object o, PoolObjectFactory factory) {
        factory.returnObject(o);
    }

    public boolean objectWrapperValid(Object bulkContext, Object o, PoolObjectFactory factory) {
        return factory.checkObject(o);
    }
}
