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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import ru.rd.courier.utils.DomHelper;

public class DateColumnInfo extends ColumnInfo {
    private DateFormat m_dateFormat;

    private void init(DateFormat df) {
        if (df == null) df = DateFormat.getDateInstance();
        m_dateFormat = df;
    }

    public DateColumnInfo(ColumnInfo obj) {
        super(obj);
        init(((DateColumnInfo)obj).m_dateFormat);
    }

    public DateColumnInfo(String name, DateFormat df) {
        super(name, java.sql.Types.DATE, "DATE", 8);
        init(df);
    }

    public DateColumnInfo(Element e) {
        super(e, java.sql.Types.DATE, "DATE", 8);
        DateFormat df = null;
        if (e.hasAttribute("date-format")) {
            df = new SimpleDateFormat(e.getAttribute("date-format"));
        }
        init(df);
    }

    public void serializeString(byte[] buffer, String value) throws SQLException {
        try {
            setDataDate(buffer, new java.sql.Date(m_dateFormat.parse(value).getTime()));
        } catch (ParseException e) {
            SQLException ne = new SQLException("see cause");
            ne.initCause(e);
            throw ne;
        }
    }

    public String getDataString(byte[] buffer) throws SQLException {
        return m_dateFormat.format(new java.sql.Date(getDataLong(buffer)));
    }

    public void setInteger(byte[] buffer, int value) throws SQLException {
        throw new SQLException("Cannot convert integer to Date");
    }

    public int getDataInteger(byte[] buffer) throws SQLException {
        throw new SQLException("Cannot convert Date to integer");
    }

    public void setFloat(byte[] buffer, float value) throws SQLException {
        throw new SQLException("Cannot convert Float to Date");
    }

    public float getDataFloat(byte[] buffer) throws SQLException {
        throw new SQLException("Cannot convert Date to Float");
    }

    public void setLong(byte[] buffer, long value) throws SQLException {
        longToBytes(buffer, getCustomDataPosition(), value);
    }

    public long getDataLong(byte[] buffer) throws SQLException {
        return longFromBytes(buffer, getCustomDataPosition());
    }

    public void setDataDate(byte[] buffer, Date value) throws SQLException {
        setLong(buffer, value.getTime());
    }

    public Date getDataDate(byte[] buffer) throws SQLException {
        return new Date(getDataLong(buffer));
    }

    public void initCustomValue(byte[] buffer) throws SQLException {
        setLong(buffer, 0);
    }

    public Object clone() {
        return new DateColumnInfo(this);
    }
}
