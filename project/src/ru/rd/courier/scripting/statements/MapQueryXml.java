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

import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptExpression;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 11.02.2008
 * Time: 16:07:39
 */
public class MapQueryXml extends MapQueryStatement {
    private final ScriptExpression m_varName;
    private final ScriptExpression m_recordTag;

    public MapQueryXml(
        ScriptExpression dbName, Map<String, ScriptExpression> params,
        ScriptExpression varName, ScriptExpression recordTag
    ) {
        super(dbName, params);
        m_varName = varName;
        m_recordTag = recordTag;
    }

    protected void processResultSet(ResultSet rs, Context ctx) throws SQLException {
        QueryXml.rsXmlToVar(rs, ctx, m_recordTag, m_varName);
    }
}
