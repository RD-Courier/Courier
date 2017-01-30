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

import ru.rd.courier.jdbc.EmptyResultSetMetaData;

import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * User: AStepochkin
 * Date: 28.11.2007
 * Time: 10:36:46
 */
public class StringListResultSet extends IteratingResultSet {
    private final String[] m_colNames;
    private final Iterator<String[]> m_dataIt;

    private String[] m_data = null;

    public StringListResultSet(String[] colNames, Iterator<String[]> dataIt) {
        super(null);
        m_colNames = colNames;
        m_dataIt = dataIt;
    }

    public StringListResultSet(List<String> colNames, List<String[]> data) {
        this(colNames.toArray(new String[colNames.size()]), data.listIterator());
    }

    public static List<String[]> getOneRowList(String[] data) {
        List<String[]> res = new ArrayList<String[]>(1);
        res.add(data);
        return res;
    }

    public StringListResultSet(List<String> colNames, String[] data) {
        this(colNames, getOneRowList(data));
    }

    public StringListResultSet(String[] colNames, String[] data) {
        this(colNames, getOneRowList(data).listIterator());
    }

    protected boolean getRecord() throws SQLException {
        if (!m_dataIt.hasNext()) return false;
        m_data = m_dataIt.next();
        return true;
    }

    protected int skipRecords(int count) throws SQLException {
        int i;
        for (i = 0; i < count; i++) {
            if (!m_dataIt.hasNext()) break;
            m_dataIt.next();
        }
        return i;
    }

    public void close() throws SQLException {
        m_data = null;
    }

    private int innerFindColumn(String columnName) throws SQLException {
        for (int i = 0; i < m_colNames.length; i++) {
            if (m_colNames[i].equals(columnName)) return i;
        }
        throw new SQLException(
            "Result set does not contain column '" + columnName + "'");
    }

    private String innerGetString(int index) {
        final String res = m_data[index];
        m_wasNull = res == null ? WAS_NULL : NOT_WAS_NULL;
        return res;
    }

    public String getString(int columnIndex) throws SQLException {
        if ((columnIndex < 1) || (columnIndex > m_colNames.length)) {
            throw new SQLException("Invalid column index '" + columnIndex + "'");
        }
        return innerGetString(columnIndex - 1);
    }

    public String getString(String columnName) throws SQLException {
        return innerGetString(innerFindColumn(columnName));
    }

    private class MetaData extends EmptyResultSetMetaData {
        public int getColumnCount() throws SQLException {
            return m_colNames.length;
        }

        public String getColumnName(int column) throws SQLException {
            return m_colNames[column - 1];
        }

        public int getPrecision(int column) throws SQLException {
            return 0;
        }

        public int getScale(int column) throws SQLException {
            return 0;
        }

        public int getColumnType(int column) throws SQLException {
            return java.sql.Types.VARCHAR;
        }

        public String getColumnTypeName(int column) throws SQLException {
            return "VARCHAR";
        }
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new MetaData();
    }
}
