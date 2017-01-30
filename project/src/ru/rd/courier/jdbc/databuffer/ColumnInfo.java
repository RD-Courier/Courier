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
import ru.rd.courier.jdbc.GenericSqlException;
import ru.rd.courier.utils.DomHelper;

import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;

public abstract class ColumnInfo {
    public String m_name;
    public int m_type;
    public String m_typeName;
    protected final boolean m_allowsNull;
    public int m_precision = 0;
    public int m_scale = 0;
    private int m_dataPosition;
    protected boolean m_wasNull;

    protected ColumnInfo(ColumnInfo obj) {
        m_name = obj.m_name;
        m_type = obj.m_type;
        m_typeName = obj.m_typeName;
        m_allowsNull = obj.m_allowsNull;
        m_precision = obj.m_precision;
        m_scale = obj.m_scale;
        setDataPosition(obj.getCustomDataPosition());
    }

    protected ColumnInfo(
        String name, int type, boolean allowsNull, String typeName, int precision
    ) {
        m_name = name;
        m_type = type;
        m_typeName = typeName;
        m_allowsNull = allowsNull;
        m_precision = precision;
        m_scale = 0;
        setDataPosition(-1);
    }

    protected ColumnInfo(String name, int type, String typeName, int precision) {
        this(name, type, false, typeName, precision);
    }

    protected ColumnInfo(Element e, int type, String typeName, int precision) {
        m_name = getNameFromNode(e);
        m_allowsNull = DomHelper.getBoolYesNo(e, "allows-null");
        m_type = type;
        m_typeName = typeName;
        m_precision = precision;
    }

    public static ColumnInfo getColumnInfo(Element e) throws SQLException {
        ColumnInfo ret = null;
        if (e.hasAttribute("type")) {
            String type = e.getAttribute("type");
            if (type.equals("integer")) {
                ret = new IntegerColumnInfo(e);
            } else if (type.equals("string")) {
                ret = new StringColumnInfo(e);
            } else if (type.equals("date")) {
                ret = new DateColumnInfo(e);
            } else if (type.equals("float")) {
                ret = new FloatColumnInfo(e);
            } else if (type.equals("big-int")) {
                ret = new BigIntColumnInfo(e);
            } else {
                throw new SQLException("Unknown Column type '" + type + "'");
            }
        } else {
            throw new SQLException("Unspecified type for column '" + getNameFromNode(e) + "'");
        }
        return ret;
    }

    public String getName() {
        return m_name;
    }

    public final int getByteSize() {
        return m_precision + (m_allowsNull ? 1 : 0) + getAdditionalByteSize();
    }

    public int getAdditionalByteSize() {
        return 0;
    }

    public int getType() throws SQLException {
        return m_type;
    }

    public String getTypeName() {
        return m_typeName;
    }

    private void updateWasNull(byte[] buffer) {
        m_wasNull = m_allowsNull && (buffer[getDataPosition()] == 0);
    }

    public boolean wasNull() {
        return m_wasNull;
    }

    public void setString(byte[] buffer, String value) throws SQLException {
        if(m_allowsNull) {
            serializeNull(buffer, value == null);
        } else {
            if(value == null) {
                throw new GenericSqlException(this, "Cannot set not nullable column to null value");
            }
        }
        serializeString(buffer, value);
    }
    protected abstract void serializeString(byte[] buffer, String value) throws SQLException;
    public String getString(byte[] buffer) throws SQLException {
        updateWasNull(buffer);
        if (m_wasNull) return null;
        return getDataString(buffer);
    }
    protected abstract String getDataString(byte[] buffer) throws SQLException;

    public abstract void setInteger(byte[] buffer, int value) throws SQLException;
    public int getInteger(byte[] buffer) throws SQLException {
        updateWasNull(buffer);
        if (m_wasNull) return 0;
        return getDataInteger(buffer);
    }
    protected abstract int getDataInteger(byte[] buffer) throws SQLException;

    public abstract void setLong(byte[] buffer, long value) throws SQLException;
    public long getLong(byte[] buffer) throws SQLException {
        updateWasNull(buffer);
        if (m_wasNull) return 0;
        return getDataLong(buffer);
    }
    protected abstract long getDataLong(byte[] buffer) throws SQLException;

    public abstract void setFloat(byte[] buffer, float value) throws SQLException;
    public float getFloat(byte[] buffer) throws SQLException {
        updateWasNull(buffer);
        if (m_wasNull) return 0;
        return getDataFloat(buffer);
    }
    protected abstract float getDataFloat(byte[] buffer) throws SQLException;

    public void setDate(byte[] buffer, java.sql.Date value) throws SQLException {
        if (value == null) {
            setNull(buffer);
            return;
        }
        setDataDate(buffer, value);
    }
    protected abstract void setDataDate(byte[] buffer, java.sql.Date value) throws SQLException;
    public java.sql.Date getDate(byte[] buffer) throws SQLException {
        updateWasNull(buffer);
        if (m_wasNull) return null;
        return getDataDate(buffer);
    }
    protected abstract java.sql.Date getDataDate(byte[] buffer) throws SQLException;

    public void initValue(byte[] buffer) throws SQLException {
        if (m_allowsNull) serializeNull(buffer, true);
        initCustomValue(buffer);
    }
    public abstract void initCustomValue(byte[] buffer) throws SQLException;
    public abstract Object clone();

    private void serializeNull(byte[] buffer, boolean value) {
        buffer[getDataPosition()] = (byte)(value ? 0 : 1);
    }

    protected void setNull(byte[] buffer, boolean value) throws GenericSqlException {
        if (m_allowsNull) {
            serializeNull(buffer, value);
        } else {
            throw new GenericSqlException(
                this, "Field '" + m_name + "' does not allow nulls"
            );
        }
    }

    public void setNull(byte[] buffer) throws GenericSqlException {
        setNull(buffer, true);
    }

    static int intFromBytes(byte[] buffer, int pos) {
        int ret = 0;
        int b;
        for (int i = 0; i < 4; i++) {
            b = buffer[pos + i];
            if (b < 0) b = b & 0xFF;
            ret <<= 8;
            ret |= b;
        }
        return ret;
    }

    static void intToBytes(byte[] buffer, int pos, int value) {
        for (int i = 3; i >= 0; i--) {
            buffer[pos + i] = (byte)value;
            value >>= 8;
        }
    }

    protected static long longFromBytes(byte[] buffer, int pos) {
        long ret = 0;
        long b;
        for (int i = 0; i < 8; i++) {
            b = buffer[pos + i];
            if (b < 0) b = b & 0xFF;
            ret <<= 8;
            ret |= b;
        }
        return ret;
    }

    protected static void longToBytes(byte[] buffer, int pos, long value) {
        for (int i = 7; i >= 0; i--) {
            buffer[pos + i] = (byte)value;
            value >>= 8;
        }
    }

    protected static java.sql.Date strToDate(String string) throws SQLException {
        try {
            return new Date(DateFormat.getDateInstance().parse(string).getTime());
        } catch (ParseException e) {
            SQLException sqle = new SQLException(e.getMessage());
            sqle.initCause(e);
            throw sqle;
        }
    }

    protected static String dateToStr(java.sql.Date date) {
        return DateFormat.getDateInstance().format(date);
    }

    protected static String getNameFromNode(Element e) {
        return e.getAttribute("name");
    }

    protected int getCustomDataPosition() {
        return m_dataPosition + (m_allowsNull ? 1 : 0);
    }

    protected int getDataPosition() {
        return m_dataPosition;
    }

    void setDataPosition(int dataPosition) {
        m_dataPosition = dataPosition;
    }
}