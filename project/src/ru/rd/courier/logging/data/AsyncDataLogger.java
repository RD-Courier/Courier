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
package ru.rd.courier.logging.data;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.CloseEventListener;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolException;
import ru.rd.thread.ArrayAsyncWork;
import ru.rd.thread.ArrayDataPart;
import ru.rd.thread.AsyncBuffer;

import java.util.List;
import java.util.Timer;

public class AsyncDataLogger extends AbstractDataLogger {
    private CourierLogger m_logger;
    private DataLogger m_targetLogger;
    private ArrayAsyncWork m_pusher;

    public AsyncDataLogger(
        CourierLogger logger, DataLogger targetLogger, ObjectPoolIntf threadPool,
        Timer timer, long period, int partSize, int maxChars,
        CloseEventListener closeListener
    ) {
        m_logger = logger;
        m_targetLogger = targetLogger;
        m_pusher = new ArrayAsyncWork(
            logger, threadPool, 4, timer,
            period, partSize,
            new AsyncBuffer.Receiver<ArrayDataPart>() {
                public void handleData(List<ArrayDataPart> dataPart) {
                    for (ArrayDataPart aDataPart : dataPart) {
                        m_targetLogger.log(aDataPart.data, aDataPart.offset, aDataPart.length);
                    }
                }

                public void close() {
                    m_targetLogger.close();
                }
            },
            maxChars, closeListener
        );
    }

    public void log(byte[] msg, int offset, int length) {
        try {
            m_pusher.add(new ArrayDataPart(msg, offset, length));
        } catch (PoolException e) {
            m_logger.error(e);
        }
    }

    public void flush() {
        m_targetLogger.flush();
    }

    public void close() {
        try {
            m_pusher.close();
        } catch (PoolException e) {
            m_logger.error(e);
        }
    }
}
