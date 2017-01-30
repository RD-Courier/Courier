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
package ru.rd.courier.scripting.dataaccess.streamed;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import ru.rd.courier.jdbc.objbuffer.*;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 22.07.2008
 * Time: 19:22:46
 */
public class DbfParser implements StreamParser {
    private String m_encoding;
    private boolean m_trim;

    public DbfParser(String encoding, boolean trim) {
        m_encoding = encoding;
        m_trim = trim;
    }

    public void parseProperties(StringSimpleParser p) {
        Properties props = p.getProperties(null, '\'', "|");
        m_encoding = StringHelper.stringParam(props, "encoding", m_encoding);
        m_trim = !StringHelper.boolParam(props, "cancel-trim", !m_trim);
    }

    public ResultSet parse(InputStream is) throws IOException {
        return new DbfResultSet(is, m_encoding, m_trim);
    }

    public void cancel() {}

    private static class DbfResultSet extends ObjectsResultSet {
        private InputStream is;
        private DBFReader m_dbf;
        private boolean m_trim;

        private static ColumnInfo[] getColumns(DBFReader dbf) throws DBFException {
            ColumnInfo[] cols = new ColumnInfo[dbf.getFieldCount()];
            for (int i = 0; i < cols.length; i++) {
                DBFField f = dbf.getField(i);
                ColumnInfo col;
                switch(f.getDataType()) {
                    case DBFField.FIELD_TYPE_C:
                        col = new StringInfo(f.getName(), f.getFieldLength());
                        break;
                    case DBFField.FIELD_TYPE_D:
                        col = new DateInfo(f.getName());
                        break;
                    case DBFField.FIELD_TYPE_F:
                        col = new FloatInfo(f.getName(), f.getFieldLength(), f.getDecimalCount());
                        break;
                    case DBFField.FIELD_TYPE_L:
                        col = new BooleanInfo(f.getName());
                        break;
                    case DBFField.FIELD_TYPE_N:
                        col = new NumericInfo(f.getName(), f.getFieldLength(), f.getDecimalCount());
                        break;
                    default:
                        throw new RuntimeException("Unknown field type '" + f.getDataType() + "'");
                }
                cols[i] = col;
            }
            return cols;
        }

        private static DBFReader createReader(InputStream data, String encoding) throws DBFException {
            DBFReader dbfr = new DBFReader(data);
            if (encoding != null) {
                dbfr.setCharactersetName(encoding);
            }
            return dbfr;
        }

        public DbfResultSet(InputStream data, String encoding, boolean trim) throws DBFException {
            this(createReader(data, encoding), trim);
            is = data;
        }

        public DbfResultSet(DBFReader dbf, boolean trim) throws DBFException {
            super(null, getColumns(dbf));
            m_dbf = dbf;
            m_trim = trim;
        }

        protected boolean getRecord() throws SQLException {
            try {
                Object[] rec = m_dbf.nextRecord();
                if (rec == null) return false;
                if (m_trim) {
                    for (int i = 0; i < rec.length; i++) {
                        Object obj = rec[i];
                        if (obj instanceof String) {
                            rec[i] = ((String)obj).trim();
                        }
                    }
                }
                setData(rec);
                return true;
            } catch (DBFException e) {
                throw new RuntimeException(e);
            }
        }

        protected int skipRecords(int count) throws SQLException {
            try{
                int i;
                for (i = 0; i < count; i++) {
                    if(m_dbf.nextRecord() == null) break;
                }
                return i;
            } catch (DBFException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws SQLException {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            super.close();
        }
    }
}
