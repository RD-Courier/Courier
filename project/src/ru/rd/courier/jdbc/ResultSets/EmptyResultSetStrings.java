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
package ru.rd.courier.jdbc.ResultSets;

import ru.rd.courier.jdbc.EmptyResultSet;
import ru.rd.courier.jdbc.EmptyResultSetMetaData;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 23.11.2007
 * Time: 20:00:44
 */
public class EmptyResultSetStrings extends EmptyResultSet {
    private final String[] m_colNames;

    public EmptyResultSetStrings(String[] colNames) {
        m_colNames = colNames;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new EmptyResultSetMetaData() {
            public int getColumnCount() {
                return m_colNames.length;
            }

            public String getColumnName(int column) {
                return m_colNames[column - 1];
            }

            public int getPrecision(int column) {
                return 0;
            }

            public int getScale(int column) {
                return 0;
            }

            public int getColumnType(int column) {
                return java.sql.Types.VARCHAR;
            }

            public String getColumnTypeName(int column) {
                return "VARCHAR";
            }
        };
    }
}
