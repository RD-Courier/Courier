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

import ru.rd.courier.CourierException;
import ru.rd.courier.logging.ConsoleCourierLogger;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.AbstractContext;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.DataReceiver;
import ru.rd.courier.scripting.dataaccess.StreamStringReceiver;
import ru.rd.courier.scripting.dataaccess.jdbc.JdbcStringSource;
import ru.rd.pool.PooledObjectHolder;
import ru.rd.pool.PoolObjectFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.List;

public class TestContext extends AbstractContext {
    public TestContext(final CourierLogger logger, DateFormat dateFormat) throws CourierException {
        super(logger, dateFormat);
    }

    protected void initDataSource(final String dbName) throws CourierException {
        new com.sybase.jdbc2.jdbc.SybDriver();
        try {
            final Connection con =  DriverManager.getConnection(
                "jdbc:sybase:Tds:repserver:5000", "stockman", "warehousing"
            );
            con.setCatalog("CLEANING");
            m_pooledObjects.put(
                dbName,
                new PooledObjectHolder() {
                    DataSource m_ds = new JdbcStringSource(new ConsoleCourierLogger(null, "test"), con, "type", true);

                    public PoolObjectFactory getFactory() {
                        return null;
                    }

                    public Object getObject() {
                        return m_ds;
                    }

                    public boolean hasObject() {
                        return m_ds != null;
                    }

                    public void release() {
                        try {
                            m_ds.close();
                        } catch (CourierException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    public void markStale() {}
                });
        } catch (SQLException e) {
            throw new CourierException("", e);
        }
    }

    protected void initDataReceiver(final String dbName) throws CourierException {
        if (dbName.equals("messages")) {
            setPooledObject(
                dbName,
                new PooledObjectHolder() {
                    DataReceiver m_dr = new StreamStringReceiver(System.out, false);

                    public PoolObjectFactory getFactory() {
                        return null;
                    }

                    public Object getObject() {
                        return m_dr;
                    }

                    public boolean hasObject() {
                        return m_dr != null;
                    }

                    public void release() {
                        try {
                            m_dr.close();
                        } catch (CourierException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    public void markStale() {}
                }
            );
        }
    }

    protected void initPooledObject(String name) {}

    public void addDbWarning(List<LinkWarning> warnings) throws CourierException {
        for (LinkWarning warning : warnings) {
            System.err.println(warning.getException().getMessage());
        }
    }
}
