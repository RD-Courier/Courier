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

import java.sql.Date;
import java.sql.SQLException;

public class StringColumnInfo extends ColumnInfo {
    private StringColumnInfo(StringColumnInfo obj) {
        super(obj);
    }

    public StringColumnInfo(String name, boolean allowsNull, int size) {
        super(name, java.sql.Types.VARCHAR, allowsNull, "VARCHAR", size);
    }

    public StringColumnInfo(String name, int size) {
        super(name, java.sql.Types.VARCHAR, "VARCHAR", size);
    }

    public StringColumnInfo(Element e) {
        super(e, java.sql.Types.VARCHAR, "VARCHAR", 0);
        init(e.hasAttribute("size") ? Integer.parseInt(e.getAttribute("size")) : 128);
    }

    private void init(int size) {
        m_precision = size;
    }

    public void serializeString(byte[] buffer, String value) throws SQLException {
        if (value.length() > m_precision) {
            throw new SQLException("String exceeded max size for column '" + m_name + "'");
        }
        intToBytes(buffer, getCustomDataPosition(), value.length());
        stringToBytes(buffer, getCustomDataPosition() + 4, value);
    }

    public String getDataString(byte[] buffer) throws SQLException {
        int size = intFromBytes(buffer, getCustomDataPosition());
        return new String(buffer, getCustomDataPosition() + 4, size);
    }

    public void setInteger(byte[] buffer, int value) throws SQLException {
        serializeString(buffer, Integer.toString(value));
    }

    public int getDataInteger(byte[] buffer) throws SQLException {
        return Integer.parseInt(getDataString(buffer));
    }

    public void setLong(byte[] buffer, long value) throws SQLException {
        serializeString(buffer, Long.toString(value));
    }

    public long getDataLong(byte[] buffer) throws SQLException {
        return Long.parseLong(getDataString(buffer));
    }

    public void setFloat(byte[] buffer, float value) throws SQLException {
        serializeString(buffer, Float.toString(value));
    }

    public float getDataFloat(byte[] buffer) throws SQLException {
        return Float.parseFloat(getDataString(buffer));
    }

    public void setDataDate(byte[] buffer, Date value) throws SQLException {
        serializeString(buffer, dateToStr(value));
    }

    public Date getDataDate(byte[] buffer) throws SQLException {
        return strToDate(getDataString(buffer));
    }

    public void initCustomValue(byte[] buffer) throws SQLException {
        serializeString(buffer, "");
    }

    public static void stringToBytes(byte[] buffer, int pos, String value) {
        byte[] bv = value.getBytes();
        System.arraycopy(bv, 0, buffer, pos, value.length());
    }

    public int getAdditionalByteSize() {
        return 4;
    }

    public Object clone() {
        return new ru.rd.courier.jdbc.databuffer.StringColumnInfo(this);
    }
}
