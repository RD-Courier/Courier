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
package ru.rd.courier.jdbc.databuffer;

import org.w3c.dom.Element;
import ru.rd.courier.jdbc.EmptyResultSet;
import ru.rd.courier.jdbc.GenericSqlException;
import ru.rd.courier.utils.DomHelper;

import java.sql.Date;
import java.sql.*;
import java.util.*;

public class DataBuffer extends EmptyResultSet {
    private RecordBuffer m_record = new RecordBuffer();

    protected List<byte[]> m_data = new LinkedList<byte[]>();
    private int m_curPos = -1;
    private ListIterator m_relMove = null;
    private byte m_moveDirection = c_NoneMove;

    private static final byte c_NoneMove = 0;
    private static final byte c_ForwardMove = 1;
    private static final byte c_BackwardMove = 2;

    public DataBuffer() {
        initMetaInfo();
    }

    public void addColumn(ColumnInfo ci, int index) throws SQLException {
        m_record.addColumn(ci, index);
        initColumnData(index);
    }

    public void addColumn(ColumnInfo ci) throws SQLException {
        addColumn(ci, m_record.getColumnCount());
    }

    private void initColumnData(int index) throws SQLException {
        ColumnInfo ci = m_record.getColumn(index + 1);
        for (byte[] data : m_data) ci.initCustomValue(data);
    }

    protected void init(Element[] cols) throws SQLException {
        clearMoveCache();
        initMetaInfo();
        for (Element col : cols) {
            addColumn(ColumnInfo.getColumnInfo(col));
        }
    }

    private boolean setPosition(int pos) {
        clearMoveCache();
        if (pos < 0) {
            m_curPos = -1;
            setCurBuffer(null);
        } else if (pos >= m_data.size()) {
            m_curPos = m_data.size();
            setCurBuffer(null);
        } else {
            m_curPos = pos;
            setCurBuffer(m_data.get(pos));
        }
        return getCurBuffer() != null;
    }

    public void clearMoveCache() {
        m_relMove = null;
        m_moveDirection = c_NoneMove;
    }

    private void setCurBuffer(byte[] buffer) {
        m_record.setCurBuffer(buffer);
    }

    private byte[] getCurBuffer() {
        return m_record.getCurBuffer();
    }

    public synchronized void addRecord() throws SQLException {
        clearMoveCache();
        byte[] buffer = m_record.createBuffer();
        setCurBuffer(buffer);
        m_record.init();
        m_data.add(buffer);
        m_curPos = m_data.size();
    }

    private static final String c_FieldValue = "value";
    public synchronized void addRecord(Element e) throws SQLException {
        addRecord();
        Element[] nl = DomHelper.getChildrenByTagName(e, "field");
        for (int i = 0; i < nl.length; i++) {
            final Element n = nl[i];
            if (!n.hasAttribute("name")) {
                throw new GenericSqlException(this, "Unspecified name for field #" + i);
            }

            /*
            System.out.println(
                "addRecord: name=" + nl[i].getAttribute("name") +
                " value=" + nl[i].getAttribute("value")
            );
            */

            final String val = n.hasAttribute(c_FieldValue) ?
                n.getAttribute(c_FieldValue) : DomHelper.getNodeValue(n);
            updateString(n.getAttribute("name"), val);
        }
    }

    public synchronized void addRecord(String[] data) throws SQLException {
        addRecord();
        for (int i = 0; i < data.length; i += 2) {
            updateString(data[i], data[i+1]);
        }
    }

    public synchronized void removeRecord(int index) throws SQLException {
        if ((index < 0) || (index >= m_data.size())) {
            throw new GenericSqlException(this, "Invalid index " + index);
        }
        clearMoveCache();
        m_data.remove(index);
        if (index == m_curPos) setPosition(-1);
    }

    private void initMetaInfo() {
        clearMoveCache();
        setPosition(-1);
        m_data.clear();
    }

    public void importMetaInfo(DataBuffer db) {
        m_record.importMetaInfo(db.getRecord());
        initMetaInfo();
    }

    private RecordBuffer getRecord() {
        return m_record;
    }

    public Object clone() throws CloneNotSupportedException {
        super.clone();
        DataBuffer ret = new DataBuffer();
        ret.m_record = (RecordBuffer)m_record.clone();
        ret.m_data = new LinkedList<byte[]>(m_data);
        ret.m_curPos = m_curPos;
        ret.m_moveDirection = m_moveDirection;

        if (m_relMove != null) {
            ret.m_relMove = ret.m_data.listIterator(ret.m_curPos);
        }
        return ret;
    }

    public void importRecord(byte[] buffer) {
        byte[] bb = new byte[buffer.length];
        System.arraycopy(buffer, 0, bb, 0, buffer.length);
        m_data.add(bb);
    }


    public void importRecord(DataBuffer buf) throws SQLException {
        addRecord();
        m_record.importRecord(buf);
    }

    // *********** Result Set implementation ***********
    public int getConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    public int getFetchSize() throws SQLException {
        return m_record.getRecordSize();
    }

    public int getRow() throws SQLException {
        return (m_curPos >= m_data.size()) ? 0 : (m_curPos + 1);
    }

    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    public void afterLast() throws SQLException {
        setPosition(m_data.size());
    }

    public void beforeFirst() throws SQLException {
        setPosition(-1);
    }

    public void close() throws SQLException {
        m_data = null;
    }

    public boolean first() throws SQLException {
        if (m_data.size() > 0) {
            setPosition(0);
            return true;
        } else {
            return false;
        }
    }

    public boolean isAfterLast() throws SQLException {
        return m_curPos >= m_data.size();
    }

    public boolean isBeforeFirst() throws SQLException {
        return m_curPos < 0;
    }

    public boolean isFirst() throws SQLException {
        return m_curPos == 0;
    }

    public boolean isLast() throws SQLException {
        return m_curPos == (m_data.size() - 1);
    }

    public boolean last() throws SQLException {
        if (m_data.size() > 0) {
            setPosition(m_data.size() - 1);
            return true;
        } else {
            return false;
        }
    }

    public boolean next() throws SQLException {
        return relative(1);
    }

    public boolean previous() throws SQLException {
        return relative(-1);
    }

    public boolean absolute(int row) throws SQLException {
        return setPosition(row);
    }

    public boolean relative(int rows) throws SQLException {
        boolean ret;
        int pos = m_curPos + rows;

        if ((pos < 0) || (pos >= m_data.size())) {
            ret = setPosition(pos);
        } else {
            if (rows == 1) {
                if (m_moveDirection != c_ForwardMove) {
                    m_moveDirection = c_ForwardMove;
                    m_relMove = m_data.listIterator(pos);
                }
                m_record.setCurBuffer((byte[])m_relMove.next());
                m_curPos = pos;
            } else if (rows == -1) {
                if (m_moveDirection != c_BackwardMove) {
                    m_moveDirection = c_BackwardMove;
                    m_relMove = m_data.listIterator(pos);
                }
                m_record.setCurBuffer((byte[])m_relMove.previous());
                m_curPos = pos;
            } else {
                setPosition(pos);
            }
            ret = true;
        }
        return ret;
    }

    public int findColumn(String columnName) throws SQLException {
        return m_record.findColumn(columnName);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return m_record.createMetaData();
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        m_record.updateString(columnIndex, x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        m_record.updateString(columnName, x);
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        m_record.updateInt(columnIndex, x);
    }

    public void updateInt(String columnName, int x) throws SQLException {
        m_record.updateInt(columnName, x);
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        m_record.updateLong(columnIndex, x);
    }

    public void updateLong(String columnName, long x) throws SQLException {
        m_record.updateLong(columnName, x);
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        m_record.updateDate(columnIndex, x);
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        m_record.updateDate(columnName, x);
    }

    public void updateNull(int columnIndex) throws SQLException {
        m_record.updateNull(columnIndex);
    }

    public void updateNull(String columnName) throws SQLException {
        m_record.updateNull(columnName);
    }

    public boolean wasNull() throws SQLException {
        return m_record.wasNull();
    }

    public java.sql.Date getDate(int columnIndex) throws SQLException {
        return m_record.getDate(columnIndex);
    }

    public Date getDate(String columnName) throws SQLException {
        return m_record.getDate(columnName);
    }

    public float getFloat(int columnIndex) throws SQLException {
        return m_record.getFloat(columnIndex);
    }

    public float getFloat(String columnName) throws SQLException {
        return m_record.getFloat(columnName);
    }

    public int getInt(int columnIndex) throws SQLException {
        return m_record.getInt(columnIndex);
    }

    public int getInt(String columnName) throws SQLException {
        return m_record.getInt(columnName);
    }

    public long getLong(int columnIndex) throws SQLException {
        return m_record.getLong(columnIndex);
    }

    public long getLong(String columnName) throws SQLException {
        return m_record.getLong(columnName);
    }

    public String getString(int columnIndex) throws SQLException {
        return m_record.getString(columnIndex);
    }

    public String getString(String columnName) throws SQLException {
        return m_record.getString(columnName);
    }

    public Time getTime(int columnIndex) throws SQLException {
        return m_record.getTime(columnIndex);
    }
}
