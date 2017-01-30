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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * User: AStepochkin
 * Date: 14.12.2006
 * Time: 15:08:59
 */
public abstract class IteratingResultSet extends EmptyResultSet {
    private final Statement m_stmt;
    private int m_curPos = 0;
    private boolean m_finished = false;

    protected abstract boolean getRecord() throws SQLException;
    protected abstract int skipRecords(int count) throws SQLException;
    protected void beforeNextRecord() throws SQLException {}

    public IteratingResultSet(Statement stmt) {
        m_stmt = stmt;
    }

    public boolean relative(int rows) throws SQLException {
        if (rows < 0) {
            throw new IllegalArgumentException(
                "Forward only result set (rows = " + rows + ")"
            );
        }
        if (m_finished) return false;
        if (rows > 1) {
            int skipCount = skipRecords(rows - 1);
            m_curPos += skipCount;
            if (skipCount < (rows - 1)) {
                m_finished = true;
                return false;
            }
        }
        beforeNextRecord();
        if (!getRecord()) {
            m_finished = true;
            return false;
        }
        m_curPos++;
        return true;
    }

    public boolean next() throws SQLException {
        return relative(1);
    }

    protected byte m_wasNull = EMPTY_NULL;
    protected static final byte EMPTY_NULL = -1;
    protected static final byte WAS_NULL = 0;
    protected static final byte NOT_WAS_NULL = 1;

    public boolean wasNull() throws SQLException {
        checkRecordRead();
        if (m_wasNull == EMPTY_NULL) throw new SQLException("No field has been read");
        return m_wasNull == WAS_NULL;
    }

    public int getConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    public int getRow() throws SQLException {
        return m_curPos;
    }

    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    public void checkRecordRead() throws SQLException {
        if (m_curPos < 1) throw new SQLException("No record has been read");
    }

    public Statement getStatement() throws SQLException {
        return m_stmt;
    }
}
