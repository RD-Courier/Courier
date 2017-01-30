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

public class FloatColumnInfo extends ColumnInfo {
    public FloatColumnInfo(ColumnInfo obj) {
        super(obj);
    }

    public FloatColumnInfo(String name) {
        super(name, java.sql.Types.FLOAT, "FLOAT", 4);
    }

    public FloatColumnInfo(Element e) {
        super(e, java.sql.Types.FLOAT, "FLOAT", 4);
    }

    public void serializeString(byte[] buffer, String value) throws SQLException {
        setFloat(buffer, Float.parseFloat(value));
    }

    public String getDataString(byte[] buffer) throws SQLException {
        return Float.toString(getDataFloat(buffer));
    }

    public void setInteger(byte[] buffer, int value) throws SQLException {
        setFloat(buffer, value);
    }

    public int getDataInteger(byte[] buffer) throws SQLException {
        return (int)getDataFloat(buffer);
    }

    public void setLong(byte[] buffer, long value) throws SQLException {
        setFloat(buffer, value);
    }

    public long getDataLong(byte[] buffer) throws SQLException {
        return (long)getDataFloat(buffer);
    }

    public void setFloat(byte[] buffer, float value) throws SQLException {
        intToBytes(buffer, getCustomDataPosition(), Float.floatToRawIntBits(value));
    }

    public float getDataFloat(byte[] buffer) throws SQLException {
        return Float.intBitsToFloat(intFromBytes(buffer, getCustomDataPosition()));
    }

    public void setDataDate(byte[] buffer, Date value) throws SQLException {
        throw new SQLException("Cannot convert Date to Float");
    }

    public Date getDataDate(byte[] buffer) throws SQLException {
        throw new SQLException("Cannot convert Float to Date");
    }

    public void initCustomValue(byte[] buffer) throws SQLException {
        setFloat(buffer, 0);
    }

    public Object clone() {
        return new FloatColumnInfo(this);
    }
}
