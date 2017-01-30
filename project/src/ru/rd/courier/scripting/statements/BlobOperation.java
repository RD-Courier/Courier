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

import ru.rd.courier.scripting.*;
import ru.rd.courier.scripting.dataaccess.jdbc.BlobData;
import ru.rd.courier.scripting.expressions.string.Const;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.CourierException;
import org.w3c.dom.Node;

import java.util.List;
import java.sql.PreparedStatement;

/**
 * User: AStepochkin
 * Date: 06.03.2006
 * Time: 14:38:26
 */

public class BlobOperation implements ScriptStatement {
    private final ScriptExpression m_dbName;
    private final ScriptExpression m_op;
    private final ScriptExpression[] m_blobs;

    public BlobOperation(
        final ScriptExpression dbName, final ScriptExpression op,
        ScriptExpression[] blobs
    ) {
        m_dbName = dbName;
        m_op = op;
        m_blobs = blobs;
    }

    public BlobOperation(
        final String dbName, final ScriptExpression op,
        ScriptExpression[] blobs
    ) {
        this(new Const(dbName), op, blobs);
    }

    public void start(final Context ctx) throws CourierException {}
    public void finish(final Context ctx) throws CourierException {}

    public void exec(final Context ctx) throws CourierException {
        final DataReceiver dr = ctx.getReceiver(m_dbName.calculate(ctx));
        ctx.addUsedLink(dr);
        try {
            final String op = m_op.calculate(ctx);
            final String[] blobs = new String[m_blobs.length];
            for (int i = 0; i < m_blobs.length; i++) {
                blobs[i] = m_blobs[i].calculate(ctx);
            }
            if (ctx.isCanceled()) return;
            
            List<LinkWarning> ws;
            try {
                ws = dr.process(new BlobData(op, blobs) );
            } catch (Exception e) {
                throw new CourierException(
                    "Error executing on receiver '" + m_dbName + "' operation:\n" + op, e
                );
            }
            ctx.addDbWarning(ws);
        } finally {
            ctx.removeUsedLink(dr);
        }
    }
}