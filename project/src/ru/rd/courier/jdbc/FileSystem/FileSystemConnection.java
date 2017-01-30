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
package ru.rd.courier.jdbc.FileSystem;

import ru.rd.courier.jdbc.ConnectionDrivenJdbcStatement;
import ru.rd.courier.jdbc.ConnectionSkeleton;
import ru.rd.courier.jdbc.CorrectUpdateResult;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.scripting.dataaccess.FileSystemSource;
import ru.rd.courier.logging.CourierLoggerAdapter;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 21.03.2005
 * Time: 13:19:42
 */
public class FileSystemConnection extends ConnectionSkeleton {
    private static int s_lastConNumber = 0;
    private FileSystemSource m_os;

    public FileSystemConnection(Properties info) {
        this(
            StringHelper.boolParam(info, "show-exec-output", false),
            StringHelper.boolParam(info, "no-error-stream-exception", false)
        );
    }

    public FileSystemConnection(boolean showExecOutput, boolean noErrorStreamException) {
        super(++s_lastConNumber);
        m_os = new FileSystemSource(new CourierLoggerAdapter(m_logger), null, showExecOutput, noErrorStreamException);
    }

    protected List innerRequest(ConnectionDrivenJdbcStatement stmt, String request) throws SQLException {
        List res = new LinkedList();
        ResultSet rs = m_os.generalRequest(request);
        if (rs == null) res.add(new CorrectUpdateResult(1));
        else res.add(rs);
        return res;
    }

    public synchronized void close() throws SQLException {
        m_os.close();
        super.close();
    }
}
