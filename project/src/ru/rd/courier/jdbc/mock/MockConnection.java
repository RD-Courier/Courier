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
package ru.rd.courier.jdbc.mock;

import ru.rd.courier.jdbc.ConnectionDrivenJdbcStatement;
import ru.rd.courier.jdbc.ConnectionSkeletonDb;
import ru.rd.courier.jdbc.JdbcDatabase;
import ru.rd.courier.utils.StringHelper;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MockConnection extends ConnectionSkeletonDb {
    private static Map s_outFiles = new HashMap();

    private String m_absPath;
    private PrintWriter m_updateOutput;
    private StringWriter m_strBuf = new StringWriter();
    private PrintWriter m_buf = new PrintWriter(m_strBuf);


    private static int s_lastConNumber = 0;

    private static class OutFileWrapper {
        public FileOutputStream m_fos;
        public int m_useCount = 0;

        public OutFileWrapper(FileOutputStream fos) {
            m_fos = fos;
        }
    }

    private static synchronized FileOutputStream getOutputStream(String path) throws FileNotFoundException {
        OutFileWrapper fw;
        fw = (OutFileWrapper)s_outFiles.get(path);
        if (fw == null) {
            fw = new OutFileWrapper(new FileOutputStream(path));
            s_outFiles.put(path, fw);
        }
        fw.m_useCount++;
        return fw.m_fos;
    }

    private static synchronized void releaseOutputStream(String path) throws SQLException {
        OutFileWrapper fw = (OutFileWrapper)s_outFiles.get(path);
        if (fw == null) {
            throw new SQLException("Output Stream '" + path + "' not found");
        } else {
            fw.m_useCount--;
            if (fw.m_useCount <= 0) {
                try {
                    try {
                        fw.m_fos.close();
                    } catch (IOException e) {
                        throw new SQLException(e.getMessage());
                    }
                } finally {
                    s_outFiles.remove(path);
                }
            }
        }
    }

    private static File extractConfFile(Properties props) {
        if(props.containsKey(MockDriver.c_fileNameParam)) {
            return new File(props.getProperty(MockDriver.c_fileNameParam));
        } else {
            return null;
        }
    }

    public MockConnection(JdbcDatabase db, Properties props) throws SQLException {
        this(db, extractConfFile(props));
        long sleepTime = StringHelper.timeParam(props, "sleep", 0);
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                m_logger.throwing("MockConnection", "MockConnection", e);
            }
        }
    }

    public MockConnection(JdbcDatabase db, File confFile) throws SQLException {
        super(++s_lastConNumber, db);
        if (confFile != null) {
            m_absPath = confFile.getAbsolutePath();
            try {
                m_updateOutput = new PrintWriter(getOutputStream(m_absPath));
            } catch (FileNotFoundException e) {
                throw new SQLException(e.getMessage());
            }
        }
    }

    public List request(ConnectionDrivenJdbcStatement stmt, String request)
    throws SQLException {
        m_buf.println(request);
        return super.request(stmt, request);
    }

    public synchronized void commit() throws SQLException {
        m_buf.flush();
        if (m_updateOutput != null) {
            m_updateOutput.print(m_strBuf.toString());
            m_updateOutput.flush();
        }
        m_strBuf.getBuffer().setLength(0);
        super.commit();
    }

    public synchronized void rollback() throws SQLException {
        m_buf.flush();
        m_strBuf.getBuffer().setLength(0);
        super.rollback();
    }

    public synchronized void close() throws SQLException {

        //System.out.println("MockConnection.close: begin");

        commit();
        try {
            super.close();
        } finally {
            try {
                if (m_updateOutput != null)
                    MockConnection.releaseOutputStream(m_absPath);
            } catch(Throwable e) {
                // !!! throw new SQLException();
            }
        }

        //System.out.println("MockConnection.close: end");

    }
}
