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
package ru.rd.courier;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.ScriptExpression;

import java.util.Map;

/**
 * User: Alexander Stepochkin
 * Date: 27.05.2005
 * Time: 16:33:48
 */
public class StdScriptProcess extends TargetScriptProcess {
    private final SourceRule m_rule;
    private String m_sProfile;

    private static SourceRule getSourceRule(Pipeline pipe, TransferRequest request) {
        SourceRule sr = pipe.getSourceProfile().getSourceRule(request.getRuleName());
        if (sr == null) {
            throw new CourierException(
                "Process start failed: there is no rule '" + request.getRuleName() + "'");
        }
        return sr;
    }

    public StdScriptProcess(
        CourierLogger logger,
        Pipeline pipe,
        Integer dbId,
        TransferRequest request,
        Integer failCount,
        ScriptExpression intervalValue,
        String pipeValue,
        Map<String, ScriptExpression> pars
    ) throws CourierException {
        super(
            logger,
            "process-log-decorator-template", Pipeline.s_PipeMessageTemplate,
            dbId, request, pipe, pipeValue,
            getSourceRule(pipe, request).getSourceIteratorProvider(),
            failCount, pars
        );

        String ruleName = request.getRuleName();
        final SourceProfile sp = pipe.getSourceProfile();
        m_sourceWaitTimeout = pipe.getSourceWaitTimeout();
        m_rule = sp.getSourceRule(ruleName);

        m_sProfile = sp.getName();
        m_ctx.setVar(c_SourceProfileVarName, m_sProfile);
        m_ctx.setVar(c_SourceRuleVarName, ruleName);
        if (m_rule.getIntervalCol() != null)
            m_ctx.setVar(c_intervalColVarName, m_rule.getIntervalCol());
        m_ctx.setVar(c_intervalTypeVarName, Character.toString(pipe.getMarkType()));
        m_ctx.setVar(c_intervalStepVarName, m_rule.getIntervalStep());
        m_ctx.setVar(c_intervalValueVarName, intervalValue);
        m_ctx.setVar(c_intervalValueCacheVarName, intervalValue);
        //m_ctx.setVar(c_RuleValueVarName, ruleValue); // obsolete
    }

    public String getSourceRuleName() {
        return m_rule.getName();
    }

    public String getSourceDesc() {
        return "sProfile = " + m_sProfile + " rule = " + getSourceRuleName();
    }
}
