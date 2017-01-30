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
package ru.rd.courier.scripting.dataaccess;

import ru.rd.courier.CourierException;
import ru.rd.courier.utils.FileHelper;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;

import java.sql.ResultSet;
import java.util.Map;
import java.io.File;

/**
 * User: AStepochkin
 * Date: 21.06.2006
 * Time: 15:49:47
 */
public class FileSource extends MapDataSourceImpl {
    private final String m_charSet;
    private final String m_fieldName;

    public FileSource(String charSet, String fieldName) {
        m_charSet = charSet;
        m_fieldName = fieldName;
    }

    public ResultSet request(Map<String, String> pars) throws CourierException {
        try {
            DataBufferResultSet rs = new DataBufferResultSet();
            final String fileName = getReqParam(pars, "file");
            final String fieldName = getParam(pars, "field-name", m_fieldName);
            final String charSet = getParam(pars, "charset", m_charSet);
            File f = new File(fileName);
            final String fileText = FileHelper.fileToString(f, charSet);
            rs.addColumn(new StringColumnInfo(fieldName, fileText.length()));
            rs.addRecord();
            rs.updateString(1, fileText);
            rs.beforeFirst();
            return rs;
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }
}
