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
package ru.rd.courier.scripting.test;

import org.w3c.dom.Document;
import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.XmlStatementFactory;
import ru.rd.courier.scripting.expressions.bool.Not;
import ru.rd.courier.scripting.expressions.bool.TestVar;
import ru.rd.courier.scripting.expressions.string.Var;
import ru.rd.courier.scripting.statements.*;
import ru.rd.courier.utils.templates.SimpleTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.DateFormat;

public class TestLauncher {
    public static void main(final String[] args) {
        //System.out.println("sumTest --> " + sumTest());
        //System.out.println("breakTest --> " + breakTest());
        //System.out.println("dbTest --> " + dbTest());
        System.out.println("createStmtTest --> " + createStmtTest());
    }

    public static boolean sumTest() {
        try {
            final ru.rd.courier.scripting.Context ctx = new TestContext(null, DateFormat.getDateInstance());
            ctx.setVar("s1", "5");
            ctx.setVar("s2", "7");
            final ScriptStatement sumStmt = new Sum("s1", "s2", "tvar");
            sumStmt.exec(ctx);
            return (Integer.parseInt(ctx.getVar("tvar")) == 12);
        } catch (NumberFormatException e) {
            return false;
        } catch (CourierException e) {
            return false;
        }
    }

    public static boolean dbTest() {
        try {
            final TestContext ctx = new TestContext(null, DateFormat.getDateInstance());

            new com.sybase.jdbc2.jdbc.SybDriver();
            Connection con = null;
            java.sql.Statement sqlStmt = null;
            java.sql.Statement sqlStmt2 = null;
            try{
                con =  DriverManager.getConnection(
                    "jdbc:sybase:Tds:repserver:5000", "stockman", "warehousing"
                );
                con.setCatalog("CLEANING");
                sqlStmt = con.createStatement();
                sqlStmt.executeUpdate("DELETE FROM crrTableTo");

                ctx.setVar("fromSql", "SELECT colString FROM crrTableFrom");
                ctx.setVar("toSql", "INSERT INTO crrTableTo (colString) values ('[%colString]')");
                final ScriptStatement stmt = new CtxResultSetLoop(
                    "s_db", null, new Var("fromSql"),
                    new Block(new ScriptStatement[] {
                        new VarTemplate("toSql", "targetOp", new SimpleTemplate()),
                        new Operation("t_db", new Var("targetOp"))
                    }),
                    null
                );
                stmt.exec(ctx);
                ctx.cleanUp();

                final ResultSet fromRs = sqlStmt.executeQuery(
                    "SELECT colString FROM crrTableFrom ORDER BY colString"
                );
                sqlStmt2 = con.createStatement();
                final ResultSet toRs = sqlStmt2.executeQuery(
                    "SELECT colString FROM crrTableTo ORDER BY colString"
                );
                boolean ret = true;
                while (fromRs.next()) {
                    if (!toRs.next()) {
                        ret = false;
                        break;
                    }
                    if (!fromRs.getString("colString").equals(toRs.getString("colString"))) {
                        ret = false;
                        break;
                    }
                }
                if (toRs.next()) ret = false;
                return ret;
            } finally {
                if (sqlStmt != null) sqlStmt.close();
                if (sqlStmt2 != null) sqlStmt2.close();
                if (con != null) con.close();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean breakTest() {
        try {
            final TestContext ctx = new TestContext(null, DateFormat.getDateInstance());
            ctx.setVar("i", "0");
            final ScriptStatement stmt = new While(
                new Not(new TestVar("i", "16")),
                new Block(new ScriptStatement[] {
                    new Inc("i", 1),
                    new If(
                        new TestVar("i", "4"),
                        new Break("while"),
                        null
                    )
                }),
                "while"
            );
            try {
                stmt.exec(ctx);
            } finally {
                ctx.cleanUp();
            }
            return (ctx.getVar("i").equals("4"));
        } catch (NumberFormatException e) {
            return false;
        } catch (CourierException e) {
            return false;
        }
    }

    public static boolean createStmtTest() {

        DocumentBuilder parser;
        try {
            parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final File confDir = new File(System.getProperty("ConfDir"));
            final File confFile = new File(confDir, "StatementFactoryConf.xml");
            final Document conf = parser.parse(confFile);
            final XmlStatementFactory sf = new XmlStatementFactory(conf, null);
            final File stmtDesc = new File(confDir, "TestStatementConf.xml");
            final ScriptStatement stmt = sf.getStatement(
                parser.parse(stmtDesc).getDocumentElement(), null
            );

            TestContext ctx;
            ctx = new TestContext(null, DateFormat.getDateInstance());
            try {
                stmt.exec(ctx);
                final int cntVar = Integer.parseInt(ctx.getVar("cnt"));
                return (cntVar == 80);
            } finally {
                ctx.cleanUp();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            if (e.getCause() != null) {
                e.getCause().printStackTrace(System.out);
            }
            return false;
        }
    }
}
