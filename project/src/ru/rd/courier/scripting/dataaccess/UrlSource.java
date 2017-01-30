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
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.utils.StreamHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.WorkThread;
import ru.rd.utils.ExceptionCatchRunnable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 07.08.2006
 * Time: 11:21:30
 */
public class UrlSource extends TimedStringReceiver implements DataSource {
    private final CourierLogger m_logger;
    private final ObjectPoolIntf m_threadPool;
    private final String m_proxyHost;
    private final int m_proxyPort;
    private long m_timeout = 0;
    private final int m_connectTimeout;
    private final int m_readTimeout;

    public UrlSource(
        CourierLogger logger, ObjectPoolIntf threadPool,
        String proxyHost, int proxyPort,
        int connectTimeout, int readTimeout
    ) {
        m_logger = logger;
        m_threadPool = threadPool;
        m_proxyHost = proxyHost;
        m_proxyPort = proxyPort;
        m_connectTimeout = connectTimeout;
        m_readTimeout = readTimeout;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        throw new CourierException("There is no operations in URL data source");
    }

    protected List<LinkWarning> timedFlush() { return null; }

    protected void timedClose() throws CourierException {}

    public void setTimeout(int timeout) throws CourierException {
        m_timeout = timeout;
    }

    public void cancel() throws CourierException {}

    private static void loadUrlData(
        CourierLogger logger, ObjectPoolIntf threadPool,
        String urlString, String proxyHost, int proxyPort,
        long timeout, int connectTimeout, int readTimeout,
        final OutputStream os
    ) throws IOException, CourierException {
        URL url = new URL(urlString);
        URLConnection con;

        if (proxyHost != null) {
            Proxy proxy = new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress(proxyHost, proxyPort)
            );
            con = url.openConnection(proxy);
        } else {
            con = url.openConnection();
        }
        if (connectTimeout > 0) con.setConnectTimeout(connectTimeout);
        if (readTimeout > 0) con.setReadTimeout(readTimeout);

        try {
            final InputStream is = url.openStream();
            try {
                ExceptionCatchRunnable task = new ExceptionCatchRunnable() {
                    protected void safeRun() throws Exception {
                        StreamHelper.transfer(is, os, 4*1024, false);
                    }
                };
                WorkThread wt = (WorkThread)threadPool.getObject();
                try {
                    if (!wt.launchWorkAndWait(task, timeout)) {
                        throw new CourierException("Request timeout");
                    }
                    if (task.getException() != null) {
                        throw new CourierException(task.getException());
                    }
                } finally {
                    threadPool.releaseObject(wt);
                }
            } finally {
                try { is.close(); } catch (Throwable e) { logger.warning(e); }
            }
        } finally {
            try { os.close(); } catch (Throwable e) { logger.warning(e); }
        }
    }

    public ResultSet request(String query) throws CourierException {
        try {
            Properties props = new Properties();
            StringHelper.parseParams(props, query, '\'');
            final String urlString = StringHelper.stringParam(props, "url");
            final String fn = StringHelper.stringParam(props, "file");

            URL url = new URL(urlString);
            URLConnection con;

            if (m_proxyHost != null) {
                Proxy proxy = new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(m_proxyHost, m_proxyPort)
                );
                con = url.openConnection(proxy);
            } else {
                con = url.openConnection();
            }
            if (m_connectTimeout > 0) con.setConnectTimeout(m_connectTimeout);
            if (m_readTimeout > 0) con.setReadTimeout(m_readTimeout);

            final OutputStream os = new FileOutputStream(fn);
            try {
                final InputStream is = url.openStream();
                try {
                    ExceptionCatchRunnable task = new ExceptionCatchRunnable() {
                        protected void safeRun() throws Exception {
                            StreamHelper.transfer(is, os, 4*1024, false);
                        }
                    };
                    WorkThread wt = (WorkThread)m_threadPool.getObject();
                    try {
                        if (!wt.launchWorkAndWait(task, m_timeout)) {
                            throw new CourierException("Request timeout");
                        }
                        if (task.getException() != null) {
                            throw new CourierException(task.getException());
                        }
                    } finally {
                        m_threadPool.releaseObject(wt);
                    }
                } finally {
                    try { is.close(); } catch (Throwable e) { m_logger.warning(e); }
                }
            } finally {
                try { os.close(); } catch (Throwable e) { m_logger.warning(e); }
            }
            DataBufferResultSet rs = new DataBufferResultSet();
            rs.addColumn(new StringColumnInfo("file", fn.length()));
            rs.addRecord();
            rs.updateString(1, fn);
            rs.beforeFirst();
            return rs;
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }
}
