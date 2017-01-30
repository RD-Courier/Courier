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

import ru.rd.courier.FileDataTestCase;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.logging.test.NullLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 16.01.2007
 * Time: 12:59:57
 */
public class XPathAllDataSourceTest extends FileDataTestCase {
    public void testBasic() throws SQLException {
        XPathAllDataSource xs = new XPathAllDataSource(
            new NullLogger(), "//reg:RDocument",
            StringHelper.arrayToMap(new String[] {
                "reg", "Pythoness.Exchange.Registrator"
            }),
            null
        );

        for (String fn: new String[] {"XPathAllDataSourceTest1.xml", "XPathAllDataSourceTest2.xml"}) {
            ResultSet rs = xs.request(getDataFile(fn).getAbsolutePath());
            rs.getString("SystemInfo/IDString");
            rs.getString("Document");
        }
    }
}
