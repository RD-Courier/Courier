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
package ru.rd.courier.scripting.dataaccess.jdbc;

import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.pool.ReleaseAwareObject;

import java.io.StringReader;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class JdbcStringReceiver extends JdbcBaseImplementation implements ReleaseAwareObject {
    private String m_safeCommitSql;
    private String m_safeRollbackSql;
    protected boolean m_recreateStmt;
    protected Statement m_stmt = null;
    protected int m_failedUseCount = 0;
    protected int m_errorsCount = 0;

    public JdbcStringReceiver(
        CourierLogger logger,
        String type, final Connection con, final boolean recreateStmt,
        final boolean autoCommit
    ) throws CourierException {
        super(logger, type, con, autoCommit);
        m_recreateStmt = recreateStmt;
    }

    public void setSafeCommitSql(String sql) {
        m_safeCommitSql = sql;
    }

    public void setSafeRollbackSql(String sql) {
        m_safeRollbackSql = sql;
    }

    private List<LinkWarning> extractResults(Statement stmt, List<LinkWarning> res) {
        int resultNumber = 1;
        while (true) {
            try {
                if (!stmt.getMoreResults() && (stmt.getUpdateCount() == -1)) break;
            } catch(SQLException e) {
                m_errorsCount++;
                if (res == null) res = new LinkedList<LinkWarning>();
                res.add(new LinkWarning(resultNumber, e, System.currentTimeMillis()));
            }
            resultNumber++;
        }
        return res;
    }

    public synchronized List<LinkWarning> timedProcessObject(final Object operation)
    throws CourierException {
        setCancelled(false);
        try {
            Throwable err = null;
            boolean wasError = false;
            List<LinkWarning> res = null;
            try {
                if (operation instanceof String) {
                    if (m_stmt == null) {
                        m_stmt = m_con.createStatement();
                        if (m_timeout > 0) m_stmt.setQueryTimeout(m_timeout);
                        if (isCancelled()) return null;
                    }
                    m_stmt.execute((String)operation);
                } else {
                    closeStatement();
                    BlobData data = (BlobData)operation;
                    PreparedStatement stmt = m_con.prepareStatement(data.getSql());
                    m_stmt = stmt;
                    if (m_timeout > 0) stmt.setQueryTimeout(m_timeout);
                    if (isCancelled()) return null;
                    ParameterMetaData md = stmt.getParameterMetaData();
                    for (int i = 0; i < md.getParameterCount(); i++) {
                        final String paramData = data.getBlobs()[i];
                        stmt.setCharacterStream(
                            i+1, new StringReader(paramData), paramData.length()
                        );
                    }
                    if (isCancelled()) return null;
                    stmt.execute();
                }

                SQLWarning warn = m_stmt.getWarnings();
                while (warn != null) {
                    if (res == null) res = new LinkedList<LinkWarning>();
                    res.add(new LinkWarning(-1, warn, System.currentTimeMillis()));
                    //m_logger.warning(warn);
                    warn = warn.getNextWarning();
                }

                int errCount = (res == null) ? 0 : res.size();
                res = extractResults(m_stmt, res);
                if (res != null && res.size() > errCount) {
                    wasError = true;
                }
            } catch(Throwable e) {
                err = e;
            }

            if (wasError || err != null) {
                m_errorsCount++;
                safeRollback();
                if (err != null) throw new CourierException(err);
            } else {
                safeCommit();
            }
            return res;
        } finally {
            if (m_recreateStmt || m_stmt instanceof PreparedStatement) {
                try { closeStatement(); }
                catch (Exception e) { m_logger.warning(e); }
            }
        }
    }

    private void safeCommit() {
        if (m_autoCommit) return;
        if (m_safeCommitSql == null) {
            try { m_con.commit(); }
            catch (Exception e) { m_logger.warning(e); }
        } else {
            tranExec(m_safeCommitSql);
        }
    }

    private void safeRollback() {
        if (m_autoCommit) return;
        if (m_safeRollbackSql == null) {
            try { m_con.rollback(); }
            catch (Exception e) { m_logger.warning(e); }
        } else {
            tranExec(m_safeRollbackSql);
        }
    }

    private void tranExec(String sql) {
        try {
            if (m_stmt != null) {
                m_stmt.execute(sql);
            }
        } catch (Exception e) {
            m_logger.warning(e);
        }
    }

    private void closeStatement() {
        if (m_stmt == null) return;
        try {
            m_stmt.close();
        } catch(SQLException e) {
            m_logger.warning(e);
        } finally {
            m_stmt = null;
        }
    }

    public void timedClose() {
        closeStatement();

        if (m_con != null) {
            try {
                m_con.close();
            } catch (Exception e) {
                m_logger.warning(e);
            } finally {
                m_con = null;
            }
        }
    }

    public void release() {
        if (m_errorsCount > 0) {
            m_failedUseCount++;
            m_errorsCount = 0;
        }
    }

    public int getFailedUseCount() {
        return m_failedUseCount;
    }
}
