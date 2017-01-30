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
package ru.rd.courier.jdbc.mock;

import ru.rd.courier.jdbc.RdDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MockDriver extends RdDriver {
    private static final String c_driverName = "mock";
    public static final String c_fileNameParam = "output-file-name";

    static {
        try {
            DriverManager.registerDriver(new MockDriver());
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    private MockDriver() {
        super(
            c_driverName,
            new ConnectionFactory() {
                public Connection create(Properties info) throws SQLException {
                    return MockDatabase.getGlobalDatabase().getConnection(info);
                }
            }
        );
    }

    protected void parseProperties(Properties props, String parsUrlPart) {
        if (parsUrlPart.length() > 0) {
            props.setProperty(c_fileNameParam, parsUrlPart);
        }
    }

    protected boolean checkUrl(Properties props) throws SQLException {
        return props.containsKey(c_fileNameParam);
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 1;
    }
}
