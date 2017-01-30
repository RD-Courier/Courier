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

import junit.framework.TestCase;
import ru.rd.courier.jdbc.databuffer.DataBuffer;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;

import java.sql.SQLException;

public class DataBufferTest extends TestCase {
    public DataBufferTest(String name) {
        super(name);
    }

    public void testNullableField() throws SQLException {
        String[] values = {"rec-1-col-1", null, "rec-1-col-3"};
        DataBuffer db = new DataBuffer();
        db.addColumn(new StringColumnInfo("string-coloumn-1", false, 364));
        db.addColumn(new StringColumnInfo("string-coloumn-2", true, 1000));
        db.addColumn(new StringColumnInfo("string-coloumn-3", true, 777));
        db.addRecord();
        for (int i = 1; i <= values.length; i++) {
            if (values[i - 1] == null) {
                db.updateNull(i);
            } else {
                db.updateString(i, values[i - 1]);
            }
        }
        db.beforeFirst();
        assertTrue(db.next());
        for (int i = 1; i <= values.length; i++) {
            String value = db.getString(i);
            assertEquals(values[i - 1] == null, db.wasNull());
            assertEquals(values[i - 1], value);
        }
    }
}
