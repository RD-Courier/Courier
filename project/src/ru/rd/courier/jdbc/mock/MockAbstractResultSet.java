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

import ru.rd.courier.jdbc.EmptyResultSet;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public abstract class MockAbstractResultSet extends EmptyResultSet {
    private final Statement m_stmt;
    protected int m_pos;

    protected abstract int getRowCount();
    protected abstract int getColCount();

    public MockAbstractResultSet(Statement stmt) {
        m_stmt = stmt;
    }

    public boolean next() throws SQLException {
        if (m_pos <= getRowCount()) m_pos++;
        return (m_pos <= getRowCount());
    }

    public boolean wasNull() throws SQLException {
        return false;
    }

    public String getString(String columnName) throws SQLException {
        return getString(findColumn(columnName));
    }

    public Date getDate(String columnName) throws SQLException {
        return getDate(findColumn(columnName));
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {
    }

    public String getCursorName() throws SQLException {
        return "Mock cursor";
    }

    public boolean isBeforeFirst() throws SQLException {
        return (m_pos < 1);
    }

    public boolean isAfterLast() throws SQLException {
        return (m_pos > getRowCount());
    }

    public boolean isFirst() throws SQLException {
        return (m_pos == 1);
    }

    public boolean isLast() throws SQLException {
        return (m_pos == getRowCount());
    }

    public void beforeFirst() throws SQLException {
        m_pos = 0;
    }

    public void afterLast() throws SQLException {
        m_pos = getRowCount() + 1;
    }

    public boolean first() throws SQLException {
        m_pos = 1;
        return true;
    }

    public boolean last() throws SQLException {
        m_pos = getRowCount();
        return true;
    }

    public int getRow() throws SQLException {
        return m_pos;
    }

    public boolean absolute(int row) throws SQLException {
        m_pos = row;
        return true;
    }

    public boolean relative(int rows) throws SQLException {
        m_pos += rows;
        return true;
    }

    public boolean previous() throws SQLException {
        if (m_pos > 0) m_pos--;
        return (m_pos != 0);
    }

    public Statement getStatement() throws SQLException {
        return m_stmt;
    }
}
