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
package ru.rd.courier.scripting.statements;

import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.DataReceiver;
import ru.rd.courier.scripting.PortionFormatterProvider;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.dataaccess.BufferedDataSource;
import ru.rd.courier.scripting.dataaccess.PortionSendEvent;
import ru.rd.courier.scripting.dataaccess.PortionSendListener;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.PooledObjectHolder;

public class TargetBuffer implements ScriptStatement {
    private final String m_dbName;
    protected final String m_formatterProviderName;
    private final String m_recLimitVarName;
    private final String m_bytesLimitVarName;
    private final ScriptStatement m_stmt;

    public TargetBuffer(
        final String dbName,
        final String formatterProviderName,
        final String recLimitVarName,
        final String bytesLimitVarName,
        final ScriptStatement stmt
    ) {
        m_dbName = dbName;
        m_formatterProviderName = formatterProviderName;
        m_recLimitVarName = recLimitVarName;
        m_bytesLimitVarName = bytesLimitVarName;
        m_stmt = stmt;
    }

    public void start(final Context ctx) throws CourierException {
        m_stmt.start(ctx);
    }

    public void finish(final ru.rd.courier.scripting.Context ctx) throws CourierException {
        m_stmt.finish(ctx);
    }

    protected void beforeSend(final Context ctx, PortionSendEvent event) throws CourierException {}
    protected void afterSend(final Context ctx, PortionSendEvent event) throws CourierException {}

    public void exec(final Context ctx) throws CourierException {
        class POH implements PooledObjectHolder {
            private final PooledObjectHolder m_po = ctx.getReceiverPooledObject(m_dbName);
            private BufferedDataSource m_bdr = null;

            public PoolObjectFactory getFactory() {
                return m_po.getFactory();
            }

            public Object getObject() {
                if (m_bdr == null) createReceiver();
                return m_bdr;
            }

            public boolean hasObject() {
                return m_bdr != null;
            }

            public void release() {
                m_po.release();
            }

            public void markStale() {
                m_po.markStale();
            }

            private void createReceiver() {
                PortionFormatterProvider fp = (PortionFormatterProvider)ctx.getObject(m_formatterProviderName);
                m_bdr = new BufferedDataSource(
                    (DataReceiver) m_po.getObject(),
                    Integer.parseInt(ctx.getVar(m_bytesLimitVarName)),
                    Integer.parseInt(ctx.getVar(m_recLimitVarName)),
                    fp.getPortionFormatter(ctx),
                    new PortionSendListener() {
                        public void before(PortionSendEvent event) throws CourierException {
                            beforeSend(ctx, event);
                        }

                        public void after(PortionSendEvent event) throws CourierException {
                            afterSend(ctx, event);
                        }
                    }
                );
            }
        }

        POH po = new POH();
        try {
            ctx.setPooledObject(m_dbName, po);
            ctx.execInnerStmt(m_stmt);
            if (po.hasObject()) ctx.addDbWarning(po.m_bdr.flush());
        } finally {
            ctx.setPooledObject(m_dbName, po.m_po);
        }
    }
}
