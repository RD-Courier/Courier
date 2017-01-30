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
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LoggingDataSource extends LoggingDataReceiver implements DataSource {
    private static DateFormat s_dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public LoggingDataSource(CourierLogger logger, DataSource ds, DataLogger dataLogger, String sRule) {
        super(logger, ds, dataLogger, sRule);
    }

    public ResultSet request(String query) throws CourierException {
        m_dataLogger.log("/* " + (m_sRule != null ? (m_sRule + " ") : "") + s_dataFormat.format(new Date()) + "\n");
        m_dataLogger.log(query);
        m_dataLogger.log("\n*/\n");
        m_dataLogger.flush();
        return ((DataSource)m_dr).request(query);
    }

    public String toString() {
        return (
            (m_sRule != null ? "rule = " + m_sRule + "; ": "") + m_dr.toString()
        );
    }
}
