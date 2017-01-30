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
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.dataaccess.PortionSendEvent;
import ru.rd.courier.CourierException;
import ru.rd.courier.TransferProcess;

import java.util.Map;
import java.util.HashMap;

/**
 * User: AStepochkin
 * Date: 07.10.2005
 * Time: 14:14:27
 */
public class TargetBufferEx extends TargetBuffer {
    private final String m_portionVarName;
    private final String m_portionSizeVarName;
    private final String m_recordsCountVarName;

    public TargetBufferEx(
        String dbName, String formatterProviderName,
        String recLimitVarName, String bytesLimitVarName,
        String portionVarName, String portionSizeVarName,
        ScriptStatement stmt
    ) {
        super(
            dbName, formatterProviderName, recLimitVarName, bytesLimitVarName,
            stmt
        );
        m_portionVarName = portionVarName;
        m_portionSizeVarName = portionSizeVarName;
        m_recordsCountVarName = TransferProcess.c_recordCountVarName;
    }

    protected void beforeSend(final Context ctx, PortionSendEvent event) throws CourierException {
        ctx.setVar(
            m_portionSizeVarName,
            Integer.toString(event.getPortionSize())
        );

        PortionFormatterProvider fp = (PortionFormatterProvider)ctx.getObject(m_formatterProviderName);

        String portion =
              fp.getBeforePortion().calculate(ctx) 
            + event.getPortion()
            + fp.getAfterPortion().calculate(ctx);

        ctx.setVar(m_portionVarName, portion);

        fp.getBeforePortionHandler().exec(ctx);
        event.setPortion(ctx.getVar(m_portionVarName));
    }

    protected void afterSend(
        final Context ctx, PortionSendEvent event
    ) throws CourierException {
        if (m_recordsCountVarName != null) {
            int recordsCount = Integer.parseInt(ctx.getVar(m_recordsCountVarName));
            ctx.setVar(
                m_recordsCountVarName,
                Integer.toString(recordsCount + event.getPortionSize())
            );
        }

        if (ctx.hasVar(TransferProcess.c_intervalValueCacheVarName)) {
            ctx.setVar(
                TransferProcess.c_intervalValueVarName,
                ctx.getVar(TransferProcess.c_intervalValueCacheVarName)
            );
        }

        PortionFormatterProvider fp = (PortionFormatterProvider)ctx.getObject(m_formatterProviderName);
        fp.getAfterPortionHandler().exec(ctx);
    }

}
