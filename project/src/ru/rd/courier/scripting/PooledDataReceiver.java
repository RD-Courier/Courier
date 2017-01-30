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

import ru.rd.courier.CourierException;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.ReleaseHelper;

import java.util.List;

/**
 * User: Astepochkin
 * Date: 30.09.2008
 * Time: 18:33:38
 */
public class PooledDataReceiver extends ReleaseHelper implements DataReceiver {
    public PooledDataReceiver(ObjectPoolIntf pool) {
        super(pool);
    }

    private DataReceiver rec() {
        return (DataReceiver)getObject();
    }

    public List<LinkWarning> process(Object operation) throws CourierException {
        return rec().process(operation);
    }

    public List<LinkWarning> flush() throws CourierException {
        return rec().flush();
    }

    public void setTimeout(int timeout) throws CourierException {
        rec().setTimeout(timeout);
    }

    public void cancel() throws CourierException {
        rec().cancel();
    }

    public void close() throws CourierException {
        release();
    }
}
