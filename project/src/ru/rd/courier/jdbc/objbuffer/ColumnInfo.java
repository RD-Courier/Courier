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
package ru.rd.courier.jdbc.objbuffer;

import java.sql.Date;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 22.07.2008
 * Time: 17:17:24
 */
public abstract class ColumnInfo {
    private String m_name;
    private int m_precision = 0;
    private int m_scale = 0;

    public ColumnInfo(String name, int precision, int scale) {
        m_name = name;
        m_precision = precision;
        m_scale = scale;
    }

    public String getName() {
        return m_name;
    }

    public int getPrecision() {
        return m_precision;
    }

    public int getScale() {
        return m_scale;
    }

    public abstract int getType();
    public abstract String getTypeName();

    public String getString(Object obj) {
        if (obj == null) return null;
        return obj.toString();
    }

    public Date getDate(Object obj) throws SQLException {
        if (obj == null) return null;
        if (obj instanceof Date) return (Date)obj;
        if (obj instanceof java.util.Date) {
            return new Date(((java.util.Date)obj).getTime());
        }
        throw new SQLException("Cannot be converted to date");
    }

    public int getInt(Object obj) throws SQLException {
        if (obj == null) return 0;
        if (obj instanceof Integer) return (Integer)obj;
        if (obj instanceof String) return Integer.valueOf((String)obj);
        throw new SQLException("Cannot be converted to int");
    }

    public long getLong(Object obj) throws SQLException {
        if (obj == null) return 0;
        if (obj instanceof Long) return (Long)obj;
        if (obj instanceof Integer) return (Integer)obj;
        if (obj instanceof String) return Long.valueOf((String)obj);
        throw new SQLException("Cannot be converted to long");
    }

    public float getFloat(Object obj) throws SQLException {
        if (obj == null) return 0;
        if (obj instanceof Float) return (Float)obj;
        if (obj instanceof String) return Float.valueOf((String)obj);
        throw new SQLException("Cannot be converted to float");
    }

    public double getDouble(Object obj) throws SQLException {
        if (obj == null) return 0;
        if (obj instanceof Double) return (Double)obj;
        if (obj instanceof Float) return (Float)obj;
        if (obj instanceof String) return Double.valueOf((String)obj);
        throw new SQLException("Cannot be converted to double");
    }

    public boolean getBoolean(Object obj) throws SQLException {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean)obj;
        throw new SQLException("Cannot be converted to boolean");
    }
}
