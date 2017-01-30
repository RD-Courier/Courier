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

import java.util.Map;
import java.util.HashMap;

/**
 * User: AStepochkin
 * Date: 01.11.2005
 * Time: 17:14:53
 */
public class MapStatementsProvider implements StatementProvider {
    private final String m_name;
    private Map<String, ScriptStatement> m_stmts = new HashMap<String, ScriptStatement>();

    public MapStatementsProvider(String name) {
        m_name = name;
    }

    public void addStatement(String name, ScriptStatement stmt) {
        m_stmts.put(name, stmt);
    }

    public void start(Context ctx) throws CourierException {
        for (ScriptStatement stmt: m_stmts.values()) {
            stmt.start(ctx);
        }
    }
    public void finish(Context ctx) throws CourierException {
        for (ScriptStatement stmt: m_stmts.values()) {
            stmt.finish(ctx);
        }
    }
    public ScriptStatement getStatement(String name) throws CourierException {
        ScriptStatement ret = m_stmts.get(name);
        if (ret == null) {
            throw new CourierException(
                "Statement '" + name + "' not found in '" + m_name + "' provider"
            );
        }
        return ret;
    }
}
