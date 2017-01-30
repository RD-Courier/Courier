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
package ru.rd.courier.scripting.dataaccess;

import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.scripting.DataReceiver;
import ru.rd.courier.scripting.ReceiverTimeCounter;
import ru.rd.courier.scripting.StandardOperationSupport;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.dataaccess.jdbc.JdbcReceiver;

import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LoggingDataReceiver implements DataReceiver, JdbcReceiver, ReceiverTimeCounter, StandardOperationSupport {
    protected final CourierLogger m_logger;
    protected final DataReceiver m_dr;
    protected final DataLogger m_dataLogger;
    protected final String m_sRule;

    private static DateFormat s_dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public LoggingDataReceiver(CourierLogger logger, DataReceiver dr, DataLogger dataLogger, String sRule) {
        m_logger = logger;
        m_dr = dr;
        m_dataLogger = dataLogger;
        m_sRule = sRule;
    }

    public final List<LinkWarning> process(Object operation) throws CourierException {
        m_dataLogger.log("/* " + m_sRule + " " + s_dataFormat.format(new Date()) + " */\n");
        m_dataLogger.log(operation.toString());
        m_dataLogger.log("\n");
        m_dataLogger.flush();
        return m_dr.process(operation);
    }

    public final List<LinkWarning> flush() throws CourierException {
        return m_dr.flush();
    }

    public final void clearTargetTime() {
        if (m_dr instanceof ReceiverTimeCounter) {
            ((ReceiverTimeCounter)m_dr).clearTargetTime();
        }
    }

    public final long getTargetTime() {
        if (m_dr instanceof ReceiverTimeCounter) {
            return ((ReceiverTimeCounter)m_dr).getTargetTime();
        } else {
            return -1;
        }
    }

    public final void setTimeout(int timeout) throws CourierException {
        m_dr.setTimeout(timeout);
    }

    public final void cancel() throws CourierException {
        m_dr.cancel();
    }

    public final void close() throws CourierException {
        m_dr.close();
    }

    public String toString() {
        return (
            "rule = " + m_sRule + "; " + m_dr.toString()
        );
    }

    public final String getType() {
        return ((StandardOperationSupport)m_dr).getType();
    }

    public final Connection getConnection() {
        return ((StandardOperationSupport)m_dr).getConnection();
    }

    public void setAutoCommit(boolean autoCommit) {
        if (m_dr instanceof JdbcReceiver) {
            ((JdbcReceiver)m_dr).setAutoCommit(autoCommit);
        }
    }
}
