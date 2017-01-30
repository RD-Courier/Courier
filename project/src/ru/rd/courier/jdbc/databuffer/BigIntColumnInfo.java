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

public class BigIntColumnInfo extends ColumnInfo {
    public boolean m_autoIncrement;
    private int m_lastValue = 0;

    public BigIntColumnInfo(BigIntColumnInfo obj) {
        super(obj);
        init(obj.m_autoIncrement);
    }

    public BigIntColumnInfo(String name, boolean autoIncrement) {
        super(name, java.sql.Types.BIGINT, "BIGINT", 8);
        init(autoIncrement);
    }

    public BigIntColumnInfo(Element e) {
        super(e, java.sql.Types.BIGINT, "BIGINT", 8);
        init(e.hasAttribute("auto-increment") && e.getAttribute("auto-increment").equals("yes"));
    }

    private void init(boolean autoIncrement) {
        m_autoIncrement = autoIncrement;
        m_lastValue = 0;
    }

    public void serializeString(byte[] buffer, String value) throws SQLException {
        setLong(buffer, Long.parseLong(value));
    }

    public String getDataString(byte[] buffer) throws SQLException {
        return Long.toString(getDataLong(buffer));
    }

    public void setInteger(byte[] buffer, int value) throws SQLException {
        setLong(buffer, value);
    }

    public int getDataInteger(byte[] buffer) throws SQLException {
        return (int)getDataLong(buffer);
    }

    public void setLong(byte[] buffer, long value) throws SQLException {
        longToBytes(buffer, getCustomDataPosition(), value);
    }

    public long getDataLong(byte[] buffer) throws SQLException {
        return longFromBytes(buffer, getCustomDataPosition());
    }

    public void setFloat(byte[] buffer, float value) throws SQLException {
        setLong(buffer, (long)value);
    }

    public float getDataFloat(byte[] buffer) throws SQLException {
        return getDataLong(buffer);
    }

    public void setDataDate(byte[] buffer, Date value) throws SQLException {
        throw new SQLException("Cannot convert Date to Long");
    }

    public Date getDataDate(byte[] buffer) throws SQLException {
        throw new SQLException("Cannot convert Long to Date");
    }

    public void initCustomValue(byte[] buffer) throws SQLException {
        setLong(buffer, 0);
    }

    public Object clone() {
        return new BigIntColumnInfo(this);
    }
}
