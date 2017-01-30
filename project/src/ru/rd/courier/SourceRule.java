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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.Context;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.StatementProvider;
import ru.rd.courier.scripting.XmlStatementFactory;
import ru.rd.courier.scripting.statements.IncRecordsCount;
import ru.rd.courier.utils.DomHelper;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SourceRule {
    public static final String c_targetProfileObjectName = "target-profile";

    public static String c_FreshType = "fresh";
    public static String c_AllType = "all";
    public static String c_GapType = "gap";

    private CourierLogger m_logger;
    private SourceProfile m_profile;
    private String m_name;
    private String m_desc;
    private String m_type;
    private Integer m_ignoreErrorCount;
    private String m_intervalCol;
    private String m_intervalStep;
    private Map<String, ScriptStatement> m_statements = new HashMap<String, ScriptStatement>();
    private final String m_targetProfileObjectName;
    private final Set<String> m_sourceLinks;
    private final Set<String> m_targetLinks;

    private static Map<String, IntervalComparator> m_intervalComps = new HashMap<String, IntervalComparator>();

    private interface IntervalComparator {
        boolean compare(
            Context ctx, String curVarName, String newVarName) throws CourierException;
    }

    static {
        m_intervalComps.put("i", new IntervalComparator() {
            public boolean compare(Context ctx, String curVarName, String newVarName) throws CourierException {
                String curValue = ctx.getVar(curVarName);
                if (curValue.length() == 0) return true;
                return (Long.parseLong(ctx.getVar(newVarName)) > Long.parseLong(curValue));
            }
        });

        m_intervalComps.put("b", new IntervalComparator() {
            public boolean compare(Context ctx, String curVarName, String newVarName) throws CourierException {
                String curValue = ctx.getVar(curVarName);
                if (curValue.length() == 0) return true;
                String newValue = ctx.getVar(newVarName);
                BigInteger bnewValue = new BigInteger(newValue, 16);
                return bnewValue.compareTo(new BigInteger(curValue, 16)) > 0;
            }
        });

        m_intervalComps.put("d", new IntervalComparator() {
            public boolean compare(Context ctx, String curVarName, String newVarName) throws CourierException {
                if (ctx.getVar(curVarName).equals("")) return true;
                return ctx.getDateVar(newVarName).after(ctx.getDateVar(curVarName));
            }
        });
    }

    private static class TargetOperationsWrapper implements ScriptStatement {
        private final ScriptStatement m_stmt;
        private IntervalComparator m_ic;

        public TargetOperationsWrapper(ScriptStatement stmt) {
            m_stmt = stmt;
        }

        public void start(Context ctx) throws CourierException {
            m_stmt.start(ctx);
            m_ic  = m_intervalComps.get(ctx.getVar(TransferProcess.c_intervalTypeVarName));
        }

        public void finish(Context ctx) throws CourierException {
            m_stmt.finish(ctx);
        }

        public void exec(Context ctx) throws CourierException {
            if (
                (m_ic != null)
                //&& ctx.hasVar(TransferProcess.c_intervalValueCacheVarName)
            ) {
                String colName = ctx.getVar(TransferProcess.c_intervalColVarName);
                if (
                    m_ic.compare(
                        ctx, TransferProcess.c_intervalValueCacheVarName, colName
                    )
                ) {
                    ctx.setVar(
                        TransferProcess.c_intervalValueCacheVarName, ctx.getVar(colName)
                    );
                }
            }

            try {
                ctx.execInnerStmt(m_stmt);
            } catch (Exception e) {
                if (!ctx.hasObject(TransferProcess.c_IgnoreErrorNumberObjectName)) {
                    throw new CourierException(e);
                }

                Integer ignoreErrorNumber = (Integer)ctx.getObject(
                    TransferProcess.c_IgnoreErrorNumberObjectName
                );
                if (ignoreErrorNumber <= 0) {
                    throw new CourierException(e);
                }

                Integer failCountObj = (Integer)ctx.getObject(
                    TransferProcess.c_FailCountObjectName
                );
                int failCount;
                if (failCountObj == null) failCount = 1;
                else failCount = failCountObj + 1;

                if (failCount < ignoreErrorNumber) {
                    ctx.setObject(TransferProcess.c_FailCountObjectName, failCount);
                    throw new CourierException(
                        //"FailCount (" + failCount + ") < IgnoreErrorCount (" + ignoreErrorNumber + ")",
                        e
                    );
                }


                ctx.addError(e.getMessage());
                ctx.warning(
                    "Error is ignored: "
                    + "FailCount (" + failCount + ") >= IgnoreErrorCount (" + ignoreErrorNumber + ")",
                    e
                );
                ctx.setObject(TransferProcess.c_FailCountObjectName, 0);

                int ignoreErrorCount;
                if (ctx.hasVar(TransferProcess.c_IgnoreErrorCountObjectName)) {
                    ignoreErrorCount = (Integer) ctx.getObject(TransferProcess.c_IgnoreErrorCountObjectName);
                } else {
                    ignoreErrorCount = 0;
                }

                ignoreErrorCount++;
                ctx.setObject(TransferProcess.c_IgnoreErrorCountObjectName, ignoreErrorCount);

                if (ctx.hasVar(TransferProcess.c_intervalValueCacheVarName)) {
                    ctx.setVar(
                        TransferProcess.c_intervalValueVarName,
                        ctx.getVar(TransferProcess.c_intervalValueCacheVarName)
                    );
                }
            }
        }
    }

    private final XmlStatementFactory.CustomTagProcessor m_tagProc =
    new XmlStatementFactory.CustomTagProcessor() {
        public ScriptStatement process(
            final XmlStatementFactory sf, final Element n
        ) throws CourierException {
            ScriptStatement ret = null;
            if (n.getNodeName().equals("target-operations-wrapper")) {
                ret = new TargetOperationsWrapper(
                    sf.getStatement(DomHelper.getFirstElement(n), this)
                );
            } else if (n.getNodeName().equals("inc-receiver-records")) {
                ret = new IncRecordsCount(n.getAttribute("receiver-name"));
            }
            return ret;
        }
    };

    private void initStatement(
        String name, Node stmtsConf
    ) throws CourierException {
        name = m_profile.getApplication().getScriptParam(name);
        m_statements.put(name, m_profile.getStmtFactory().getStatement(
            DomHelper.getChild(stmtsConf, name).getFirstChild(), m_tagProc
        ));
    }

    public SourceRule(
        final SourceProfile profile, final CourierLogger logger, final Element conf
    ) throws CourierException {
        m_logger = logger;
        m_profile = profile;
        setName(DomHelper.getNodeAttr(conf, "name"));
        m_desc = DomHelper.getNodeValue(DomHelper.getChild(conf, "description", false));
        m_type = DomHelper.getNodeAttr(conf, "type");
        m_intervalStep = DomHelper.getNodeAttr(conf, "interval-step");
        m_intervalCol = DomHelper.getNodeAttr(conf, "interval-column", false);
        Node stmtsConf = DomHelper.getChild(conf, "statements");
        for (String stmtName: new String[]{
            TargetScriptProcess.c_SourcePluginMainName,
            TargetScriptProcess.c_SourcePluginBeforeName,
            TargetScriptProcess.c_SourcePluginAfterName,
            TargetScriptProcess.c_SourcePluginFinallyName
        }) {
            initStatement(stmtName, stmtsConf);
        }
        m_targetProfileObjectName = m_profile.getApplication().getScriptParam(
            "target-profile-object-name"
        );

        final String ignoreErrorAttr = "ignore-error-number";
        if (conf.hasAttribute(ignoreErrorAttr)) {
            m_ignoreErrorCount = DomHelper.getIntNodeAttr(conf, ignoreErrorAttr);
        } else {
            if (m_type.equals(c_FreshType)) {
                m_ignoreErrorCount = 2;
            } else if (m_type.equals(c_AllType)) {
                m_ignoreErrorCount = 1;
            } else if (m_type.equals(c_GapType)) {
                m_ignoreErrorCount = -1;
            }
        }


        m_sourceLinks = new HashSet<String>();
        m_targetLinks = new HashSet<String>();
        DomHelper.initDbLinks(
            DomHelper.getChild(conf, "db-links", true), m_sourceLinks, m_targetLinks
        );
    }

    public StatementProvider getSourceIteratorProvider() {
        return new StatementProvider() {
            public void start(Context ctx) throws CourierException {
                for (ScriptStatement stmt: m_statements.values()) stmt.start(ctx);
            }

            public ScriptStatement getStatement(String name) {
                return m_statements.get(name);
            }

            public void finish(Context ctx) throws CourierException {
                for (ScriptStatement stmt: m_statements.values()) stmt.finish(ctx);
            }
        };
    }

    public void register(final String pipeName) throws CourierException {
        m_profile.getApplication().getSystemDb().registerSourceRule(
            pipeName, getName(), getDesc(), getType()
        );
    }

    public SourceProfile getProfile() {
        return m_profile;
    }

    public String getName() {
        return m_name;
    }

    public String getDesc() {
        return m_desc;
    }

    public void setName(final String name) {
        m_name = name;
    }

    public String getType() {
        return m_type;
    }

    public Integer getIgnoreErrorCount() {
        return m_ignoreErrorCount;
    }

    public String getIntervalCol() {
        return m_intervalCol;
    }

    public String getIntervalStep() {
        return m_intervalStep;
    }

    public boolean isMainType() {
        return (m_type.equals(c_FreshType) || m_type.equals(c_AllType));
    }

    public String getTargetProfileObjectName() {
        return m_targetProfileObjectName;
    }

    public Set<String> getSourceLinks() {
        return m_sourceLinks;
    }

    public Set<String> getTargetLinks() {
        return m_targetLinks;
    }
}
