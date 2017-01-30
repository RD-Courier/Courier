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
import ru.rd.courier.jdbc.ResultSets.StringBufferListResultSet;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.LeftTemplateLoggerDecorator;
import ru.rd.courier.logging.StdLoggerAdapter;
import ru.rd.courier.logging.data.AbstractDataLogger;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.scripting.PreparedTemplate;
import ru.rd.courier.scripting.ScriptExpression;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.dataaccess.JmsSource;
import ru.rd.courier.scripting.expressions.string.Const;
import ru.rd.courier.scripting.expressions.string.Null;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.ErrorHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.templates.HashMapStringContext;
import ru.rd.courier.schedule.*;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PooledObjectHolder;
import ru.rd.scheduling.leveled.*;
import ru.rd.scheduling.leveled.launchers.PeriodicLauncher;
import ru.rd.thread.FreeListener;
import ru.rd.thread.PoolExecutor;
import ru.rd.thread.WorkThread;
import ru.rd.utils.StatedObject;
import ru.rd.utils.StatedObjectExtendable;

import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Logger;

public class Pipeline extends StatedObjectExtendable implements FullPipeline {
    public static int c_WorkState = 1;
    public static int c_StoppedState = c_WorkState + 1;
    public static int c_FrozenState = c_StoppedState + 1;

    public static char c_IntegerMarkType = 'i';
    public static char c_DateMarkType = 'd';

    public static final String defaultTimeUnit = "s";

    private boolean m_disabled = false;
    private Application m_appl = null;
    private final CourierLogger m_originalLogger;
    private boolean m_needDataLogger;
    private boolean m_dataLoggingBuffered;
    private boolean m_clearVarsOnStart;
    private long m_clearVarsPeriod;
    private long m_clearVarsTime;
    private DataLogger m_dataLogger = null;
    private String m_name = null;
    private String m_sourceDbName = null;
    private String m_targetDbName = null;
    private String m_sourceProfileName = null;
    private String m_targetProfileName = null;
    private int m_sourceWaitTimeout = -1;
    private int m_targetWaitTimeout = -1;
    private int m_checkPointInterval;
    private long m_stopTimeout;
    private String m_baseRuleName = null;
    private Integer m_ignoreErrorCount;
    private StartStopIntersector m_scheduler = null;
    private StartStopListenerSet m_switch;
    private String m_desc = null;
    private char m_markType;
    private String m_markInitial;
    private int m_maxWorkCount;
    private Map<Integer, ProcessExecutor> m_activeProcs =
        new HashMap<Integer, ProcessExecutor>();
    private boolean m_firstLaunch = true;
    private boolean m_warningAsError;
    private ScriptStatement m_pipeScript;

    //private static final String c_stoppedState = "STOPPED";
    //private static final String c_runningState = "RUNNING";
    //private static final String c_stoppingState = "STOPPING";

    private static class FakeLogger extends AbstractDataLogger {
        private static FakeLogger m_instance = new FakeLogger();
        public static FakeLogger instance() {
            return m_instance;
        }

        public void log(byte[] msg, int offset, int length) {}
        public void flush() {}
        public void close() {}
    }

    private class BaseProcessRegistrator implements Runnable, NextLaunchNotifier {
        private final String m_pipeName = getName(); // to prevent deadlock
        private final String m_ruleName;
        private final ScriptStatement m_launchStmt;
        private SupervisorFactory m_supervisorFactory;
        private NextLaunchListener m_nextLaunchListener;

        public BaseProcessRegistrator(
            String ruleName, ScriptStatement launchStmt,
            SupervisorFactory supervisorFactory
        ) {
            m_ruleName = ruleName;
            m_launchStmt = launchStmt;
            m_supervisorFactory = supervisorFactory;
        }

        public BaseProcessRegistrator(
            ScriptStatement launchStmt, SupervisorFactory supervisorFactory
        ) {
            this(m_baseRuleName, launchStmt, supervisorFactory);
        }

        public void setSupervisor(SupervisorFactory supervisorFactory) {
            m_supervisorFactory = supervisorFactory;
        }

        public SupervisorFactory getSupervisor() {
            return m_supervisorFactory;
        }

        public void run() {
            try {
                if (m_nextLaunchListener != null) {
                    m_nextLaunchListener.onNextLaunch();
                    m_nextLaunchListener = null;
                }
                registerProcess(
                    m_ruleName, m_launchStmt,
                    (m_supervisorFactory == null) ? null : m_supervisorFactory.create(),
                    new Pipeline.StdProcessFactory(null)
                );
            } catch (CourierException e) { m_logger.error(e); }
        }

        public void setListener(NextLaunchListener listener) {
            m_nextLaunchListener = listener;
        }

        public String toString() {
            return "Base process launcher:" +
                " pipe " + m_pipeName + "; rule = " + m_ruleName;
        }
    }

    public Pipeline(
        final Application appl, final CourierLogger msgh, final Element conf
    ) throws CourierException {
        super(msgh);
        m_appl = appl;
        m_originalLogger = msgh;
        m_state = c_stateStopped;

        try {
            init(conf);
        } catch (CourierException e) {
            m_disabled = true;
            throw e;
        }
    }

    public boolean isDisabled() {
        return m_disabled;
    }

    private Logger getLogger() {
        Logger logger = null;
        if (m_logger instanceof StdLoggerAdapter) {
            logger = ((StdLoggerAdapter)m_logger).getInnerLogger();
        }
        return logger;
    }

    public static final String c_nameAttr = "name";
    private static final String c_sourceNameAttr = "source-name";
    private static final String c_clearVarsPeriodAttr = "clear-vars-period";

    public static String s_PipeMessageTemplate = "[ [%pipe] ] ";

    private Timer getRealTimeSourceTimer() {
        return m_appl.getListeningSourceTimer();
    }

    private static class XmlConfigurer {
        protected final Element rconf;
        protected final String m_confStop;
        protected Element conf;

        public XmlConfigurer(Node rconf, String confStop) {
            this.rconf = (Element)rconf;
            m_confStop = confStop;
        }

        public final String getAttr(String name, String def) {
            return DomHelper.getNodeAttr(conf, name, def);
        }

        public final String getAttr(String name) {
            return DomHelper.getNodeAttr(conf, name);
        }

        public final int getInt(String name) {
            return DomHelper.getIntNodeAttr(conf, name);
        }

        public final String getUpAttr(String name, String def) {
            return DomHelper.getUpNodeAttr(conf, name, m_confStop, def);
        }

        public final boolean getUpBool(String name, boolean def) {
            return DomHelper.getUpBoolYesNo(conf, name, m_confStop, def);
        }

        public final boolean getUpBool(String name) {
            return DomHelper.getUpBoolYesNo(conf, name, m_confStop);
        }

        public final int getUpInt(String name, int def) {
            return DomHelper.getUpIntNodeAttr(conf, name, m_confStop, def);
        }

        public final long getUpLong(String name, long def) {
            return DomHelper.getUpLongNodeAttr(conf, name, m_confStop, def);
        }

        public final long getUpTime(String name, long def) {
            return DomHelper.getUpTimeNodeAttr(conf, name, m_confStop, def);
        }

        public final Element getChild(String tag) {
            return DomHelper.getChild(conf, tag);
        }

        public final Element getChild(String tag, Element def) {
            return DomHelper.getChild(conf, tag, def);
        }
    }

    class Initializer extends XmlConfigurer {
        public Initializer(Node conf) {
            super(conf, ((Element)conf.getParentNode()).getTagName());
        }

        private void init() {
            conf = rconf;
            m_targetDbName = getAttr("target-db");
            m_targetProfileName = getAttr("target-name");
            m_needDataLogger = getUpBool("data-logging");
            m_dataLoggingBuffered = getUpBool("data-logging-buffered");
            m_clearVarsOnStart = getUpBool("clear-vars-on-start");
            m_clearVarsPeriod = getUpTime(c_clearVarsPeriodAttr, -1);
            m_clearVarsTime = -1;
            m_stopTimeout = getUpLong("stop-timeout", 10*1000);
            m_warningAsError = getUpBool("warning-as-error", false);
            setDesc(DomHelper.getNodeValue(getChild("description", null)));

            m_pipeScript = Application.createWrapStatement(
                m_appl, conf, "script",
                TargetScriptProcess.c_processScriptName
            );

            m_sourceWaitTimeout = getUpInt("source-wait-timeout", -1);
            m_targetWaitTimeout = getUpInt("target-wait-timeout", -1);

            String pname = getName();
            m_switch = new StartStopContainer(pname + " switch", getLogger());
            StartStopIntersector hostSch = new StartStopIntersector(pname + " host intersector", getLogger());
            StartStopMerger hmMerge = new StartStopMerger(pname + " HM merger", getLogger());
            hmMerge.subscribe(hostSch);
            hmMerge.subscribe(m_appl.getMainPipesSchedules().getSchedule(pname));
            m_scheduler = new StartStopIntersector("Pipe '" + getName() + "' root schedule level", getLogger());
            m_scheduler.subscribe(m_switch);
            m_scheduler.subscribe(hmMerge);
            m_scheduler.subscribe(m_appl.getSchedule());
            hostSch.subscribe(m_appl.getHostSchedule(m_targetDbName));
            m_scheduler.subscribe(m_appl.getHostEnableSchedule(m_targetDbName));
            m_sourceProfileName = null;
            if (rconf.hasAttribute(c_sourceNameAttr)) {
                m_sourceDbName = getAttr("source-db");
                hostSch.subscribe(m_appl.getHostSchedule(m_sourceDbName));
                m_scheduler.subscribe(m_appl.getHostEnableSchedule(m_sourceDbName));
                m_sourceProfileName = getAttr(c_sourceNameAttr);
                String markType = getAttr("mark-type", null);
                if ((markType == null) || (markType.length() == 0)) {
                    m_markType = ' ';
                } else {
                    m_markType = markType.charAt(0);
                }
                m_markInitial = getUpAttr("initial-interval-value", null);
                conf = DomHelper.getChild(rconf, "base-process", false);
                if (conf != null) initBaseProcess();
                conf = DomHelper.getChild(rconf, "clear-vars", false);
                if (conf != null) initClearVarsScheduler();
            } else {
                conf = DomHelper.getChild(conf, "active-source", true);
                initRtSource();
            }

            m_maxWorkCount = getUpInt("max-working-count", 0);

            int cpIntervalFromConfig = getUpInt("checkpoint-interval", -1);

            int cpIntervalFromProfiles = Math.max(
                    getTargetWaitTimeout()
                    , getSourceWaitTimeout()
            );

            m_checkPointInterval = (cpIntervalFromConfig > 0 ? cpIntervalFromConfig : cpIntervalFromProfiles);

            if (m_checkPointInterval < 0) m_checkPointInterval = 600;

            if (cpIntervalFromProfiles > m_checkPointInterval) {
                m_logger.warning(
                    "Source or target profile wait timeout " + cpIntervalFromProfiles +
                    " is greater than value " + m_checkPointInterval +
                    " specified for pipeline check point interval and therefore" +
                    " wait timeout will be used as check point interval"
                );
                m_checkPointInterval = cpIntervalFromProfiles;
            }
        }

        private void initClearVarsScheduler() {
            ScheduleInitializer scheduleInit = new PipeScheduleInitializer() {
                protected Runnable createWork(Node conf, StartStopSet parent) {
                    return new Runnable() {
                        public void run() {
                            getSystemDb().clearPipeVars(getName());
                        }
                        public String toString() {
                            return "Pipe " + m_name + " clearing vars";
                        }
                    };
                }
            };

            scheduleInit.initScheduleLevel(conf, m_scheduler);
        }

        private void initBaseProcess() throws CourierException {
            m_baseRuleName = getAttr("source-rule");
            String ignoreErrorCountAttr = getAttr("ignore-error-number", null);
            if (ignoreErrorCountAttr == null) {
                String procType = m_appl.getSourceProfile(
                    m_sourceProfileName).getSourceRule(m_baseRuleName).getType();
                procType = procType.toLowerCase();
                m_ignoreErrorCount = getUpInt(procType + "-ignore-error-number", 0);
            } else {
                m_ignoreErrorCount = new Integer(ignoreErrorCountAttr);
            }

            initSchedule();
        }

        private void initRtSource() {
            final ScriptStatement stmt = Application.createWrapStatement(
                m_appl, conf, "script", TargetScriptProcess.c_stdScriptName
            );

            String type = getAttr("type");
            final StartStopListener rts;
            StringHandler sh = new StringHandler() {
                public boolean handle(String data, TransferResultListener resultHandler) {
                    return registerProcess(
                        null, stmt, null,
                        new AsyncProcessFactory(new StringBufferListResultSet(null, "RtField", data)),
                        resultHandler
                    );
                }
            };
            if (type.equals("jms")) {
                rts = new JmsSource(m_logger, m_appl.getThreadPool(), getRealTimeSourceTimer(), conf, sh);
            } else {
                throw new RuntimeException("Invalid RtSource type '" + type + "'");
            }

            Node scheduleElement = getChild("schedule", null);
            if (scheduleElement == null) {
                m_scheduler.addListener(rts);
            } else {
                ScheduleInitializer scheduleInit = new PipeScheduleInitializer() {
                    protected void onLevel(StartStopSet level, int subScheduleCount) {
                        if (subScheduleCount == 0) {
                            level.addListener(rts);
                        }
                    }
                    protected Runnable createWork(Node conf, StartStopSet parent) {
                        return null;
                    }
                };

                scheduleInit.initScheduleLevel(scheduleElement, m_scheduler);
            }
        }

        private void initSchedule() throws CourierException {
            final ScriptStatement stmt = Application.createWrapStatement(
                m_appl, conf, "script", TargetScriptProcess.c_stdScriptName
            );

            if (conf.hasAttribute("timeout")) {
                BaseProcessRegistrator bpr = new BaseProcessRegistrator(stmt, null);
                String timeUnit = conf.hasAttribute("timeout-unit") ? getAttr("timeout-unit") : defaultTimeUnit;
                long timeout = StringHelper.parseTimeUnit(getInt("timeout"), timeUnit);
                bpr.setSupervisor(initSupervisor(conf, m_scheduler, bpr));
                adjustSupervisor(bpr, timeout, m_scheduler, bpr);
                m_scheduler.addListener(new PeriodicLauncher(getLogger(), timeout, bpr, m_appl.getTimer()));
                return;
            }

            Node scheduleElement = getChild("schedule", null);
            if (scheduleElement == null) {
                scheduleElement = m_appl.getPipeDefaultSheduleConf();
            }
            if (scheduleElement == null) return;

            ScheduleInitializer scheduleInit = new PipeScheduleInitializer() {
                protected Runnable createWork(Node conf, StartStopSet parent) {
                    BaseProcessRegistrator bpr = new BaseProcessRegistrator(stmt, null);
                    try {
                        bpr.setSupervisor(initSupervisor(conf, parent, bpr));
                    } catch (CourierException err) {
                        throw new RuntimeException(err);
                    }
                    return bpr;
                }

                protected void adjustPeriodicLauncher(Runnable work, PeriodicLauncher launcher, StartStopSet parent) {
                    BaseProcessRegistrator bpr = (BaseProcessRegistrator)work;
                    adjustSupervisor(bpr, launcher.getInterval(), parent, bpr);
                }

                protected void adjustOnceLauncher(Runnable work, StartStopSet parent) {
                    BaseProcessRegistrator bpr = (BaseProcessRegistrator)work;
                    adjustSupervisor(bpr, -1, parent, bpr);
                }
            };

            scheduleInit.initScheduleLevel(scheduleElement, m_scheduler);
        }
    }

    private void init(final Element pconf) throws CourierException {
        setName(DomHelper.getNodeAttr(pconf, c_nameAttr));

        HashMapStringContext ctx = new HashMapStringContext();
        ctx.setVar("pipe", getName());
        m_logger = new LeftTemplateLoggerDecorator(
            m_originalLogger, m_appl.getParam("pipe-log-decorator-template", s_PipeMessageTemplate), ctx
        );

        (new Initializer(pconf)).init();
    }

    private abstract class PipeScheduleInitializer extends ScheduleInitializer {
        protected final Logger getLogger() {
            return Pipeline.this.getLogger();
        }

        protected final Timer getTimer() {
            return Pipeline.this.m_appl.getTimer();
        }
    }

    public interface NextLaunchListener {
        void onNextLaunch();
    }

    public interface NextLaunchNotifier {
        void setListener(NextLaunchListener listener);
    }

    private void adjustSupervisor(
        BaseProcessRegistrator bpr, long timeout,
        final StartStopSet scheduleParent, final NextLaunchNotifier launchNotifier
    ) {
        long defMark = StringHelper.parseTime(getCourier().getParam("default-relaunch-if-more", "21m"));
        final long defTimeout = StringHelper.parseTime(getCourier().getParam("default-relaunch-timeout", "5m"));
        final int defMaxCount = Integer.parseInt(getCourier().getParam("default-relaunch-max-count", "10"));

        if ((timeout >= 0 && timeout < defMark) || bpr.getSupervisor() != null) return;
        final CompoundRelaunchChecker checkers = new CompoundRelaunchChecker();
        checkers.addChecker(new ErrorRelaunchChecker());
        final CompoundLimitFactory limits = new CompoundLimitFactory();

        limits.addFactory(
            new LimitFactory() {
                public RelaunchLimit create() {
                    return new ScheduleLimit(scheduleParent);
                }
            }
        );

        limits.addFactory(
            new LimitFactory() {
                public RelaunchLimit create() {
                    return new NextLaunchLimit(launchNotifier);
                }
            }
        );

        limits.addFactory(
            new LimitFactory() {
                public RelaunchLimit create() {
                    return new MaxRelaunchLimit(defMaxCount);
                }
            }
        );

        SupervisorFactory sf = new SupervisorFactory() {
            private final LimitFactory m_limitFactory = limits;

            public TransferProcessSupervisor create() {
                return new GenericSupervisor(
                    defTimeout, checkers, m_limitFactory.create()
                );
            }
        };
        bpr.setSupervisor(sf);
    }

    public static SupervisorFactory initSupervisor(
        Node conf, final StartStopSet scheduleParent,
        final NextLaunchNotifier launchNotifier
    ) throws CourierException {
        final Element rconf = DomHelper.getChild(conf, "relaunch-if", false);
        if (rconf == null) return null;

        final CompoundRelaunchChecker checkers = new CompoundRelaunchChecker();
        if (DomHelper.getBoolYesNo(rconf, "empty", false)) {
            checkers.addChecker(new NoRecordsRelaunchChecker());
        }
        if (DomHelper.getBoolYesNo(rconf, "error", false)) {
            checkers.addChecker(new ErrorRelaunchChecker());
        }
        if (DomHelper.getBoolYesNo(rconf, "break", false)) {
            checkers.addChecker(new TestVarRelaunchChecker("$Break", "1"));
        }

        final long timeout = ScheduleInitializer.extractTimeFromConf(DomHelper.getChild(rconf, "time-out"));
        if (timeout <= 0) {
            throw new CourierException("Timeout for relauncher not specified");
        }

        return new SupervisorFactory() {
            private final LimitFactory m_limitFactory =
                initRelaunchLimits(rconf, scheduleParent, launchNotifier);

            public TransferProcessSupervisor create() {
                return new GenericSupervisor(
                    timeout, checkers, m_limitFactory.create()
                );
            }
        };
    }

    private static LimitFactory initRelaunchLimits(
        Element conf, final StartStopSet scheduleParent,
        final NextLaunchNotifier launchNotifier
    ) {
        CompoundLimitFactory ret = new CompoundLimitFactory();
        conf = DomHelper.getChild(conf, "limits", false);
        if (conf == null) return ret;
        if (DomHelper.getBoolYesNo(conf, "end-of-schedule", false)) {
            ret.addFactory(
                new LimitFactory() {
                    private final StartStopSet m_scheduleParent = scheduleParent;
                    public RelaunchLimit create() {
                        return new ScheduleLimit(m_scheduleParent);
                    }
                }
            );
        }

        if (DomHelper.getBoolYesNo(conf, "until-next-launch", false)) {
            ret.addFactory(
                new LimitFactory() {
                    public RelaunchLimit create() {
                        return new NextLaunchLimit(launchNotifier);
                    }
                }
            );
        }

        final String maxCountStr = DomHelper.getNodeAttr(conf, "max-relaunch-count", null);
        if (maxCountStr != null) {
            ret.addFactory(
                new LimitFactory() {
                    private int m_max = Integer.parseInt(maxCountStr);
                    public RelaunchLimit create() {
                        return new MaxRelaunchLimit(m_max);
                    }
                }
            );
        }

        return ret;
    }

    void register() throws CourierException {
        m_appl.getSystemDb().registerPipeline(
            getName(), getDesc(), c_WorkState,
            m_checkPointInterval, m_markType, m_maxWorkCount
        );
        if (m_sourceProfileName != null) getSourceProfile().register(getName());
    }

    void start() throws Exception {
        synchronized(lock) {
            if (m_needDataLogger) {
                m_dataLogger = m_appl.getPipeLogger(getName(), m_dataLoggingBuffered);
            } else {
                m_dataLogger = FakeLogger.instance();
            }

            if (m_sourceDbName != null) {
                SourceProfile sp = getSourceProfile();

                if (!m_appl.hasPool(m_sourceDbName)) {
                    throw new RuntimeException(
                        "Data source '" + m_sourceDbName + "' not found");
                }

                if (m_baseRuleName != null) {
                    SourceRule sr = sp.getSourceRule(m_baseRuleName);
                    if (sr == null) {
                        throw new RuntimeException(
                            "Invalid source rule '" + m_baseRuleName + "'");
                    }
                }
            }

            if (!m_appl.hasPool(m_targetDbName)) {
                throw new RuntimeException(
                    "Data target '" + m_targetDbName + "' not found");
            }

            m_switch.start(null);
            m_state = c_stateStarted;
        }
    }

    void stop() throws CourierException {
        synchronized(lock) {
            if (m_state != c_stateStarted) return;
            m_state = c_stateStopping;
            m_switch.stop();
            checkStoppedState();
        }
    }

    public String getName() {
        return m_name;
    }

    private void setName(final String name) {
        m_name = name;
    }

    public String getDesc() {
        return m_desc;
    }

    private void setDesc(final String desc) {
        m_desc = desc;
    }

    public char getMarkType() {
        return m_markType;
    }

    public SourceProfile getSourceProfile() throws CourierException {
        return m_appl.getSourceProfile(m_sourceProfileName);
    }

    public TargetProfile getTargetProfile() throws CourierException {
        return m_appl.getTargetProfile(m_targetProfileName);
    }

    public DateFormat getDateFormat() throws CourierException {
        return m_appl.getDatabaseDateFormat();
    }

    public PooledObjectHolder getPooledObject(String dbName) throws CourierException {
        return m_appl.getPooledObject(dbName);
    }

    public ObjectPoolIntf getObjectPool(String name) {
        return m_appl.getObjectPool(name);
    }

    public int getTargetWaitTimeout() throws CourierException {
        return (m_targetWaitTimeout > 0 ? m_targetWaitTimeout : getTargetProfile().getWaitTimeout());
    }

    public int getSourceWaitTimeout() throws CourierException {
        return (m_sourceWaitTimeout > 0 ? m_sourceWaitTimeout : (getSourceProfile() == null ? -1 : getSourceProfile().getWaitTimeout()));
    }

    public SystemDb getSystemDb() {
        return m_appl.getSystemDb();
    }

    public String getDefaultSourceName() {
        return m_sourceDbName;
    }

    public String getDefaultTargetName() {
        return m_targetDbName;
    }

    public DataLogger getDataLogger() {
        return m_dataLogger;
    }

    public String getScriptParam(String name) throws CourierException {
        return m_appl.getScriptParam(name);
    }

    private class ProcessExecutor extends StatedObject implements Runnable, FreeListener {
        private final TransferProcess m_process;
        private final ObjectPoolIntf m_threadPool;
        private WorkThread m_thread;

        public ProcessExecutor(final TransferProcess process, final ObjectPoolIntf threadPool) {
            super(Pipeline.this.m_logger, false);
            m_process = process;
            m_threadPool = threadPool;
            m_thread = null;
        }

        public void start() {
            WorkThread thread = (WorkThread)m_threadPool.getObject();
            synchronized(lock) {
                m_thread = thread;
                m_thread.launchWork(this, this);
                setState(c_stateStarted);
            }
        }

        public void run() {
            for (CourierListener l: m_appl.getListeners()) {
                try { l.processStarted(Pipeline.this, m_process); }
                catch (Exception e) { m_logger.warning(e); }
            }

            try { m_process.start(); }
            catch (Exception e) { m_logger.error(e); }
        }

        public String toString() {
            return "process executor of pipe = '" + getName() + "' id = " +
                m_process.getId();
        }

        public void stop() {
            if(getState() != c_stateStarted) return;
            PoolExecutor pe = new PoolExecutor(m_appl.getThreadPool());
            pe.exec(
                new Runnable() {
                    public void run() {
                        try {
                            m_logger.info(
                                "Trying to stop process " + m_process.getId()
                                + "; state: " + m_process.getCurrentStateDesc()
                                + "\n" + ErrorHelper.stackTraceToString(m_thread.getStackTrace())
                            );

                            m_process.stop();
                        }
                        catch (Exception e) { m_logger.error(e); }
                    }

                    public String toString() {
                        return "trying to stop transfer process id = " +
                            m_process.getId();
                    }
                },
                m_stopTimeout,
                m_logger
            );

            try { waitState(c_stateStopped, m_stopTimeout); }
            catch (Exception e) { m_logger.warning(e); }

            if (getState() == c_stateStarted) {
                m_logger.info(
                    "Trying to interrupt process " + m_process.getId()
                    + "; state: " + m_process.getCurrentStateDesc()
                    + "\n" + ErrorHelper.stackTraceToString(m_thread.getStackTrace())
                );

                m_thread.interrupt();

                try { waitState(c_stateStopped, 10000); }
                catch (Exception e) { m_logger.warning(e); }
            }

            if (getState() == c_stateStarted) {
                m_logger.warning(
                    "Interrupt has not helped to stop process " + m_process.getId()
                    + "; state: " + m_process.getCurrentStateDesc()
                    + "\n" + ErrorHelper.stackTraceToString(m_thread.getStackTrace())
                );

                // is potentially not to return so may be implemented as timeout-ed
                try { m_process.close(); }
                catch (Exception e) { m_logger.warning(e); }

                innerFree();
            }
        }

        public void free(Runnable w) {
            innerFree();
        }

        public void innerFree() {
            if (getState() != c_stateStarted) return;

            setState(c_stateStopped);

            try {
                cleanUpProcess(m_process);
                getSystemDb().processFinished(m_process);
            }
            catch (Throwable e) { m_logger.error(e); }

            for (CourierListener l: m_appl.getListeners()) {
                try { l.processFinished(Pipeline.this, m_process); }
                catch (Exception e) { m_logger.warning(e); }
            }

            try { m_threadPool.releaseObject(m_thread); }
            catch (Throwable e) { m_logger.error(e); }

            try { checkStoppedState(); }
            catch (Throwable e) { m_logger.error(e); }

            try {
                getSystemDb().checkWaitingProcess();
            } catch (Exception e) {
                m_logger.error(e);
            }
        }

        public TransferProcess getProcess() {
            return m_process;
        }
    }

    boolean breakProcess(final Integer dbId) throws CourierException {
        if (!m_activeProcs.containsKey(dbId)) return false;

        final ProcessExecutor procExec = m_activeProcs.get(dbId);
        PoolExecutor pe = new PoolExecutor(m_appl.getThreadPool());
        pe.exec(
            new Runnable() {
                public void run() { procExec.stop(); }

                public String toString() {
                    return "pipeline stopping process " + (dbId);
                }
            }
        );
        return true;
    }

    ScriptExpression getIntegerInitialIntervalValue() throws CourierException {
        if (m_markInitial == null) {
            return new Const(TransferProcess.c_intMarkDefault.toString());
        } else {
            return new PreparedTemplate(m_markInitial);
        }
    }

    private ScriptExpression getDateInitialIntervalValue(
        DateFormat dateFormat
    ) throws CourierException {
        if (m_markInitial == null) {
            return new Const(dateFormat.format(TransferProcess.c_dateMarkDefault));
        } else {
            return new PreparedTemplate(m_markInitial);
        }
    }

    public boolean isRunning() {
        synchronized(lock) {
            return m_state == c_stateStarted;
        }
    }

    private boolean needToClearVarsBecauseOfTime() {
        if (m_clearVarsPeriod <= 0) return false;
        if (m_clearVarsTime < 0) return true;
        long now = System.currentTimeMillis();

        m_logger.debug("now - clearVarsTime = " + (now - m_clearVarsTime));

        return (now - m_clearVarsTime) > m_clearVarsPeriod;
    }

    private boolean needToClearVars() {
        return (m_clearVarsOnStart && m_firstLaunch) ||
            needToClearVarsBecauseOfTime();
    }

    TransferProcess launchProcess(
        final Integer dbId,
        TransferRequest request, LocalSystemDb.PipeInfo dynPipe
    ) throws CourierException {
        synchronized(lock) {
            if (m_state != c_stateStarted) {
                m_logger.debug("launchProcess: pipeline '" + m_name + "' is not running");
                return null;
            }
            //ensureState(c_runningState);

            String intervalValue = dynPipe.getIntervalValue();
            String pipeValue = dynPipe.getPipeValue();

            if (needToClearVars()) {
                intervalValue = null;
                pipeValue = "";
                if (m_clearVarsPeriod > 0) {
                    long now = System.currentTimeMillis();
                    long adjustment = m_clearVarsTime < 0 ?
                        0 : (now - m_clearVarsTime) % m_clearVarsPeriod;
                    m_clearVarsTime = now - adjustment;
                }
            }

            ScriptExpression intervalValueExp = new Null();
            if (intervalValue == null) {
                if (m_markType == 'i' || m_markType == 'b') {
                    intervalValueExp = getIntegerInitialIntervalValue();
                } else if (m_markType == 'd') {
                    intervalValueExp = getDateInitialIntervalValue(m_appl.getDatabaseDateFormat());
                }
            } else {
                intervalValueExp = new Const(intervalValue);
            }

            final TransferProcess tp = request.getFactory().createProcess(
                m_originalLogger,
                this, dbId,
                request, dynPipe.getFailCount(),
                intervalValueExp, pipeValue
            );
            ProcessExecutor pe = new ProcessExecutor(tp, m_appl.getThreadPool());
            m_activeProcs.put(dbId, pe);
            pe.start();
            return tp;
        }
    }

    public static interface ProcessFactory {
        TransferProcess createProcess(
            CourierLogger logger, Pipeline pipe,
            Integer id, TransferRequest request,
            Integer failCount, ScriptExpression intervalValue, String pipeValue
        );
    }

    public static class StdProcessFactory implements ProcessFactory {
        private final Map<String, ScriptExpression> m_pars;

        public StdProcessFactory(Map<String, ScriptExpression> pars) {
            m_pars = pars;
        }

        public TransferProcess createProcess(
            CourierLogger logger, Pipeline pipe,
            Integer id, TransferRequest request,
            Integer failCount, ScriptExpression intervalValue, String pipeValue
        ) {
            return new StdScriptProcess(
                logger, pipe, id, request, failCount, intervalValue, pipeValue, m_pars
            );
        }
    }

    public static class AsyncProcessFactory implements ProcessFactory {
        private final ResultSet m_rs;

        public AsyncProcessFactory(ResultSet rs) {
            m_rs = rs;
        }

        public TransferProcess createProcess(
            CourierLogger logger, Pipeline pipe,
            Integer id, TransferRequest request,
            Integer failCount, ScriptExpression intervalValue, String pipeValue
        ) {
            return new AsyncTransferScriptProcess(
                logger, pipe.getCourier(), id, request, failCount, pipe, pipeValue, m_rs
            );
        }
    }

    public void cleanUpProcess(TransferProcess tp) {
        synchronized(lock) {
            if (m_activeProcs.containsKey(tp.getId())) {
                if (m_firstLaunch /* && (tp.getErrorCount() == 0)*/) {
                    m_firstLaunch = false;
                }
                m_activeProcs.remove(tp.getId());
            }
        }
    }

    private void checkStoppedState() {
        synchronized(lock) {
            boolean notifyApp = false;
            if (m_state == c_stateStopping && m_activeProcs.size() == 0) {
                if (m_dataLogger != null) m_dataLogger.close();
                m_state = c_stateStopped;
                notifyApp = true;
            }
            if (notifyApp) m_appl.pipelineStopped(this);
        }
    }

    public void syncProgress() throws CourierException {
        synchronized(lock) {
            for (ProcessExecutor pe: m_activeProcs.values()) {
                pe.getProcess().syncProgress();
            }
        }
    }

    public int getActiveProcCount() {
        synchronized(lock) {
            return m_activeProcs.size();
        }
    }

    public void stopActiveProcesses() {
        final Collection<ProcessExecutor> procs;
        synchronized(lock) {
            procs = new ArrayList<ProcessExecutor>(m_activeProcs.values());
        }
        PoolExecutor pe = new PoolExecutor(m_appl.getThreadPool());
        pe.exec(
            new Runnable() {
                public void run() {
                    for (ProcessExecutor pe: procs) pe.stop();
                }

                public String toString() {
                    return "pipeline " + m_name + " stopping active processes";
                }
            }
        );
    }

    public CourierContext getCourier() {
        return m_appl;
    }

    public String getBaseRuleName() {
        return m_baseRuleName;
    }

    public Set<String> getUsedSourceNames() throws CourierException {
        Set<String> res = new HashSet<String>();
        res.add(getDefaultSourceName());
        if (getBaseRuleName() != null) {
            res.addAll(
                getSourceProfile().getSourceRule(getBaseRuleName()).getSourceLinks()
            );
            res.addAll(
                getTargetProfile().getSourceLinks()
            );
        }
        return res;
    }

    public Set<String> getUsedTargetNames() throws CourierException {
        Set<String> res = new HashSet<String>();
        res.add(getDefaultTargetName());
        if (getBaseRuleName() != null) {
            res.addAll(
                getSourceProfile().getSourceRule(getBaseRuleName()).getTargetLinks()
            );
            res.addAll(
                getTargetProfile().getTargetLinks()
            );
        }
        return res;
    }

    public final Set<String> getUsedDbNames() throws CourierException {
        Set<String> ret = getUsedSourceNames();
        ret.addAll(getUsedTargetNames());
        return ret;
    }

    private static void incCounter(Map<String, Integer> counters, Set<String> names) {
        for (String name: names) incCounter(counters, name);
    }

    private static void incCounter(Map<String, Integer> counters, String name) {
        if (counters.containsKey(name)) {
            counters.put(name, counters.get(name) + 1);
        } else {
            counters.put(name, 1);
        }
    }

    public void incDbCounter(Map<String, Integer> counters) throws CourierException {
        Set<String> names = getUsedSourceNames();
        names.addAll(getUsedTargetNames());
        names.remove(null);
        incCounter(counters, names);
    }

    public ScriptStatement getPipeScript() {
        return m_pipeScript;
    }

    public boolean warningAsError() {
        return m_warningAsError;
    }

    private boolean registerProcess(
        String ruleName,
        ScriptStatement stmt, TransferProcessSupervisor supervisor,
        Pipeline.ProcessFactory factory, TransferResultListener resultHandler
    ) {
        TransferRequest tr = new TransferRequest(
            getName(), ruleName, m_ignoreErrorCount, stmt, supervisor, factory
        );
        tr.setResultHandler(resultHandler);
        return m_appl.getSystemDb().registerProcessRequest(tr);
    }

    private boolean registerProcess(
        String ruleName,
        ScriptStatement stmt, TransferProcessSupervisor supervisor,
        Pipeline.ProcessFactory factory
    ) {
        return registerProcess(ruleName, stmt, supervisor, factory, null);
    }
}
