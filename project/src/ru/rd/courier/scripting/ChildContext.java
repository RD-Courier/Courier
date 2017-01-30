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
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ChildContext implements Context, ScriptStatement {
    protected Context m_pCtx;
    private ScriptStatement m_stmt;

    protected ChildContext(final ScriptStatement stmt) {
        m_stmt = stmt;
    }

    public String getCurrentStateDesc() {
        return m_pCtx.getCurrentStateDesc();
    }

    public void execInnerStmt(final ScriptStatement stmt) throws CourierException {
        execCtxStmt(this, stmt);
    }

    public void execCtxStmt(final Context ctx, final ScriptStatement stmt) throws CourierException {
        m_pCtx.execCtxStmt(ctx, stmt);
    }

    public boolean isCanceled() {
        return m_pCtx.isCanceled();
    }

    public void addDbWarning(List<LinkWarning> warnings) throws CourierException {
        m_pCtx.addDbWarning(warnings);
    }

    public void addError(String mes) {
        m_pCtx.addError(mes);
    }

    public long getTargetTime() {
        return m_pCtx.getTargetTime();
    }

    public void addSourceTime(final long time) {
        m_pCtx.addSourceTime(time);
    }

    public long getSourceTime() {
        return m_pCtx.getSourceTime();
    }

    public int getErrorCount() {
        return m_pCtx.getErrorCount();
    }

    public String getErrorText() {
        return m_pCtx.getErrorText();
    }

    public Date getDateVar(String name) throws CourierException {
        return m_pCtx.getDateVar(name);
    }

    public void setDateVar(String name, Date value) {
        m_pCtx.setDateVar(name, value);
    }

    public DateFormat getDateFormat() {
        return m_pCtx.getDateFormat();
    }

    public boolean hasVar(final String name) {
        return m_pCtx.hasVar(name);
    }

    public ScriptExpression getVarExpression(String name) {
        return m_pCtx.getVarExpression(name);
    }

    public String getVar(final String name) throws CourierException {
        return m_pCtx.getVar(name);
    }

    public void setVar(final String name, final String value) {
        m_pCtx.setVar(name, value);
    }

    public Set<String> getVarKeySet() {
        return m_pCtx.getVarKeySet();
    }

    public void setVar(String name, ScriptExpression exp) {
        m_pCtx.setVar(name, exp);
    }

    public void removeVar(final String name) {
        m_pCtx.removeVar(name);
    }

    public boolean hasObject(final String name) {
        return m_pCtx.hasObject(name);
    }

    public Object getObject(final String name) throws CourierException {
        return m_pCtx.getObject(name);
    }

    public void setObject(final String name, final Object value) throws CourierException {
        m_pCtx.setObject(name, value);
    }

    public void removeObject(final String name) throws CourierException {
        m_pCtx.removeObject(name);
    }

    public DataSource getSource(String dbName) throws CourierException {
        return m_pCtx.getSource(dbName);
    }

    public ResultSet getResultSet(final String rsName) throws CourierException {
        return m_pCtx.getResultSet(rsName);
    }

    public void addResultSet(String rsName, ResultSet rs) throws CourierException {
        m_pCtx.addResultSet(rsName, rs);
    }

    public void removeResultSet(final String rsName) throws CourierException {
        m_pCtx.removeResultSet(rsName);
    }

    public PooledObjectHolder getPooledObject(String name) throws CourierException {
        return m_pCtx.getPooledObject(name);
    }

    public PooledObjectHolder getReceiverPooledObject(String dbName) throws CourierException {
        return m_pCtx.getReceiverPooledObject(dbName);
    }

    public void addUsedLink(final DataLink link) throws CourierException {
        m_pCtx.addUsedLink(link);
    }

    public void removeUsedLink(final DataLink link) throws CourierException {
        m_pCtx.removeUsedLink(link);
    }

    public DataReceiver getReceiver(final String dbName) throws CourierException {
        return m_pCtx.getReceiver(dbName);
    }

    public void setPooledObject(final String dbName, final PooledObjectHolder dr) throws CourierException {
        m_pCtx.setPooledObject(dbName, dr);
    }

    public void removePooledObject(String dbName) throws CourierException {
        m_pCtx.removePooledObject(dbName);
    }

    public void setBreak(final String label) {
        m_pCtx.setBreak(label);
    }

    public String getBreakLabel() {
        return m_pCtx.getBreakLabel();
    }

    public void start(final Context ctx) throws CourierException {
        m_pCtx = ctx;
        m_stmt.start(this);
    }

    public void finish(final Context ctx) throws CourierException {
        m_stmt.finish(this);
    }

    public void exec(final Context ctx) throws CourierException {
        m_pCtx = ctx;
        m_stmt.exec(this);
    }

    public void warning(final String msg) {
        m_pCtx.warning(msg);
    }

    public void warning(Throwable e) {
        m_pCtx.warning(e);
    }

    public void warning(String msg, Throwable e) {
        m_pCtx.warning(msg, e);
    }

    public void error(String msg) {
        m_pCtx.error(msg);
    }

    public void error(Throwable e) {
        m_pCtx.error(e);
    }

    public void error(String msg, Throwable e) {
        m_pCtx.error(msg, e);
    }

    public void info(final String msg) {
        m_pCtx.info(msg);
    }

    public String getLoggerName() {
        return m_pCtx.getLoggerName();
    }

    public CourierLogger getParentLogger() {
        return m_pCtx.getParentLogger();
    }

    public CourierLogger getChild(String name) {
        return m_pCtx.getChild(name);
    }

    public void stop() {
        m_pCtx.stop();
    }

    public void debug(final String msg) {
        m_pCtx.debug(msg);
    }
}
