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
package ru.rd.courier.scripting;

import ru.rd.courier.CourierException;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.PooledObjectHolder;

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

public abstract class AbstractContext implements Context {
    private CourierLogger m_log = null;
    private ScriptStatement m_topStmt = null;
    private ScriptStatement m_curStmt = null;
    private DataLink m_curDataLink = null;
    private String m_state = c_stopped;

    private static final String c_stopped = "STOPPED";
    private static final String c_active = "ACTIVE";
    private static final String c_notCleaned = "NOT_CLEANED";

    private Context m_parent = null;
    private Map<String, ScriptExpression> m_vars = new HashMap<String, ScriptExpression>(); // variables
    private Map<String, ResultSet> m_rs = new HashMap<String, ResultSet>();   // result sets
    private Map<String, Object> m_objs = new HashMap<String, Object>(); // objects
    private String m_breakLabel = null;
    private volatile long m_aliveCounter;
    protected int m_errorCount;
    private String m_lastError;
    private String m_errorStack;
    private long m_sourceTime;
    private long m_customTime;
    protected Map<String, PooledObjectHolder> m_pooledObjects =
        new HashMap<String, PooledObjectHolder>();
    private final DateFormat m_dateFormat;

    public AbstractContext(
        final CourierLogger log, DateFormat dateFormat
    ) throws CourierException {
        if (log == null) throw new CourierException("Logger cannot be null");
        m_log = log;
        m_dateFormat = dateFormat;
    }

    protected abstract void initDataSource(String dbName) throws CourierException;
    protected abstract void initDataReceiver(String dbName) throws CourierException;
    protected String resolveAlias(String dbName) {
        return dbName;
    }
    private interface StmtProviderIteratorHandler {
        void handle(StatementProvider provider) throws CourierException;
    }

    private void iterateStmtProviders(StmtProviderIteratorHandler handler)
    throws CourierException {
        for (Object o : m_objs.values()) {
            if (o instanceof StatementProvider) {
                handler.handle((StatementProvider) o);
            }
        }
    }

    private void ensureState(String expectedState) throws CourierException {
        if (m_state != expectedState) {
            throw new CourierException(
                "Illegal state '" + m_state + "' but expected '" + expectedState + "'"
            );
        }
    }

    public void exec(final ScriptStatement stmt) throws CourierException {
        ensureState(c_stopped);
        m_state = c_active;

        try {
            m_aliveCounter = 0;
            m_errorCount = 0;
            m_lastError = null;
            m_errorStack = null;
            m_sourceTime = 0;
            m_topStmt = stmt;

            stmt.start(this);
            iterateStmtProviders(new StmtProviderIteratorHandler() {
                public void handle(StatementProvider provider) throws CourierException {
                    provider.start(AbstractContext.this);
                }
            });

            execInnerStmt(stmt);

            iterateStmtProviders(new StmtProviderIteratorHandler() {
                public void handle(StatementProvider provider) throws CourierException {
                    provider.finish(AbstractContext.this);
                }
            });
            stmt.finish(this);
        } finally {
            m_state = c_notCleaned;
        }
    }

    public String getCurrentStateDesc() {
        StringBuffer ret = new StringBuffer();
        if (m_curStmt != null) {
            ret.append("current statement: ");
            ret.append(m_curStmt.toString());
        }
        DataLink dl = getUsedLink();
        if (dl != null) {
            if (ret.length() > 0) ret.append("; ");
            ret.append("current data link: ");
            ret.append(dl.toString());
        }
        return ret.toString();
    }

    public void execInnerStmt(final ScriptStatement stmt) throws CourierException {
        if (m_state != c_active) return;
        m_aliveCounter++;
        final ScriptStatement curStmt = m_curStmt;
        try {
            m_curStmt = stmt;
            stmt.exec(this);
        } finally {
            m_curStmt = curStmt;
        }
    }

    public void execCtxStmt(final Context ctx, final ScriptStatement stmt) throws CourierException {
        if (m_state != c_active) return;
        m_aliveCounter++;
        final ScriptStatement curStmt = m_curStmt;
        try {
            m_curStmt = stmt;
            stmt.exec(ctx);
        } finally {
            m_curStmt = curStmt;
        }
    }

    public boolean isCanceled() {
        // decided to use parent only as var context
        //if (getParent() != null) return getParent().isCanceled();

        return (m_state != c_active);
    }

    public String getLoggerName() {
        return m_log.getLoggerName();
    }

    public CourierLogger getParentLogger() {
        return m_log.getParentLogger();
    }

    public CourierLogger getChild(String name) {
        return m_log.getChild(name);
    }

    public void debug(final String msg) {
        m_log.debug(msg);
    }

    public void info(final String msg) {
        m_log.info(msg);
    }

    public void warning(final String msg) {
        m_log.warning(msg);
    }

    public void warning(Throwable e) {
        m_log.warning(e);
    }

    public void warning(String msg, Throwable e) {
        m_log.warning(msg, e);
    }

    public void error(String msg) {
        m_log.error(msg);
    }

    public void error(Throwable e) {
        m_log.error(e);
    }

    public void error(String msg, Throwable e) {
        m_log.error(msg, e);
    }

    public void addError(String mes) {
        m_lastError = mes;
        m_errorCount++;
    }

    public String getErrorText() {
        return m_lastError;
    }

    public int getErrorCount() {
        return m_errorCount;
    }

    public String getErrorStack() {
        return m_errorStack;
    }

    public void setErrorStack(String errorStack) {
        this.m_errorStack = errorStack;
    }

    public void addSourceTime(final long time) {
        m_sourceTime += time;
    }

    public long getTargetTime(String dataLinkName) {
        PooledObjectHolder rh = m_pooledObjects.get(dataLinkName);
        if (rh == null || !(rh.getObject() instanceof DataReceiver)) return 0;
        return ((ReceiverTimeCounter)rh.getObject()).getTargetTime();
    }

    public long getTargetTime() {
        long ret = 0;
        for (PooledObjectHolder rh :m_pooledObjects.values()) {
            if (rh.hasObject() && rh.getObject() instanceof DataReceiver) {
                ret += ((ReceiverTimeCounter)rh.getObject()).getTargetTime();
            }
        }
        return ret;
    }

    public long getSourceTime() {
        return m_sourceTime;
    }

    public void addCustomTime(final long time) {
        m_customTime += time;
    }

    public long getCustomTime() {
        return m_customTime;
    }

    public boolean movedSinceLastCall() {
        final boolean ret = (m_aliveCounter != 0);
        m_aliveCounter = 0;
        return ret;
    }

    public void stop() {
        if (m_state != c_active) return;

        m_state = c_notCleaned;
        DataLink dl = getUsedLink();
        if (dl != null) {
            try { dl.cancel(); }
            catch (CourierException e) { m_log.error(e); }
        }
    }

    public final boolean hasVar(final String name) {
        return m_vars.containsKey(name) || (m_parent != null &&  m_parent.hasVar(name));
    }

    public final String getVar(final String name) throws CourierException {
        return getVarExpression(name).calculate(this);
    }

    public ScriptExpression getVarExpression(String name) {
        if (m_vars.containsKey(name)) {
            return m_vars.get(name);
        }
        else if (m_parent != null) return m_parent.getVarExpression(name);
        else throw new CourierException("Variable '" + name + "' does not exist");
    }

    public Date getDateVar(String name) throws CourierException {
        try {
            return m_dateFormat.parse(getVar(name));
        } catch (ParseException e) {
            throw new CourierException(e);
        }
    }

    public final void setVar(final String name, final String value) throws CourierException {
	if(name == null) throw new CourierException("Unable to store variable with <NULL> name");
        m_vars.put(
            name,
            new ru.rd.courier.scripting.expressions.string.Const(value)
        );
    }

    public Set<String> getVarKeySet() {
        return m_vars.keySet();
    }

    public void setVar(String name, ScriptExpression exp) {
        m_vars.put(name, exp);
    }

    public void setDateVar(String name, Date value) {
        setVar(name, value == null ? null : m_dateFormat.format(value));
    }

    public DateFormat getDateFormat() {
        return m_dateFormat;
    }

    public void removeVar(final String name) {
        if (m_vars.containsKey(name)) m_vars.remove(name);
    }

    public boolean hasObject(final String name) {
        return m_objs.containsKey(name) || (m_parent != null &&  m_parent.hasObject(name));
    }

    public Object getObject(final String name) throws CourierException {
        if (m_objs.containsKey(name)) return m_objs.get(name);
        else if (m_parent != null) return m_parent.getObject(name);
        else throw new CourierException("Object '" + name + "' does not exist");
    }

    public void setObject(final String name, final Object value) throws CourierException {
        m_objs.put(name, value);
    }

    public void removeObject(final String name) throws CourierException {
        if (m_objs.containsKey(name))  m_objs.remove(name);
    }

    protected DataSource innerGetDataSource(String dbName) throws CourierException {
        dbName = resolveAlias(dbName);
        PooledObjectHolder ret = m_pooledObjects.get(dbName);
        if (ret == null) {
            initDataSource(dbName);
            ret = m_pooledObjects.get(dbName);
        }
        if (ret == null) return null;
        return (DataSource)ret.getObject();
    }

    protected abstract void initPooledObject(String name) throws CourierException;

    public PooledObjectHolder getPooledObject(String name) throws CourierException {
        name = resolveAlias(name);
        PooledObjectHolder ret = m_pooledObjects.get(name);
        if (ret != null) return ret;
        initPooledObject(name);
        ret = m_pooledObjects.get(name);
        if (ret == null) throw new CourierException("Object '" + name + "' not found");
        return ret;
    }

    public PooledObjectHolder getReceiverPooledObject(String dbName) throws CourierException {
        dbName = resolveAlias(dbName);
        PooledObjectHolder ret = m_pooledObjects.get(dbName);
        if (ret != null) return ret;
        initDataReceiver(dbName);
        ret = m_pooledObjects.get(dbName);
        if (ret != null) return ret;
        throw new CourierException("Receiver '" + dbName + "' not found");
    }

    public DataSource getSource(String dbName) throws CourierException {
        return innerGetDataSource(dbName);
    }

    public DataReceiver getReceiver(String dbName) throws CourierException {
        return (DataReceiver)getReceiverPooledObject(dbName).getObject();
    }

    public final void setPooledObject(final String dbName, final PooledObjectHolder obj) {
        m_pooledObjects.put(resolveAlias(dbName), obj);
    }

    public final void removePooledObject(final String dbName) {
        PooledObjectHolder rh = m_pooledObjects.remove(resolveAlias(dbName));
        if (rh != null) rh.release();
    }

    private static final int cRequestMaxLengthInError = 3*1024;
    private static final String cTooLongWarning =
        " ... (Request has been shortened. Please turn to the log.)";

    public static ResultSet createResultSet(Context ctx, String dbName, String sql) throws CourierException {
        final DataSource ds = ctx.getSource(dbName);
        if (ds == null) {
            throw new CourierException("Data Source '" + dbName + "' not found");
        }
        ctx.addUsedLink(ds);
        ResultSet rs = null;
        try {
            final long begTime = System.currentTimeMillis();
            try {
                rs = ds.request(sql);
            } catch (Exception e) {
                throw new CourierException(
                    "Error opening '" + dbName + "' result set. Request:\n" +
                    (sql.length() < cRequestMaxLengthInError ?
                        sql : sql.substring(0, cRequestMaxLengthInError) + cTooLongWarning),
                     e
                );
            }
            ctx.addSourceTime(System.currentTimeMillis() - begTime);
        } finally {
            ctx.removeUsedLink(ds);
        }
        return rs;
    }

    public static ResultSet createResultSet(Context ctx, String dbName, String rsName, String sql) throws CourierException {
        try {
            ctx.removeResultSet(rsName);
        } catch(Exception e) {
            ctx.warning(e);
        }
        ResultSet rs = createResultSet(ctx, dbName, sql);
        ctx.addResultSet(rsName, rs);
        return rs;
    }

    public void addResultSet(String rsName, ResultSet rs) throws CourierException {
        m_rs.put(rsName, rs);
    }

    public ResultSet getResultSet(final String rsName) throws CourierException {
        if (m_rs.containsKey(rsName)) {
            return m_rs.get(rsName);
        } else {
            if (m_parent != null) {
                return m_parent.getResultSet(rsName);
            } else {
                throw new CourierException(
                    "There is no result set '" + rsName + "' in current context"
                );
            }
        }
    }

    public static void closeResultSet(Context ctx, ResultSet rs) {
        try {
            Statement stmt;
            stmt = rs.getStatement();

            try {
                final long begTime = System.currentTimeMillis();
                rs.close();
                ctx.addSourceTime(System.currentTimeMillis() - begTime);
            } catch (Exception e) {
                ctx.warning(e);
            }

            if (stmt != null) {
                final long begTime = System.currentTimeMillis();
                stmt.close();
                ctx.addSourceTime(System.currentTimeMillis() - begTime);
            }
        } catch (Exception e) {
            ctx.warning(e);
        }
    }

    public void removeResultSet(final String rsName) throws CourierException {
        final ResultSet rs = m_rs.remove(rsName);
        if (rs == null) return;
        closeResultSet(this, rs);
    }

    public DataLink getUsedLink() {
        return m_curDataLink;
    }

    public void addUsedLink(final DataLink link) throws CourierException {
        // !!! implementation for single-threaded script
        // multi-threaded implementation requires set instead of just variable
        m_curDataLink = link;
    }

    public void removeUsedLink(final DataLink link) throws CourierException {
        // !!! implementation for single-threaded script
        // multi-threaded implementation requires set instead of just variable
        m_curDataLink = null;
    }

    public void setBreak(final String label) {
        m_breakLabel = label;
    }

    public String getBreakLabel() {
        return m_breakLabel;
    }

    public void cleanUp() throws CourierException {
        ensureState(c_notCleaned);

        try {
            for (ResultSet resultSet : m_rs.values()) {
                try {
                    closeResultSet(this, resultSet);
                } catch (Exception e) {
                    m_log.warning("Context could not close statement", e);
                }
            }
            m_rs.clear();
        } catch(Exception e) {
            m_log.warning(e);
        }

        closeConnections();
    }

    public final Context getParent() {
        return m_parent;
    }

    public final void setParent(final Context ctx) {
        m_parent = ctx;
    }

    private void closeConnections() {
        try {
            //m_log.debug("closeConnections: m_pooledObjects.size=" + m_pooledObjects.size());
            DataLink dl = getUsedLink();
            Set<Object> releasedObjects = new HashSet<Object>();
            for (PooledObjectHolder rh: m_pooledObjects.values()) {
                Object dr = rh.getObject();
                try {
                    if (!releasedObjects.contains(dr)) {
                        if (dr == dl) rh.markStale();
                        rh.release();
                        releasedObjects.add(dr);
                    }
                } catch (ConcurrentModificationException e) {
                    throw e;
                } catch(Exception e) {
                    m_log.warning(
                        "Context could not close data receiver: " + dr, e
                    );
                }
            }
            m_pooledObjects.clear();
        } catch(Exception e) {
            m_log.warning(e);
        }
    }
}
