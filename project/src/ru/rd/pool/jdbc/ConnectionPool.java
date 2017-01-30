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
package ru.rd.pool.jdbc;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.ObjectPool;
import ru.rd.pool.PoolException;
import ru.rd.pool.AsynchObjectPool;

import java.sql.Connection;
import java.util.Properties;

public class ConnectionPool {
    private CourierLogger m_logger;
    private String m_desc;
    private ObjectPool m_threadPool;
    private String m_driverName;
    private ObjectPool m_pool = null;
    private ConnectionFactory m_factory = null;
    private boolean m_closed = true;

    private int m_initialCapacity;
    private int m_incCapacity;
    private int m_maxCapacity;
    private long m_allocateTimeout;
    private long m_shrinkInterval;
    private int m_shrinkCapacity;
    private long m_shrinkObjPeriod;
    private long m_checkInterval;
    private long m_checkTimeout;
    private long m_expirePeriod;

    private void init(
        final CourierLogger logger, final String desc,
        ObjectPool threadPool, final String driverName,
        ConnectionFactory factory
    ) {
        m_logger = logger;
        m_desc = desc;
        m_threadPool = threadPool;
        m_driverName = driverName;
        m_factory = factory;
        setProperties(
            0, 1, -1, 30 * 1000,
            10*1000, 1, 10 * 60 * 1000,
            15 * 60 * 1000, 10 * 1000,
            5 * 60 * 60 * 1000
        );
    }

    public ConnectionPool(
        final CourierLogger logger,
        final String desc, ObjectPool threadPool,
        final String driverName, final String url
    ) throws PoolException {
        init(
            logger, desc, threadPool, driverName,
            new ConnectionFactory(logger, driverName, url)
        );
    }

    public ConnectionPool(
        final CourierLogger logger,
        final String desc, ObjectPool threadPool,
        final String driverName, final String url,
        final String user, final String password
    ) throws PoolException {
        init(
            logger, desc, threadPool, driverName,
            new UserNameConnectionFactory(m_logger, driverName, url, user, password)
        );
    }

    public ConnectionPool(
        final CourierLogger logger,
        final String desc, ObjectPool threadPool,
        final String driverName, final String url,
        final Properties info, String progNameParamName
    ) throws PoolException {
        init(
            logger, desc, threadPool, driverName,
            new PropConnectionFactory(logger, driverName, url, info, progNameParamName)
        );
    }

    public ConnectionPool(
        final CourierLogger logger,
        final String desc, ObjectPool threadPool,
        final String driverName, final String url,
        final int initialCapacity, final int incCapacity, int maxCapacity, long allocateTimeout,
        final long shrinkInterval, final int shrinkCapacity, final long shrinkObjPeriod,
        final long checkInterval, long checkTimeout,
        long expirePeriod
    ) throws PoolException {
        this(logger, desc, threadPool, driverName, url);
        setProperties(
            initialCapacity, incCapacity, maxCapacity, allocateTimeout,
            shrinkInterval, shrinkCapacity, shrinkObjPeriod,
            checkInterval, checkTimeout,
            expirePeriod
        );
    }

    public ConnectionPool(
        final CourierLogger logger,
        final String desc, ObjectPool threadPool,
        final String driverName, final String url,
        Properties info, String progNameParamName,
        final int initialCapacity, final int incCapacity, int maxCapacity, long allocateTimeout,
        final long shrinkInterval, final int shrinkCapacity, final long shrinkObjPeriod,
        final long checkInterval, long checkTimeout,
        long expirePeriod
    ) throws PoolException {
        this(
            logger, desc, threadPool, driverName, url,
            info, progNameParamName
        );
        setProperties(
            initialCapacity, incCapacity, maxCapacity, allocateTimeout,
            shrinkInterval, shrinkCapacity, shrinkObjPeriod,
            checkInterval, checkTimeout,
            expirePeriod
        );
    }

    public ConnectionPool(
        final CourierLogger logger,
        final String desc, ObjectPool threadPool,
        final String driverName, final String url,
        final String user, final String password,
        final int initialCapacity, final int incCapacity, int maxCapacity, long allocateTimeout,
        final long shrinkInterval, final int shrinkCapacity, final long shrinkObjPeriod,
        final long checkInterval, long checkTimeout,
        long expirePeriod
    ) throws PoolException {
        this(
            logger, desc, threadPool, driverName, url,
            user, password
        );
        setProperties(
            initialCapacity, incCapacity, maxCapacity, allocateTimeout,
            shrinkInterval, shrinkCapacity, shrinkObjPeriod,
            checkInterval, checkTimeout,
            expirePeriod
        );
    }

    private void setProperties(
        final int initialCapacity, final int incCapacity, int maxCapacity, long allocateTimeout,
        final long shrinkInterval, final int shrinkCapacity, final long shrinkObjPeriod,
        final long checkInterval, long checkTimeout,
        long expirePeriod
    ) {
        m_initialCapacity = initialCapacity;
        m_incCapacity = incCapacity;
        m_maxCapacity = maxCapacity;
        m_allocateTimeout = allocateTimeout;
        m_shrinkInterval = shrinkInterval;
        m_shrinkCapacity = shrinkCapacity;
        m_shrinkObjPeriod = shrinkObjPeriod;
        m_checkInterval = checkInterval;
        m_checkTimeout = checkTimeout;
        m_expirePeriod = expirePeriod;

        if (m_pool != null) {
            // !!! to actually change parameters we need support from Object pool
            //m_pool.setProperties();
        }
    }

    public Connection getConnection() throws PoolException {
        return (Connection)getObjectPool().getObject();
    }

    public synchronized void start() throws PoolException {
        if (!m_closed) throw new IllegalStateException("Already started");
        getObjectPool().start();
        m_closed = false;
    }

    public synchronized void close() throws PoolException {
        if(m_pool != null) m_pool.close();
        m_closed = true;
    }

    private ObjectPool getObjectPool() {
        if (m_pool == null) {
            m_pool = new AsynchObjectPool(
                m_desc,
                m_logger, m_threadPool, m_factory,
                m_initialCapacity, m_incCapacity, m_maxCapacity, m_allocateTimeout,
                m_shrinkInterval, m_shrinkCapacity, m_shrinkObjPeriod,
                m_checkInterval, m_checkTimeout,
                m_expirePeriod
            );
        }
        return m_pool;
    }

    public String getDriverName() {
        return m_driverName;
    }

    public void setMaxCapacity(int maxCapacity) {
        m_maxCapacity = maxCapacity;
        if (!m_closed) m_pool.setMaxCapacity(maxCapacity);
    }

    public String toString() {
        return (
            "Connection pool '" + m_desc + "': "
            + " active = " + (!m_closed)
            + "; driver-name = " + m_driverName
            + "; initial-capacity = " + m_initialCapacity
            + "; inc-capacity = " + m_incCapacity
            + "; max-capacity = " + m_maxCapacity
            + "; check-interval = " + m_checkInterval
            + "; check-timeout = " + m_checkTimeout
            + "; expire-period = " + m_expirePeriod
            + "; allocate-timeout = " + m_allocateTimeout
            + "; shrink-interval = " + m_shrinkInterval
            + "; shrink-capacity = " + m_shrinkCapacity
            + "; shrink-obj-period = " + m_shrinkObjPeriod
            + "\n" + m_pool.toString()
        );
    }
}
