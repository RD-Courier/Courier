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
import ru.rd.courier.TransferProcessResult;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.StringContext;
import ru.rd.pool.PooledObjectHolder;

import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface Context extends StringContext, CourierLogger, TransferProcessResult {
    String getCurrentStateDesc();
    void execInnerStmt(ScriptStatement stmt) throws CourierException;
    void execCtxStmt(Context ctx, ScriptStatement stmt) throws CourierException;
    boolean isCanceled();

    void addDbWarning(List<LinkWarning> warnings) throws CourierException;
    void addError(String mes);

    long getTargetTime();
    void addSourceTime(long time);
    long getSourceTime();

    Set<String> getVarKeySet();
    ScriptExpression getVarExpression(String name);
    void setVar(String name, ScriptExpression exp);
    Date getDateVar(String name) throws CourierException;
    void setDateVar(String name, Date value);
    DateFormat getDateFormat();

    boolean hasObject(String name);
    Object getObject(String name) throws CourierException;
    void setObject(String name, Object value) throws CourierException;
    void removeObject(String name) throws CourierException;

    PooledObjectHolder getPooledObject(String name) throws CourierException;
    PooledObjectHolder getReceiverPooledObject(String dbName) throws CourierException;    
    DataReceiver getReceiver(String dbName) throws CourierException;
    void setPooledObject(String name, PooledObjectHolder dr) throws CourierException;
    void removePooledObject(final String dbName) throws CourierException;

    void addUsedLink(DataLink link) throws CourierException;
    void removeUsedLink(DataLink link) throws CourierException;

    DataSource getSource(String dbName) throws CourierException;
    ResultSet getResultSet(String rsName) throws CourierException;
    void addResultSet(String rsName, ResultSet rs) throws CourierException;
    void removeResultSet(String rsName) throws CourierException;

    void setBreak(String label);
    String getBreakLabel();
}
