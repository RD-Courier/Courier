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

import org.apache.commons.lang.exception.ExceptionUtils;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.LeftTemplateLoggerDecorator;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.scripting.*;
import ru.rd.courier.scripting.dataaccess.LoggingDataReceiver;
import ru.rd.courier.scripting.dataaccess.LoggingDataSource;
import ru.rd.courier.scripting.statements.ObjectStatementCaller;
import ru.rd.courier.utils.ErrorHelper;
import ru.rd.courier.utils.templates.HashMapStringContext;
import ru.rd.courier.utils.templates.SimpleTemplate;
import ru.rd.pool.PoolException;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.PooledObjectHolder;
import ru.rd.thread.AsyncBuffer;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class TargetScriptProcess implements TransferProcess, TransferProcessResult {
    private boolean m_active = true;
    protected final CourierLogger m_logger;
    protected final DataLogger m_dataLogger;
    private final Integer m_dbId;
    private final SystemDb m_sysDb;

    protected final ScriptContext m_ctx;
    private ScriptStatement m_topStmt;
    private final AsyncBuffer<ProcessWarnings> m_warnBuffer;
    private int m_recordCount;
    private long m_targetTime = 0;
    private long m_startTime;
    private long m_totalTime;

    private final FullPipeline m_pipe;
    protected String m_pipeDesc;

    private final String m_tProfile;
    protected final String m_defaultTargetDbName;
    private final int m_targetWaitTimeout;

    private final String m_defaultSourceDbName;
    protected int m_sourceWaitTimeout = 0;

    public static final String c_ScriptsObjectName = "Scripts";
    public static final String c_pipesScriptName = "pipes";
    public static final String c_pipeScriptName = "pipe";
    public static final String c_processScriptName = "process";
    public static final String c_stdScriptName = "std";

    public static final String c_SourcePluginNamePrefix = "source-";
    public static final String c_SourcePluginNamePostfix = "-stmt-name";

    private static String getPluginName(String shortName) {
        return c_SourcePluginNamePrefix + shortName + c_SourcePluginNamePostfix;
    }

    public static final String c_SourcePluginMainName = getPluginName("main");
    public static final String c_SourcePluginBeforeName = getPluginName("before");
    public static final String c_SourcePluginAfterName = getPluginName("after");
    public static final String c_SourcePluginFinallyName = getPluginName("finally");

    protected class ScriptContext extends AbstractContext {
        public ScriptContext(final CourierLogger logger, DateFormat dateFormat) throws CourierException {
            super(logger, dateFormat);
        }

        public void addDbWarning(List<LinkWarning> warnings) throws CourierException {
            if ((warnings == null) || (warnings.size() == 0)) return;

            addError(
                "Process " + getId() + " operation batch raised " + warnings.size() + " errors." +
                " The first one: " + warnings.get(0).getException().getMessage()
            );

            final int severeErrorsCount = 10;
            StringBuffer sbErr = new StringBuffer();
            StringBuffer sbWarn = new StringBuffer();
            String stackTrace = null;
            int i = 0;
            for (LinkWarning lw: warnings) {
                if (i == 0) {
                    stackTrace = ErrorHelper.stackTraceToString(
                        lw.getException().getStackTrace()
                    );
                }
                StringBuffer sb = (i < severeErrorsCount) ? sbErr : sbWarn;
                sb.append('\n');
                sb.append(lw.getResultNumber());
                sb.append(" --> ");
                sb.append(lw.getException().getMessage());
                i++;
            }
            String header = (
                "Process " + getId() + " operation batch raised " +
                warnings.size() + " errors. StackTrace:\n" + stackTrace +
                "\nThese are the "
            );
            error(
                header + "first " +  Math.min(warnings.size(), severeErrorsCount)
                + " ones:" + sbErr.toString()
            );
            if (warnings.size() > severeErrorsCount) {
                warning(
                    header + "last " + (warnings.size() - severeErrorsCount) +
                    " ones:" + sbWarn.toString());
            }

            try {
                m_warnBuffer.add(new ProcessWarnings(m_dbId, warnings));
            } catch (PoolException e) {
                throw new CourierException(e);
            }

            if (m_pipe.warningAsError()) {
                throw new CourierException(
                    "warning-as-error is enabled so see cause",
                    warnings.get(0).getException()
                );
            }
        }

        protected String resolveAlias(String dbName) {
            final String sa = processResolveSourceAlias(dbName);
            if (sa != null) return sa;

            if (dbName.equals(getPipeline().getDefaultTargetName())) {
                return m_defaultTargetDbName;
            }
            return dbName;
        }

        public final PooledObjectHolder initDatabase(String dbName, boolean isSource) {
            final String defSource = getPipeline().getDefaultSourceName();
            final boolean isDefName;
            final String realName;
            if (defSource != null && dbName.equals(m_defaultSourceDbName)) {
                realName = defSource;
                isDefName = true;
            } else if (dbName.equals(m_defaultTargetDbName)) {
                realName = getPipeline().getDefaultTargetName();
                isDefName = true;
            } else {
                realName = dbName;
                isDefName = false;
            }

            PooledObjectHolder po = getPipeline().getPooledObject(realName);
            if (po == null) {
                throw new RuntimeException("Invalid database name '" + realName + "'");
            }
            DataReceiver dr = (DataReceiver)po.getObject();
            if (dr instanceof ReceiverTimeCounter) {
                ((ReceiverTimeCounter)dr).clearTargetTime();
            }
            if (po.getObject() instanceof DataSource) {
                dr.setTimeout(m_sourceWaitTimeout);
                DataSource ds = (DataSource)po.getObject();
                if (isDefName) {
                    ds = new LoggingDataSource(m_logger, ds, m_dataLogger, getSourceRuleName());
                    po = new DelegatePooledObjectHolder(po, ds);
                }
            } else {
                dr.setTimeout(m_targetWaitTimeout);
                if (isDefName) {
                    dr = new LoggingDataReceiver(m_logger, dr, m_dataLogger, m_pipeDesc);
                    po = new DelegatePooledObjectHolder(po, dr);
                }
            }
            setPooledObject(dbName, po);

            return po;
        }

        protected void initDataSource(String dbName) throws CourierException {
            initDatabase(dbName, true);
        }

        protected void initDataReceiver(String dbName) throws CourierException {
            initDatabase(dbName, false);
        }

        protected void initPooledObject(String name) throws CourierException {
            setPooledObject(name, getPipeline().getPooledObject(name));
        }

        public void cleanUp() throws CourierException {
            try { super.cleanUp(); }
            catch (Exception e) { m_logger.warning(e); }

            try { m_warnBuffer.close(); }
            catch (PoolException e) { m_logger.warning(e); }
        }
    }

    public TargetScriptProcess(
        final CourierLogger logger,
        final String logDecorName, final String logDecorDef,
        final Integer dbId,
        final TransferRequest request,
        final Pipeline pipe,
        final String pipeValue,
        final StatementProvider sourceStatements,
        Integer failCount,
        Map<String, ScriptExpression> pars
    ) throws CourierException {
        m_pipeDesc = formPipeDesc(getCourierParam(pipe, logDecorName, logDecorDef), dbId, pipe, request.getRuleName());
        m_logger = new LeftTemplateLoggerDecorator(logger, m_pipeDesc);
        final CourierContext applContext = pipe.getCourier();

        m_recordCount = -1;

        m_defaultTargetDbName = applContext.getScriptParam("target-db-name");
        m_defaultSourceDbName = pipe.getScriptParam("source-db-name");
        m_pipe = pipe;

        m_sysDb = pipe.getSystemDb();
        m_dataLogger = pipe.getDataLogger();

        final TargetProfile tp = pipe.getTargetProfile();
        m_targetWaitTimeout = pipe.getTargetWaitTimeout();

        m_warnBuffer = new AsyncBuffer<ProcessWarnings>(
            m_logger, applContext.getThreadPool(), 1,
            applContext.getTimer(), 0,
            new AsyncBuffer.Receiver<ProcessWarnings>() {
                public void handleData(List<ProcessWarnings> dataPart) {
                    try { m_sysDb.addWarnings(dataPart); }
                    catch (CourierException e) { m_logger.error(e); }
                }

                public void close() {
                    // !!! handle this
                }
            },
            10, null
        );

        m_dbId = dbId;
        m_ctx = new ScriptContext(m_logger, pipe.getDateFormat());
        m_ctx.setParent(applContext.getCommonContext());

        m_ctx.setObject(
            applContext.getScriptParam("courier-object-name"),
            applContext
        );

        m_ctx.setObject(
            applContext.getScriptParam("pipe-object-name"),
            pipe
        );
        m_ctx.setVar(c_PipeNameVarName, pipe.getName());
        m_ctx.setVar(c_PipeValueVarName, pipeValue);

        m_ctx.setVar(c_processId, dbId.toString());
        m_ctx.setVar("$ConfigPath", applContext.getAppFile(null).getAbsolutePath());
        m_ctx.setVar(c_recordCountVarName, "0");
        m_ctx.setObject(c_IgnoreErrorNumberObjectName, request.getIgnoreErrorCount());
        //m_ctx.setObject(c_IgnoreErrorNumberObjectName, m_rule.getIgnoreErrorCount());
        m_ctx.setObject(c_FailCountObjectName, failCount);

        m_ctx.setObject(
            applContext.getScriptParam("source-iterator-object-name"),
            sourceStatements
        );

        m_ctx.setObject(SourceRule.c_targetProfileObjectName, tp);
        m_tProfile = tp.getName();
        m_ctx.setVar(c_TargetProfileVarName, m_tProfile);
        m_ctx.setVar(TargetProfile.c_bytesLimit, Integer.toString(tp.getBytesLimit()));
        m_ctx.setVar(
            TargetProfile.c_recordsLimit,
            Integer.toString(tp.getRecordsLimit())
        );

        m_topStmt = applContext.getPipesTopStatement();
        MapStatementsProvider scripts = new MapStatementsProvider(c_ScriptsObjectName);
        m_ctx.setObject(c_ScriptsObjectName, scripts);
        scripts.addStatement(c_pipesScriptName, applContext.getPipesScript());
        scripts.addStatement(c_pipeScriptName, pipe.getPipeScript());
        ScriptStatement processStmt = request.getLaunchStmt();
        if (processStmt == null) {
            processStmt = new ObjectStatementCaller(c_ScriptsObjectName, c_stdScriptName);
        }
        scripts.addStatement(c_processScriptName, processStmt);
        scripts.addStatement(c_stdScriptName, applContext.getSourceTopStatement());

        if (pars != null) {
            for (Map.Entry<String, ScriptExpression> e: pars.entrySet()) {
                m_ctx.setVar(e.getKey(), e.getValue());
            }
        }
    }

    public final Integer getId() {
        return m_dbId;
    }

    public final String getTargetProfileName() {
        return m_tProfile;
    }

    public final DataLogger getDataLogger() {
        return m_dataLogger;
    }

    public final int getRecordCount() {
        return m_recordCount;
    }

    public final int getErrorCount() {
        return m_ctx.getErrorCount();
    }

    public final String getErrorText() {
        return m_ctx.getErrorText();
    }

    public final String getErrorStack() {
        return m_ctx.getErrorStack();
    }

    public long getStartTime() {
        return m_startTime;
    }

    public long getTotalTime() {
        return m_totalTime;
    }

    public long getTargetTime() {
        return m_targetTime;
    }

    public long getSourceTime() {
        return m_ctx.getSourceTime();
    }

    public String getSourceDbName() {
        return getPipeline().getDefaultSourceName();
    }

    public String getTargetDbName() {
        return getPipeline().getDefaultTargetName();
    }

    public PoolObjectFactory getPoolFactory(String name) {
        return getPipeline().getObjectPool(name).getObjectFactory();
    }

    public String getIntervalValue() {
        return getVar(TransferProcess.c_intervalValueVarName);
    }

    public boolean hasVar(String name) {
        return m_ctx.hasVar(name);
    }

    public String getVar(String name) throws CourierException {
        return m_ctx.getVar(name);
    }

    public void setVar(String name, String value) {
        m_ctx.setVar(name, value);
    }

    public void removeVar(String name) {
        m_ctx.removeVar(name);
    }

    public Date getDateVar(String name) throws CourierException {
        return m_ctx.getDateVar(name);
    }

    public Object getObject(String name) throws CourierException {
        return m_ctx.getObject(name);
    }

    private String getVar(
        AbstractContext ctx, String name, String caption
    ) throws CourierException {
        if (ctx.hasVar(name)) return " " + caption + "=" + ctx.getVar(name);
        return "";
    }

    protected final ScriptStatement getTopStatement() {
        return m_topStmt;
    }

    protected final String processResolveSourceAlias(String dbName) {
        final String defSource = getPipeline().getDefaultSourceName();
        if (dbName.equals(defSource)) return m_defaultSourceDbName;
        /*
        if (dbName.equals(m_defaultTargetDbName)) {
            final String defTarget = getPipeline().getDefaultTargetName();
            if (defTarget.equals(defSource)) return m_defaultSourceDbName;
        }
        */
        return null;
    }

    protected final FullPipeline getPipeline() {
        return m_pipe;
    }

    //******************************* override ********************************
    protected String getSourceRuleName() {
        return null;
    }
    //*************************************************************************

    public final String getCurrentStateDesc() {
        return m_ctx.getCurrentStateDesc();
    }

    public void start() {
        long endTime;
        try {
            m_logger.info(
               "+" + m_dbId + getVar(m_ctx, c_intervalValueVarName, "IV")
            );
            
            m_startTime = System.currentTimeMillis();
            m_ctx.exec(getTopStatement());
            endTime = System.currentTimeMillis();
        } catch(Throwable e) {
            endTime = System.currentTimeMillis();
            String errorText = ErrorHelper.getOriginalCause(e).getMessage();
            m_ctx.addError(errorText);
            m_ctx.setErrorStack(ExceptionUtils.getStackTrace(e));
            m_logger.error(errorText, e);
        }
        synchronized (this) {
            if (!m_active) return;
        }

        m_totalTime = endTime - m_startTime;

        String recordCount = "???";
        String intervalValue = "";
        Integer failCount = null;
        int ignoreErrorCount = 0;
        try {
            if (m_ctx.hasVar(c_recordCountVarName)) {
                recordCount = m_ctx.getVar(c_recordCountVarName);
                m_recordCount = Integer.parseInt(recordCount);
            }
            intervalValue = getVar(m_ctx, c_intervalValueVarName, "IV");
            if (getErrorCount() == 0) {
                m_ctx.setObject(TransferProcess.c_FailCountObjectName, 0);
                failCount = 0;
            } else {
                failCount = (Integer)m_ctx.getObject(TransferProcess.c_FailCountObjectName);
            }
            if (m_ctx.hasVar(TransferProcess.c_IgnoreErrorCountObjectName)) {
                ignoreErrorCount = ((Integer)m_ctx.getObject(TransferProcess.c_IgnoreErrorCountObjectName));
            }
        } catch (Exception e) {
            m_logger.warning(e);
        }

        if (ignoreErrorCount > 0) {
            m_logger.error(
                "Process " + m_dbId + " ignored " + ignoreErrorCount + " error(s)"
            );
        }

        m_targetTime = m_ctx.getTargetTime(m_defaultTargetDbName);

        try { close(); }
        catch (Exception e) { m_logger.warning(e); }

        try { getDataLogger().flush(); }
        catch (Exception e) { m_logger.warning(e); }

        m_logger.info(
            "#" + m_dbId
            + "    (R=" + recordCount + ")"
            + " | TT=" + formatTime(getTotalTime())
            + " ST=" + formatTime(getSourceTime())
            + " RT=" + formatTime(getTargetTime())
            + " |" + intervalValue
            + " | EC=" + getErrorCount()
            + " IEC=" + ignoreErrorCount
            + " FC=" + failCount
        );
    }

    private static NumberFormat s_reportNumberFormat;
    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        s_reportNumberFormat = new DecimalFormat("0.0", dfs);
    }

    private static String formatTime(long time) {
        double d = (double)(time / 100) / 10D;
        return s_reportNumberFormat.format(d) + "s";
    }

    public void stop() throws CourierException {
        m_ctx.stop();
    }

    public void close() throws CourierException {
        synchronized (this) {
            if (!m_active) return;
            m_active = false;
        }
        m_ctx.cleanUp();
    }

    public void syncProgress() throws CourierException {
        m_sysDb.processProgress(
            m_dbId,
            Integer.parseInt(m_ctx.getVar(c_recordCountVarName)),
            m_ctx.getErrorCount(),
            m_ctx.movedSinceLastCall()
        );
    }

    public synchronized TransferProcessResult getResults() {
        if (m_active) throw new RuntimeException("Process is active");
        return this;
    }

    private static String getCourierParam(Pipeline pipe, String name, boolean mustExist) {
        return pipe.getCourier().getParam(name, mustExist);
    }

    private static String getCourierParam(Pipeline pipe, String name, String def) {
        String ret = getCourierParam(pipe, name, false);
        if (ret == null) ret = def;
        return ret;
    }

    protected static String formPipeDesc(
        String template, Integer id, Pipeline pipe, String ruleName
    ) throws CourierException {
        HashMapStringContext ctx = new HashMapStringContext();
        ctx.setVar("id", "" + id);
        ctx.setVar("pipe", pipe.getName());
        ctx.setVar("rule", ruleName != null ? ruleName : "");
        SimpleTemplate tmpl = new SimpleTemplate();
        return tmpl.process(template, ctx);
    }

}
