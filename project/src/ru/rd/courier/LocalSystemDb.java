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

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.Storage;
import ru.rd.courier.utils.FileStorage;

import java.io.*;
import java.util.*;

public class LocalSystemDb extends BaseSystemDb {
    private TimerTask m_storeStateTask;
    private final String m_encoding;
    private Storage m_storage;
    private static final String c_storageFileName = "system-db.xml";
    private static final String c_IntervalValueAttr = "interval-value";
    private static final String c_CheckPointIntervalAttr = "checkpoint-interval";

    public static class PipeInfo {
        public String m_code;
        public String m_desc;
        public int m_checkpointInterval;
        public char m_markType;
        public final int m_maxWorkCount;

        private Integer m_failCount;
        private String m_intervalValue;
        private String m_pipeValue;

        public long m_checkTime;
        public int m_inWorkCount;

        public Map<String, RuleInfo> m_rules = new HashMap<String, RuleInfo>();

        public PipeInfo(
            String code, String desc, int checkpointInterval, char markType,
            int maxWorkCount
        ) {
            if (code == null) {
                throw new IllegalArgumentException("Code cannot be null");
            }
            if (desc == null) {
                throw new IllegalArgumentException("Description cannot be null");
            }
            if (checkpointInterval <= 0) {
                throw new IllegalArgumentException("Checkpoint interval must be > 0");
            }

            m_code = code;
            m_desc = desc;
            m_checkpointInterval = checkpointInterval;
            m_markType = markType;
            m_maxWorkCount = maxWorkCount;
            m_intervalValue = null;
            m_pipeValue = "";

            m_inWorkCount = 0;
            m_checkTime = 0;
        }

        public boolean maxWorkingExceeded() {
            return (m_maxWorkCount > 0) && (m_inWorkCount >= m_maxWorkCount);
        }

        private static char getIntervalValueType(Element e) {
            char ret = ' ';
            String p = DomHelper.getNodeAttr(e, "mark-type", false);
            if ((p != null) && (p.length() > 0)) {
                ret = p.charAt(0);
            }
            return ret;
        }

        public static final String c_codeAttr = "code";

        public void toXml(Element parent) {
            Element e = parent.getOwnerDocument().createElement("pipe");
            e.setAttribute("code", m_code);
            e.setAttribute("pipe-value", getPipeValue());
            if (getIntervalValue() != null) {
                e.setAttribute(c_IntervalValueAttr, getIntervalValue());
            }

            /*
            for (Iterator it = m_rules.values().iterator(); it.hasNext();) {
                RuleInfo r = (RuleInfo)it.next();
                r.toXml(e);
            }
            */
            parent.appendChild(e);
        }

        public void restoreState(Node n) {
            String p;
            for (String attrName:
                new String[]{
                    "int-mark", "date-mark", // deprication support
                    c_IntervalValueAttr}
            ) {
                p = DomHelper.getNodeAttr(n, attrName, false);
                if (p != null) m_intervalValue = p;
            }
            p = DomHelper.getNodeAttr(n, "pipe-value", false);
            if (p != null) m_pipeValue = p;
        }

        public void addRule(RuleInfo r) {
            m_rules.put(r.m_name, r);
        }

        public RuleInfo getRule(String name) {
            return m_rules.get(name);
        }

        public void baseProcessAlive() {
            m_checkTime = System.currentTimeMillis();
        }

        public void onProcessStart() {
            m_varsCleared = false;
        }

        private boolean m_varsCleared = false;

        public void clearVars() {
            m_intervalValue = null;
            m_pipeValue = null;
            m_failCount = null;
            m_varsCleared = true;
        }

        public void setVarsIfPossible(
            String intervalValue, String pipeValue, Integer failCount
        ) {
            if (canSetVars()) {
                m_intervalValue = intervalValue;
                m_pipeValue = pipeValue;
                m_failCount = failCount;
            }
        }

        public boolean canSetVars() {
            return !m_varsCleared;
        }

        public String toString() {
            return (
                "Code = " + m_code +
                " Description = " + m_desc +
                " CheckpointInterval = " + m_checkpointInterval +
                " MarkType = " + m_markType +
                " IntervalValue = " + getIntervalValue() +
                " PipeValue = " + getPipeValue()
            );
        }

        public Integer getFailCount() {
            return m_failCount;
        }

        public String getIntervalValue() {
            return m_intervalValue;
        }

        public String getPipeValue() {
            return m_pipeValue;
        }
    }

    private static class RuleInfo {
        public PipeInfo m_pipe;
        public String m_name;
        public String m_desc;
        public String m_type;

        public void toXml(Element parent) {
            Element e = parent.getOwnerDocument().createElement("rule");
            e.setAttribute("name", m_name);
            if ( m_desc != null) e.setAttribute("desc", m_desc);
            if ( m_type != null) e.setAttribute("type", m_type);
            parent.appendChild(e);
        }

        public RuleInfo(
            PipeInfo pipe, String name, String desc, String type
        ) {
            m_pipe = pipe;
            m_name = name;
            m_desc = desc;
            m_type = type;
        }

        public boolean isSingleThreaded() {
            return (
                m_type.equals(SourceRule.c_FreshType)
                ||
                m_type.equals(SourceRule.c_AllType)
            );
        }

    }

    public static class ProcessInfo {
        private boolean m_stopping = false;
        private final CourierLogger m_logger;
        private final SystemDb m_sysDb;
        private final Timer m_timer;
        public final int m_id;
        public final TransferRequest m_request;
        public PipeInfo m_pipe;
        public RuleInfo m_rule;
        public boolean m_break = false;

        public ProcessInfo(
            CourierLogger logger, SystemDb sysDb, Timer timer,
            int id, TransferRequest request, PipeInfo pipe, RuleInfo rule
        ) {
            m_logger = logger;
            m_sysDb = sysDb;
            m_timer = timer;
            m_id = id;
            m_request = request;
            m_pipe = pipe;
            m_rule = rule;
        }

        public TransferRequest getRequest() {
            return m_request;
        }

        public TransferProcessSupervisor getSupervisor() {
            return m_request.getSupervisor();
        }

        public int getRelaunchCount() {
            return getRequest().getRelaunchCount();
        }

        public void progress(
            int recordCount, int errorCount, boolean movedSinceLastCall
        ) {
            if (movedSinceLastCall) {
                PipeInfo pipe = m_pipe;
                pipe.baseProcessAlive();
            }
        }

        public void onStart() {
            m_pipe.onProcessStart();
        }

        public void finished(TransferProcessResult result) throws CourierException {
            PipeInfo pipe = m_pipe;

            if (m_request.getResultHandler() != null) {
                m_request.getResultHandler().transferFinished(result);
            }

            if (!m_stopping) {
                pipe.m_inWorkCount--;

                pipe.setVarsIfPossible(
                    result.hasVar(TransferProcess.c_intervalValueVarName) ? result.getVar(TransferProcess.c_intervalValueVarName): null,
                    result.getVar(TransferProcess.c_PipeValueVarName),
                    (Integer)result.getObject(TransferProcess.c_FailCountObjectName)
                );
            }

            if ((getSupervisor() != null) && !limitsExceeded()) {
                long timeOut = getSupervisor().getRelaunchTimeout(result);
                if (timeOut > 0) {
                    m_logger.info(
                        "Registering relaunch of " + getDesc() +
                        " in " + timeOut + " milliseconds"
                    );
                    m_timer.schedule(
                        new TimerTask() {
                            public void run() {
                                try {
                                    getRequest().incRelaunchCount();
                                    m_sysDb.registerProcessRequest(getRequest());
                                } catch (CourierException e) { m_logger.error(e); }
                            }
                        },
                        timeOut
                    );
                } else {
                    getSupervisor().finish();
                }
            }
        }

        private boolean limitsExceeded() {
            if ((getSupervisor() != null) && !getSupervisor().canBeRelaunched(this)) {
                m_logger.error(
                    "Process " + getDesc() +
                    " has not accomplished its goal because: " +
                    getSupervisor().getRejectReason(this)
                );
                getSupervisor().finish();
                return true;
            } else {
                return false;
            }
        }

        private void addWarnings(List warnings) {}

        public String getDesc() {
            return (
                "pipe = '" + m_pipe.m_code +
                (m_rule == null ? "" : "' rule = '" + m_rule.m_name + "'")
            );
        }
    }

    private class SyncTask extends TimerTask {
        public void run() {
            try {
                syncDatabase();
            } catch(Throwable e) {
                m_logger.error(e);
            }
        }
    }

    private class StoreStateTask extends TimerTask {
        public void run() {
            try {
                storeState();
            } catch(Throwable e) {
                m_logger.error(e);
            }
        }
    }

    private Map<String, PipeInfo> m_pipes = new HashMap<String, PipeInfo>();
    private int lastId = 1;
    private Map<Integer, ProcessInfo> m_waitProcs = new HashMap<Integer, ProcessInfo>();
    private Map<Integer, ProcessInfo> m_workProcs = new HashMap<Integer, ProcessInfo>();

    public LocalSystemDb(
        final Application appl, final CourierLogger msgh,
        final Node n, final Timer timer
    ) throws CourierException {
        super(appl, msgh, n, timer);
        m_encoding = appl.getParam("encoding", false);
        String sdbFile = appl.getParam("system-db-file", false);
        setStorage(new FileStorage(appl.getAppFile(sdbFile == null ? c_storageFileName : sdbFile)));
    }

    private void releaseBrokenProcesses() throws CourierException {
        for (PipeInfo pipe: m_pipes.values()) {
            if (pipe.m_checkpointInterval <= 0) continue;
            long criticalTime = pipe.m_checkTime + pipe.m_checkpointInterval * 1000;
            long curTime = System.currentTimeMillis();

            //m_logger.debug("Pipe " + pipe.m_code + "curTime = " + curTime + " criticalTime = " + criticalTime);

            if ((pipe.m_inWorkCount > 0) && (curTime > criticalTime)) {
                m_logger.error(
                    "Pipeline " + pipe.m_code + " has not registered progress for more than " +
                    pipe.m_checkpointInterval + " seconds" +
                    " and all its working processes will be stopped"
                );

                for (ProcessInfo proc: m_workProcs.values()) {
                    if (proc.m_pipe.m_code.equals(pipe.m_code)) {
                        proc.m_stopping = true;
                    }
                }

                if (m_appl.hasPipeline(pipe.m_code)) {
                    m_appl.getPipeline(pipe.m_code).stopActiveProcesses();
                }

                pipe.m_inWorkCount = 0;
            }
        }
    }

    private void launchProcess(ProcessInfo proc) throws CourierException {
        final PipeInfo pipe = proc.m_pipe;
        final Pipeline pl = m_appl.getPipeline(pipe.m_code);
        if (pl == null) {
            m_logger.warning(
                "Trying to start process: Pipeline '" + pipe.m_code +
                "' not found"
            );
        } else {
            proc.onStart();
            m_workProcs.put(proc.m_id, proc);
            TransferProcess tp = pl.launchProcess(proc.m_id, proc.getRequest(), pipe);
            if (tp != null) {
                pipe.m_inWorkCount++;
                pipe.baseProcessAlive();
            }
        }
    }

    private boolean rejectLaunch(RuleInfo rule) {
        if (rule == null) return false;
        final PipeInfo pipe = rule.m_pipe;
        return
        (
            rule.isSingleThreaded() && (pipe.m_inWorkCount > 0)
        ) || pipe.maxWorkingExceeded();
    }

    private void launchWaitingProcesses() throws CourierException {
        for (Iterator<ProcessInfo> it = m_waitProcs.values().iterator(); it.hasNext(); ) {
            ProcessInfo proc = it.next();
            if (!rejectLaunch(proc.m_rule)) {
                try {
                    launchProcess(proc);
                } finally {
                    it.remove();
                }
            }
        }
    }

    public synchronized void clearPipeVars(String pipeName) {
        m_pipes.get(pipeName).clearVars();
    }

    private void stopBrokenProcesses() throws CourierException {
        for (ProcessInfo proc: m_workProcs.values()) {
            if (proc.m_break) {
                PipeInfo pipe = proc.m_pipe;
                final Pipeline pl = m_appl.getPipeline(pipe.m_code);
                if (pl == null) {
                    m_logger.warning(
                        "While breaking process: pipeline '" + pipe.m_code + "' not found"
                    );
                } else {
                    pl.breakProcess(proc.m_id);
                }
            }
        }
    }

    protected synchronized void syncDatabase() throws CourierException {
        if (!m_started) return;
        m_logger.debug("System DB synchronization");

        try { releaseBrokenProcesses(); }
        catch (Exception e) { m_logger.error(e); }

        try { launchWaitingProcesses(); }
        catch (Exception e) { m_logger.error(e); }

        try { stopBrokenProcesses(); }
        catch (Exception e) { m_logger.error(e); }

        m_appl.syncProgress();
    }

    protected synchronized void storeState() throws CourierException {
        if (!m_started) return;
        Document doc = new DocumentImpl();
        Element e = doc.createElement("system-db");
        for (PipeInfo pipe: m_pipes.values()) pipe.toXml(e);
        doc.appendChild(e);

        OutputFormat format = new OutputFormat(doc, m_encoding, true);
        XMLSerializer serializer;
        Writer out = null;
        try {
            out = new OutputStreamWriter(getStorage().getStoreStream());
            serializer = new XMLSerializer(out, format);
            serializer.asDOMSerializer();
            serializer.serialize(doc);
        } catch (Exception e1) {
            throw new CourierException(e1);
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception ee) { m_logger.error(ee); }
        }
        //m_logger.debug("Local system courier database stored to: " + out.getAbsolutePath());
    }

    protected synchronized void restoreState() throws CourierException {
        //if (!m_started) return;
        Storage storage = getStorage();
        m_logger.debug("Restoring local system database state from : " + storage);
        try {
            InputStream in = storage.getRestoreStream();
            if (in == null) return;
            Document conf = m_appl.parseXml(in);
            if (conf == null) return;
            Element[] pipes = DomHelper.getChildrenByTagName(
                conf.getDocumentElement(), "pipe", false
            );
            if (pipes == null) return;
            for (Element pipe : pipes) {
                PipeInfo pi = m_pipes.get(
                        DomHelper.getNodeAttr(pipe, PipeInfo.c_codeAttr)
                );
                if (pi != null) {
                    pi.restoreState(pipe);
                    m_logger.debug("Pipeline '" + pi.toString() + "' state restored");
                }
            }
        } catch (Exception e) {
            m_logger.warning(e);
        }
    }

    protected TimerTask getTask() {
        return new SyncTask();
    }

    private Storage getStorage() {
        return m_storage;
    }

    public void setStorage(Storage factory) {
        m_storage = factory;
    }

    public synchronized void start() throws CourierException {
        super.start();
        restoreState();
        m_storeStateTask = new StoreStateTask();
        m_timer.schedule(m_storeStateTask, 5 * 1000, 10 * 1000);
        m_started = true;
    }

    public synchronized void stop() throws CourierException {
        if (m_storeStateTask != null) m_storeStateTask.cancel();
        storeState();
        super.stop();
        m_started = false;
    }

    public boolean isActive() {
        return m_started;
    }

    public synchronized void registerPipeline(
        String code, String desc, int status,
        int checkpointInterval, char markType,
        int maxWorkCount
    ) throws CourierException {
        if (!m_pipes.containsKey(code)) {
            m_pipes.put(
                code,
                new PipeInfo(code, desc, checkpointInterval, markType, maxWorkCount)
            );
        }
    }

    public synchronized void registerSourceRule(
        String pipeName, String name, String desc, String type
    ) throws CourierException {
        PipeInfo pipe = m_pipes.get(pipeName);
        pipe.addRule(new RuleInfo(pipe, name, desc, type));
    }

    public synchronized boolean registerProcessRequest(TransferRequest request) throws CourierException {
        String pipeName = request.getPipeName();
        String ruleName = request.getRuleName();

        PipeInfo pipe = m_pipes.get(pipeName);
        if (pipe == null) {
            throw new CourierException("Pipeline " + pipeName + " not found");
        }
        if (pipe.maxWorkingExceeded()) return false;
        final Pipeline pl = m_appl.getPipeline(pipe.m_code);
        if (!pl.isRunning()) return false;

        for (ProcessInfo processInfo : m_waitProcs.values()) {
            final RuleInfo rule = processInfo.m_rule;
            if (
                processInfo.m_pipe.m_code.equals(pipeName) &&
                (rule != null &&
                    (ruleName != null && rule.m_name.equals(ruleName)) &&
                    rule.isSingleThreaded()
                 )
            ) return false;
        }
        ProcessInfo proc = new ProcessInfo(
            m_logger, this, m_timer, lastId, request, pipe, pipe.getRule(ruleName)
        );
        lastId++;
        m_waitProcs.put(proc.m_id, proc);
        launchWaitingProcesses();
        //m_timer.schedule(new SyncTask(), 0);
        return true;
    }

    public synchronized void processFinished(TransferProcess process)
    throws CourierException {
        ProcessInfo proc = m_workProcs.remove(process.getId());
        if (proc != null) {
            proc.finished(process.getResults());
            if (proc.m_rule != null && proc.m_rule.isSingleThreaded()) storeState();
        }
    }

    public synchronized void processProgress(
        Integer dbId, int recordCount, int errorCount, boolean movedSinceLastCall
    ) throws CourierException {
        ProcessInfo proc = m_workProcs.get(dbId);
        if (proc == null) {
            m_logger.error(
                "Trying to notify proccess progress while process for id " +
                dbId + " not found"
            );
        } else {
            proc.progress(recordCount, errorCount, movedSinceLastCall);
        }
    }

    public synchronized void addWarnings(List<ProcessWarnings> warnings) throws CourierException {
        for (ProcessWarnings pw : warnings) {
            ProcessInfo proc = m_workProcs.get(pw.m_id);
            proc.addWarnings(pw.m_warnings);
        }
    }

    public synchronized void checkWaitingProcess() {
        if (!m_waitProcs.isEmpty()) {
            launchWaitingProcesses();
        }
    }
}
