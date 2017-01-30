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
import ru.rd.courier.scripting.ScriptStatement;

import java.util.ArrayList;
import java.util.List;

public class Block implements ScriptStatement {
    protected List<ScriptStatement> m_stats = new ArrayList<ScriptStatement>();
    private String m_breakLabel;

    public Block(final String label, final Object[] stats) {
        m_breakLabel = label;
        if (stats == null) return;
        for (Object stat : stats) {
            m_stats.add((ScriptStatement)stat);
        }
    }

    public Block(final Object[] stats) {
        this(null, stats);
    }

    public Block(final String label, final List<ScriptStatement> stats) {
        m_breakLabel = label;
        if (stats == null) return;
        m_stats.addAll(stats);
    }

    public Block(final List<ScriptStatement> stats) {
        this(null, stats);
    }

    public void start(final Context ctx) throws CourierException {
        for (ScriptStatement stat : m_stats) {
            if (ctx.isCanceled()) break;
            stat.start(ctx);
        }
    }

    public void finish(final Context ctx) throws CourierException {
        for (ScriptStatement stat : m_stats) {
            if (ctx.isCanceled()) break;
            stat.finish(ctx);
        }
    }

    // ***************** ScriptStatement implementation ********
    public void exec(final Context ctx) throws CourierException {
        int i = 0;
        for (ScriptStatement stat : m_stats) {
            if (ctx.isCanceled()) break;
            try {
                ctx.execInnerStmt(stat);
            } catch (Exception e) {
                throw new CourierException("Error inside block statement #" + (i+1), e);
            }
            if (ctx.getBreakLabel() != null ) {
                if(ctx.getBreakLabel().equals(m_breakLabel)) ctx.setBreak(null);
                break;
            }
            i++;
        }
    }
    // **************************************************************

    public void setBreakLabel(final String label) {
        m_breakLabel = label;
    }

    public void addStatement(final ScriptStatement stat) {
        m_stats.add(stat);
    }

}
