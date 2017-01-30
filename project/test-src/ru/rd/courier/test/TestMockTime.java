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
package ru.rd.courier.test;

import ru.rd.courier.jdbc.mock.MockDatabase;
import ru.rd.courier.jdbc.mock.MockDriver;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class TestMockTime {
    public static void main(String[] args) throws SQLException {
        Connection con = null;
        try {
            Properties props = new Properties();
            props.setProperty(MockDriver.c_fileNameParam, "./mockConnectionOutput.txt");
            con = MockDatabase.getGlobalDatabase().getConnection(props);
            Statement stmt = con.createStatement();

            for (int i = 0; i < 100; i++) {
                long begTime = System.currentTimeMillis();
                ResultSet rs = stmt.executeQuery("S 500 4 col");
                System.out.println("time --> " + (System.currentTimeMillis() - begTime));
            }

            /*
            ResultSetMetaData md = rs.getMetaData();
            int i = 1;
            while(rs.next()) {
                System.out.print(i + " -->");
                for(int c = 1; c <= md.getColumnCount(); c++) {
                    System.out.print(" " + rs.getDataString(c));
                }
                System.out.println();
                i++;
            }
            */
            //stmt.close();
        } finally{
            if(con != null) con.close();
        }
    }
}
