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

import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.jdbc.ResultSets.StringBufferedResultSet;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 14.08.2006
 * Time: 9:16:03
 */
public class StringBufferListResultSet extends StringBufferedResultSet {
    private final Iterator<StringBuffer[]> m_dataIt;

    public StringBufferListResultSet(
        Statement stmt, IterColumnInfo[] infos, List<StringBuffer[]> data
    ) {
        super(stmt, infos);
        m_dataIt = data.iterator();
    }

    private static List<StringBuffer[]> recordToList(StringBuffer[] data) {
        List<StringBuffer[]> ret = new LinkedList<StringBuffer[]>();
        ret.add(data);
        return ret;
    }

    public StringBufferListResultSet(
        Statement stmt, IterColumnInfo[] infos, StringBuffer[] data
    ) {
        this(stmt, infos, recordToList(data));
    }

    public StringBufferListResultSet(
        Statement stmt, List<IterColumnInfo> infos, List<StringBuffer> data
    ) {
        this(
            stmt, infos.toArray(new IterColumnInfo[infos.size()]),
            recordToList(data.toArray(new StringBuffer[data.size()]))
        );
    }

    public StringBufferListResultSet(
        Statement stmt, String colName, String data
    ) {
        this(
            stmt,
            new IterColumnInfo[] {new IterColumnInfo(colName)},
            new StringBuffer[] {new StringBuffer(data)}
        );
    }

    protected boolean getRecord() throws SQLException {
        if (!m_dataIt.hasNext()) return false;
        m_data = m_dataIt.next();
        return true;
    }

    protected boolean needToClearNulls() {
        return false;
    }

    protected int skipRecords(int count) throws SQLException {
        int i;
        for (i = 0; i < count; i++) {
            if (!m_dataIt.hasNext()) break;
            m_dataIt.next();
        }
        return i;
    }
}
