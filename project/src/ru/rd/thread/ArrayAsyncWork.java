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
package ru.rd.thread;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.CloseEventListener;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolException;

import java.util.Timer;

/**
 * User: AStepochkin
 * Date: 16.11.2007
 * Time: 10:02:50
 */
public class ArrayAsyncWork extends AsyncBuffer<ArrayDataPart> {
    private int m_maxCount;
    private int m_count;

    public ArrayAsyncWork(
        CourierLogger logger,
        ObjectPoolIntf threadPool, int maxThreadCnt, Timer timer,
        long period, int partSize,
        AsyncBuffer.Receiver<ArrayDataPart> receiver, int maxCount,
        CloseEventListener closeListener
    ) {
        super(
            logger, threadPool, maxThreadCnt,
            timer, period,
            receiver, partSize, closeListener
        );
        m_maxCount = maxCount;
        m_count = 0;
    }

    protected void objectRemoving(ArrayDataPart obj) {
        m_count -= obj.data.length;
    }

    protected void objectAdding(ArrayDataPart obj) throws PoolException {
        m_count += obj.data.length;
        if (m_count > m_maxCount) flush();
    }
}
