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
import ru.rd.courier.jdbc.mock.MockDatabase;
import ru.rd.courier.jdbc.mock.MockTable;
import ru.rd.courier.logging.SimpleFormatter;
import ru.rd.courier.scripting.DataReceiver;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.ReceiverTimeCounter;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.ErrorHelper;
import ru.rd.courier.utils.FileHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.pool.PoolObjectFactory;
import ru.rd.pool.ObjectPoolIntf;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

/**
 * User: AStepochkin
 * Date: 01.07.2005
 * Time: 13:32:15
 */
public class CourierTestCase extends FileDataTestCase {
    private String m_testName;
    private Application m_courier;
    private Handler m_courierLogHandler;
    private Map<String, Pipe> m_pipes;
    private Map<String, PipeListener> m_customs;
    private CountDownLatch m_doneSignal;
    private List<String> m_errors = new LinkedList<String>();
    private List<LogRecord> m_logData = new LinkedList<LogRecord>();
    private Timer m_timer;
    private int m_launchNumber;
    private MockDatabase m_mockdb;
    private boolean m_stopped;

    private static final String c_attachLogger = "ru.rd.courier";

    protected static class Process {
        public TransferProcess m_tp;
        public final List<String> m_errors = new LinkedList<String>();
        public Date m_start, m_stop;
        public List<String> m_data = new LinkedList<String>();

        public Process(TransferProcess tp) {
            m_tp = tp;
            m_start = new Date();
        }

        public void addError(String error) {
            m_errors.add(error);
        }

        public void processData(String portion) {
            m_data.add(portion);
        }

        public List<String> getData() {
            return m_data;
        }
    }

    private static class ExpectedResults {
        public final List<String> m_errors;
        public final List<String> m_data;
        private final int m_errorCount;
        private final String m_intervalValue;

        public ExpectedResults(
            List<String> errors, List<String> data,
            int errorCount, String intervalValue
        ) {
            m_errors = errors;
            m_data = data;
            m_errorCount = errorCount;
            m_intervalValue = intervalValue;
        }

        public int getErrorCount() {
            return m_errorCount;
        }

        public String getIntervalValue() {
            return m_intervalValue;
        }
    }

    protected interface InitCustomizer {
        String getPipeOtherAttributes();
        public String getPipeCustomTags();
    }

    private static class ReceiverHolder {
        public final SimpleReceiver receiver;
        public final String name;

        public ReceiverHolder(String name, SimpleReceiver receiver) {
            this.name = name;
            this.receiver = receiver;
        }
    }

    protected abstract class Pipe {
        protected final String m_name;
        private final List<ReceiverHolder> m_recs = new LinkedList<ReceiverHolder>();
        private final String m_text;
        private final String m_sp;
        private final String m_tp;
        private final String m_db;
        private Integer m_ignoreErrorCount;
        private int m_workProcCount = 0;
        private int m_maxWorkProcCount;
        private final int m_expectedMaxWorkProcCount;
        private int m_curLaunchNumber = 0;
        private int m_rejectedProcCount = 0;
        private PipeListener m_listener;
        private List<List<Element[]>> m_processInputData;

        public Pipe(
            String name, Integer ignoreErrorCount,
            String otherAttributes, String sourceTransform,
            String sourceRuleOtherAttributes,
            String targetRuleSection, String targetCommonSections,
            int expectedMaxWorkProcCount, Element inputData
        ) throws IOException, CourierException {
            final String cSourceProfileName = "source-profile";
            final String cTargetProfileName = "target-profile";
            m_name = name;
            m_ignoreErrorCount = ignoreErrorCount;
            m_expectedMaxWorkProcCount = expectedMaxWorkProcCount;
            m_listener = m_customs.containsKey(m_name) ?
                m_customs.get(m_name) : new EmptyPipeListener();
            m_text = applyCourierTemplate(
                m_pipeTemplate,
                new String[][]{
                      {"name", decoratePipeName(name, "")}
                    //, {"source-db", decoratePipeName(cPipeName, "source-db")}
                    , {"source", decoratePipeName(name, cSourceProfileName)}
                    , {"target-db", getReceiverName(name)}
                    , {"target", decoratePipeName(name, cTargetProfileName)}
                    , {"checkpoint-interval", "600"}
                    , {"other-attributes", otherAttributes +
                        m_listener.getInitCustomizer().getPipeOtherAttributes()
                    }
                    , {"custom-tags",
                        m_listener.getInitCustomizer().getPipeCustomTags()
                    }
                }
            );

            m_sp = applyCourierTemplate(
                m_sourceProfileTemplate,
                new String[][]{
                      {"name", decoratePipeName(name, cSourceProfileName)}
                    , {"rule", applyCourierTemplate(m_sourceRuleTemplate,
                        new String[][]{
                              {"name", cRuleName}
                            , {"transform", sourceTransform}
                            , {"source-rule-other-attributes", sourceRuleOtherAttributes}
                        }
                    )}
                }
            );

            m_tp = applyCourierTemplate(
                m_targetProfileTemplate,
                new String[][]{
                      {"name", decoratePipeName(name, cTargetProfileName)}
                    , {"common", targetCommonSections}
                    , {"rule", targetRuleSection}
                }
            );

            m_db = "";

            m_processInputData = new LinkedList<List<Element[]>>();
            if (inputData != null) {
                try {
                    MockDatabase.getGlobalDatabase().addTable(
                        m_name, DomHelper.getChild(inputData, "table", true)
                    );
                } catch (SQLException e) {
                    throw new CourierException(e);
                }
                Element perProcess = DomHelper.getChild(inputData, "per-process", false);
                if (perProcess != null) {
                    for (Element lid: DomHelper.getChildrenByTagName(perProcess, "launch", false)) {
                        List<Element[]> processData = new LinkedList<Element[]>();
                        m_processInputData.add(processData);
                        for (Element pid: DomHelper.getChildrenByTagName(lid, "process", false)) {
                            processData.add(
                                DomHelper.getChildrenByTagName(
                                    pid, "record", false
                                )
                            );
                        }
                    }
                }
            }
        }

        public Pipe(Node e) throws IOException, CourierException {
            this(
                DomHelper.getNodeAttr(e, "name"),
                new Integer(DomHelper.getIntNodeAttr(e, "ignore-error-number", 0)),
                DomHelper.getChildValue(e, "other-attributes", ""),
                DomHelper.getChildValue(e, "source-transform"),
                DomHelper.getChildValue(e, "source-rule-other-attributes", ""),
                DomHelper.getChildValue(e, "target-rule-section"),
                DomHelper.getChildValue(e, "target-common-sections", ""),
                DomHelper.getIntNodeAttr(e, "max-working-processes", 1),
                DomHelper.getChild(e, "input-data", false)
            );
        }

        public final synchronized void processStarted(Pipeline pipe, TransferProcess tp) {
            System.out.println(
                  "processStarted:"
                + " pipe = " + pipe.getName()
                + " id = " + tp.getId()
                + " doneSignal.Count = " + m_doneSignal.getCount()
            );
            m_workProcCount++;
            if (m_workProcCount > m_maxWorkProcCount) {
                m_maxWorkProcCount = m_workProcCount;
            }
            try {
                m_listener.processStarted(this, pipe, tp);
                customProcessStarted(pipe, tp);
            } catch (Exception e) {
                addPipeError(e);
            }
        }

        public final synchronized void processFinished(Pipeline pipe, TransferProcess tp) {
            System.out.println(
                  "processFinished:"
                + " pipe = " + pipe.getName()
                + " id = " + tp.getId()
                + " doneSignal.Count = " + m_doneSignal.getCount()
                + " error-count = " + tp.getErrorCount()
                + " error = " + tp.getErrorText()
            );
            m_workProcCount--;
            customProcessFinished(pipe, tp);
            try {
                m_listener.processFinished(this, pipe, tp);
            } catch (Exception e) {
                addPipeError(e);
            }
            m_doneSignal.countDown();
        }

        public final void start() {
            m_curLaunchNumber = -1;
            m_listener.onCourierStart();
            launchNextPortion();
        }

        public void addPipeError(String msg, boolean stopTest) {
            addError(decorateAssertMes(msg));
            stopTest();
        }

        public void addPipeError(String msg) {
            addPipeError(msg, true);
        }

        public void addPipeError(Exception e) {
            addPipeError(
                e.getMessage() + "\n" + ErrorHelper.stackTraceToString(e.getStackTrace()),
                true
            );
        }

        public void setListener(PipeListener listener) {
            m_listener = listener;
        }

        public void customTargetProcess(String request) throws CourierException {
            processData(request);
            if (m_listener != null) m_listener.customTargetProcess(this, request);
        }

        private MockDatabase getDb() {
            try {
                return MockDatabase.getGlobalDatabase();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        protected final synchronized void launchNextPortion() {
            if (m_stopped) return;
            m_curLaunchNumber++;
            if (hasNextPortion()) {
                if (m_customs != null && m_customs.containsKey(m_name)) {
                        m_customs.get(m_name).onLaunchPortion(this);
                }

                if (m_launchNumber < m_processInputData.size()) {
                    List<Element[]> procData = m_processInputData.get(m_launchNumber);
                    if (m_curLaunchNumber < procData.size()) {
                        try {
                            for (Element e: procData.get(m_curLaunchNumber)) {
                                getDb().getTable(m_name).addRecord(e);
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                customNextPortion();
            }
        }

        public final void registerProcess() {
            boolean success = false;
            try {
                success = m_courier.getSystemDb().registerProcessRequest(
                    new TransferRequest(
                        m_name, cRuleName, m_ignoreErrorCount,
                        null, null, new Pipeline.StdProcessFactory(null)
                    )
                );
            } catch (CourierException e) {
                addPipeError(e.getMessage());
            }
            if (!success) {
                m_rejectedProcCount++;
                m_doneSignal.countDown();
            }
        }

        protected final String decorateAssertMes(String mes) {
            return "Pipe " + m_name + ": " + mes;
        }

        public final void checkResults() {
            m_listener.check(this);
            assertEquals("Rejected proc count", 0, m_rejectedProcCount);

            assertTrue(
                "Max Working Processes Count " + m_maxWorkProcCount +
                    " exceeded " + m_expectedMaxWorkProcCount,
                m_expectedMaxWorkProcCount <= 0 ||
                    m_expectedMaxWorkProcCount <= m_maxWorkProcCount
            );

            for (ReceiverHolder r: m_recs) {
                assertFalse("Receiver '" + r.name + "' has not been closed", r.receiver.m_active);
            }

            customCheckResults();
        }

        protected final String getResultsCompareDump() {
            return (
                  "\n------------ EXPECTED ---------------------\n"
                + expectedResultsToString()
                + "\n------------ ACTUAL -----------------------\n"
                + actualResultsToString()
                + "\n-------------------------------------------\n"
            );
        }

        protected final int getLaunchNumber() {
            return m_curLaunchNumber;
        }

        protected final int getWorkProcCount() {
            return m_workProcCount;
        }

        public final DataReceiver getReceiver(String name) {
            SimpleReceiver r = new SimpleReceiver(this);
            m_recs.add(new ReceiverHolder(name, r));
            return r;
        }
        protected void customProcessStarted(Pipeline pipe, TransferProcess tp) {}
        protected void customProcessFinished(Pipeline pipe, TransferProcess tp) {}
        protected abstract boolean hasNextPortion();
        protected void customNextPortion() {}
        public abstract int getTotalLaunchAmount();
        protected abstract void customCheckResults();
        protected abstract String actualResultsToString();
        protected abstract String expectedResultsToString();
        public abstract void processData(String portion);
    }

    protected class ConcurentPipe extends Pipe {
        private int[] m_launchAmounts;
        protected long m_interval;
        private final Set<String> m_errors = new HashSet<String>();
        private Set<String> m_expData, m_expErrors;
        private TimerTask m_launchTask = null;

        public ConcurentPipe(Node e) throws IOException, CourierException {
            super(e);
            init(
                StringHelper.toIntArray(
                    DomHelper.getNodeAttr(e, "launch-amounts", "1"), ','
                ),
                DomHelper.getLongNodeAttr(e, "launch-interval", 0),
                new HashSet<String>(DomHelper.getChildrenValues(
                    DomHelper.getChild(e, "expected-results"), "portion")),
                new HashSet<String>(DomHelper.getChildrenValues(
                    DomHelper.getChild(e, "expected-results"), "error"))
            );
        }

        private void init(
            int[] launchAmounts, long interval,
            Set<String> expData, Set<String> expErrors
        ) {
            m_launchAmounts = launchAmounts;
            m_interval = interval;
            m_expData = expData;
            m_expErrors = expErrors;
        }

        private boolean hasLaunchAmount() {
            return getLaunchNumber() < m_launchAmounts.length;
        }

        protected boolean hasNextPortion() {
            return hasLaunchAmount();
        }

        protected void customNextPortion() {
            if (m_launchTask != null) m_launchTask = null;
            System.out.println(
                "launchNextPortion: pipe = " + m_name
                + " portion = " + getLaunchNumber()
                + " size = " + m_launchAmounts[getLaunchNumber()]
            );
            for (int i = 0; i < m_launchAmounts[getLaunchNumber()]; i++) {
                registerProcess();
            }
            scheduleNextPortion();
        }

        public void scheduleNextPortion() {
            System.out.println(
                "launchNextPortion: scheduling next portion: pipe = " + m_name
                + " portion = " + getLaunchNumber()
                + " size = " + m_launchAmounts[getLaunchNumber()]
                + " in " + m_interval
            );
            m_launchTask = new TimerTask() { public void run() { launchNextPortion(); } };
            m_timer.schedule(m_launchTask, m_interval);
        }

        public final int getTotalLaunchAmount() {
            int res = 0;
            for (int m_launchAmount : m_launchAmounts) {
                res += m_launchAmount;
            }
            return res;
        }

        protected void customCheckResults() {
            if (m_expData != null) assertEquals(m_expData, m_data);

            if (m_expErrors != null) {
                assertEquals(m_expErrors, m_errors);
            }
        }

        private List<String> m_data = new LinkedList<String>();
        public void processData(String portion) {
            m_data.add(portion);
        }

        private List<String> convertSetToList(Set<String> set) {
            List<String> list = new LinkedList<String>();
            list.addAll(set);
            Collections.sort(list);
            return list;
        }

        private List<String> errorsToList() {
            return convertSetToList(m_errors);
        }

        protected String actualResultsToString() {
            StringWriter res = new StringWriter();
            actualResultsToString(res, "process");
            stringListToXml(errorsToList(), res, "error");
            return res.toString();
        }

        private void actualResultsToString(StringWriter buffer, String tagName) {
            buffer.write("\n<" + tagName + ">");
            stringListToXml(m_data, buffer, "portion");
            buffer.write("</" + tagName + ">");
        }

        public void actualResultsToString(StringWriter buffer) {
            buffer.write("\n<launch>");
            actualResultsToString(buffer, "process");
            buffer.write("</launch>");
        }

        protected String expectedResultsToString() {
            StringWriter res = new StringWriter();
            stringListToXml(convertSetToList(m_expData), res, "portion");
            stringListToXml(convertSetToList(m_expErrors), res, "error");
            return res.toString();
        }
    }

    protected class ConsecutivePipe extends Pipe {
        private int m_launchAmount;
        private List<List<ExpectedResults>> m_expectedResult;
        private final Map<Integer, Process> m_procs = new HashMap<Integer, Process>();
        private List<Integer> m_procOrder = new LinkedList<Integer>();
        private Integer m_curProcId = null;

        public ConsecutivePipe(Node e) throws IOException, CourierException {
            super(e);
            init(
                DomHelper.getIntNodeAttr(e, "launch-amount", 1),
                loadExpectedResuts(DomHelper.getChild(e, "expected-results", false))
            );
        }

        private List<List<ExpectedResults>> loadExpectedResuts(Node n) {
            if (n == null) return null;
            Element[] launches = DomHelper.getChildrenByTagName(n, "launch", false);
            List<List<ExpectedResults>> res = new LinkedList<List<ExpectedResults>>();
            for (Element lconf: launches) {
                List<ExpectedResults> lres = new LinkedList<ExpectedResults>();
                Element[] processes = DomHelper.getChildrenByTagName(lconf, "process", false);
                for (Element pconf: processes) {
                    List<String> presData = null;
                    Element dataConf = DomHelper.getChild(pconf, "data", false);
                    if (dataConf != null) {
                        Element[] portions = DomHelper.getChildrenByTagName(dataConf, "portion", false);
                        if (portions != null && portions.length > 0) {
                            presData = new LinkedList<String>();
                            for (Element p: portions) {
                                presData.add(DomHelper.getNodeValue(p));
                            }
                        }
                    }
                    List<String> errors = DomHelper.getChildrenValues(pconf, "error");
                    lres.add(new ExpectedResults(
                        errors.size() == 0 ? null : errors, presData,
                        DomHelper.getIntNodeAttr(pconf, "error-count", -1),
                        DomHelper.getNodeAttr(pconf, "interval-value", null)
                    ));
                }
                res.add(lres);
            }
            return res;
        }

        private void init(
            int launchAmount,
            List<List<ExpectedResults>> expectedResult
        ) {
            m_launchAmount = launchAmount;
            m_expectedResult = expectedResult;
        }

        public final int getTotalLaunchAmount() {
            return m_launchAmount;
        }

        public List<Process> getProcesses() {
            List<Process> ret = new LinkedList<Process>();
            ListIterator<Integer> procIdIt = m_procOrder.listIterator();
            while (procIdIt.hasNext()) {
                ret.add(m_procs.get(procIdIt.next()));
            }
            return ret;
        }

        protected void customCheckResults() {
            if (m_expectedResult == null) return;
            if (m_launchNumber >= m_expectedResult.size()) return;

            List<ExpectedResults> expProcesses = m_expectedResult.get(m_launchNumber);
            assertEquals(
                decorateAssertMes("process amounts differ"),
                expProcesses.size(), m_procOrder.size()
            );
            int pc = 1;
            ListIterator<ExpectedResults> expProcessIt = expProcesses.listIterator();
            ListIterator<Integer> procIdIt = m_procOrder.listIterator();
            while (expProcessIt.hasNext()) {
                String procDesc = decorateAssertMes(
                    "launch = " + m_launchNumber + " process = " + pc
                );
                ExpectedResults expResults = expProcessIt.next();
                Process proc = m_procs.get(procIdIt.next());

                List<String> exp = expResults.m_data;
                if (exp != null) {
                    List<String> act = proc.getData();
                    checkProcessDataResults(procDesc, exp, act);
                }

                if (expResults.m_errors != null) {
                    checkStringLists(
                        procDesc + " errors",
                        expResults.m_errors, proc.m_errors, "error"
                    );
                }

                if (expResults.getErrorCount() >= 0) {
                    assertEquals(
                        procDesc + " error count invalid",
                        expResults.getErrorCount(), proc.m_tp.getErrorCount());
                }

                if (expResults.getIntervalValue() != null) {
                    String iv = null;
                    try {
                        iv = proc.m_tp.getResults().getVar(
                            TransferProcess.c_intervalValueVarName);
                    } catch (CourierException e) {
                        fail("Could not get interval value");
                    }
                    assertEquals(
                        procDesc + " interval value invalid",
                        expResults.getIntervalValue(), iv);
                }

                pc++;
            }
        }

        private void checkProcessDataResults(
            String procDesc, List<String> exp, List<String> act
        ) {
            assertEquals(
                procDesc + ": portion amounts differ"
                  + "\n" + getResultsCompareDump() + "\n"
                ,
                exp.size(), act.size()
            );
            ListIterator<String> expPortionIt = exp.listIterator();
            ListIterator<String> actIt = act.listIterator();
            int i = 1;
            while (expPortionIt.hasNext()) {
                assertEquals(
                      procDesc
                    + " portion = " + i + ": target portions differ"
                    + "\n" + getResultsCompareDump() + "\n"
                    ,
                    expPortionIt.next(), actIt.next()
                );
                i++;
            }
        }

        protected boolean hasNextPortion() {
            return (getLaunchNumber() < m_launchAmount);
        }

        protected void customNextPortion() {
            registerProcess();
        }

        protected void customProcessStarted(Pipeline pipe, TransferProcess tp) {
            final Integer id = tp.getId();
            m_procOrder.add(id);
            m_procs.put(id, new Process(tp));
            if (m_curProcId != null) addPipeError(
                "Trying to set current proc id " + id +
                " while it is already not null " + m_curProcId
            );
            m_curProcId = id;
        }

        protected void customProcessFinished(Pipeline pipe, TransferProcess tp) {
            Process proc = m_procs.get(tp.getId());
            proc.m_stop = new Date();
            if (tp.getErrorCount() > 0) proc.addError(tp.getErrorText());
            m_curProcId = null;
            if (getWorkProcCount() == 0) launchNextPortion();
        }

        public void processData(String portion) {
            getCurProc().processData(portion);
        }

        protected String actualResultsToString() {
            StringWriter res = new StringWriter();
            Iterator<Integer> it = m_procOrder.listIterator();
            while (it.hasNext()) {
                Process proc = m_procs.get(it.next());
                res.write("<process>");
                stringListToXml(proc.getData(), res, "portion");
                if (proc.m_errors.size() > 0) res.write('\n');
                stringListToXml(proc.m_errors, res, "error");
                if (proc.m_errors.size() > 0) res.write('\n');
                res.write("</process>");
            }
            return res.toString();
        }

        protected String expectedResultsToString() {
            return expectedResultsToString(m_expectedResult.get(m_launchNumber));
        }

        private String expectedResultsToString(List<ExpectedResults> data) {
            StringWriter res = new StringWriter();
            expectedResultsToXml(data, res);
            return res.toString();
        }

        private void expectedResultsToXml(
            List<ExpectedResults> data, StringWriter buffer
        ) {
            for (ExpectedResults p: data) {
                buffer.write("<process>");
                stringListToXml(p.m_data, buffer, "portion");
                buffer.write("</process>");
            }
        }

        private Process getCurProc() {
            return m_procs.get(m_curProcId);
        }
    }

    private String applyCourierTemplate(String file, String[][] ctx)
    throws IOException, CourierException {
        return applyTemplate(getTemplateFile(file), ctx);
    }

    protected static class SimpleReceiver implements DataReceiver, ReceiverTimeCounter {
        private final Pipe m_pipe;
        private boolean m_active = true;

        public SimpleReceiver(Pipe pipe) {
            m_pipe = pipe;
        }

        public List<LinkWarning> process(Object operation) throws CourierException {
            m_pipe.customTargetProcess((String)operation);
            LineNumberReader lr = new LineNumberReader(new StringReader((String)operation));
            try {
                for(String line = lr.readLine(); line != null; line = lr.readLine()) {
                    if (line.charAt(0) == 'E') {
                        throw new CourierException("Test Error");
                    }
                }
            } catch (IOException e) {
                throw new CourierException(e);
            }
            return null;
        }
        public List<LinkWarning> flush() throws CourierException { return null; }
        public void clearTargetTime() {}
        public long getTargetTime() { return 0; }
        public void setTimeout(int timeout) throws CourierException {}
        public void cancel() throws CourierException {}
        public void close() throws CourierException {
            m_active = false;
        }
    }

    protected final synchronized void addError(String errorText) {
        m_errors.add(errorText);
    }

    private static final String cRuleName = "TestRule";
    private String getReceiverName(String pipeName) {
        return decoratePipeName(pipeName, "target-db");
    }

    private static String decoratePipeName(String pipeName, String name) {
        return pipeName + (name.length() > 0 ? "-" : "") + name;
    }

    protected class NullInitCustomizer implements InitCustomizer {
        public String getPipeOtherAttributes() {
            return "";
        }
        public String getPipeCustomTags() {
            return "";
        }
    }

    private void addPipe(Node e) throws IOException, CourierException {
        m_pipes.put(
            DomHelper.getNodeAttr(e, "name"),
            new ConsecutivePipe(e)
        );
    }

    private void loadPipes(Node conf) throws IOException, CourierException {
        Node pipesConf = DomHelper.getChild(conf, "pipes");
        for (Node n: DomHelper.getChildrenByTagName(pipesConf, "pipe", false)) {
            addPipe(n);
        }
    }

    private File getTemplateFile(String name) {
        return getDataFile("templates" + File.separator + name);
    }

    private String formConf() throws IOException, CourierException {
        StringBuffer pipeText = new StringBuffer();
        StringBuffer spText = new StringBuffer();
        StringBuffer tpText = new StringBuffer();
        StringBuffer dbText = new StringBuffer();
        for (Pipe pipe: m_pipes.values()) {
            pipeText.append('\n');
            pipeText.append(pipe.m_text);
            spText.append('\n');
            spText.append(pipe.m_sp);
            tpText.append('\n');
            tpText.append(pipe.m_tp);
            dbText.append('\n');
            dbText.append(pipe.m_db);
        }

        final String cLogDirName = "courier-log";
        final File testLogDir = getTempFile(cLogDirName);
        String configText = applyCourierTemplate(
            m_topTemplate,
            new String[][]{
                  {"log-dir", testLogDir.getAbsolutePath()}
                , {"attach-logger", c_attachLogger}
                , {"mock-db", applyCourierTemplate(m_mockDbTemplate,
                        new String[][]{}
                  )}
                , {"db-profiles", dbText.toString()}
                , {"source-profiles", spText.toString()}
                , {"target-profiles", tpText.toString()}
                , {"pipelines", pipeText.toString()}
            }
        );


        FileHelper.stringToFile(
            configText, getTempFile("dynamic-config-text.xml"), "cp1251"
        );

        return configText;
    }

    private int getTotalLaunchAmount() {
        int res = 0;
        for (final Pipe pipe: m_pipes.values()) {
            res += pipe.getTotalLaunchAmount();
        }
        return res;
    }

    protected Application getCourier() {
        return m_courier;
    }

    private void initCourier(Node conf)
    throws IOException, CourierException, SQLException {
        String mockdbFile = DomHelper.getNodeAttr(conf, "mock-db", false);
        Logger logger = Logger.getLogger("");
        m_mockdb = (mockdbFile == null) ?
            new MockDatabase(logger) :
            new MockDatabase(getDataFile(mockdbFile), logger);

        MockDatabase.setGlobalDatabase(m_mockdb);

        loadPipes(conf);
        String configText = formConf();
        m_courier = new Application(
            "CourierTest",
            new ByteArrayInputStream(configText.getBytes("cp1251")),
            getTempDir(""), getDataFile("distr"), false
        );
        m_courierLogHandler = new ConsoleHandler() {
            public void publish(LogRecord record) {
                //super.publish(record);
                if (record.getLevel().intValue() >= getLevel().intValue()) {
                    m_logData.add(record);
                }
            }
        };
        m_courierLogHandler.setLevel(Level.SEVERE);
        m_courierLogHandler.setFormatter(new SimpleFormatter());
        Logger.getLogger(c_attachLogger).addHandler(m_courierLogHandler);
        m_doneSignal = new CountDownLatch(getTotalLaunchAmount());
        m_courier.addListener(
            new CourierListener() {
                public void courierStarting() {}
                public void courierStopped() {}
                public void processStarted(Pipeline pipe, TransferProcess tp) {
                    Pipe p = m_pipes.get(pipe.getName());
                    p.processStarted(pipe, tp);
                }

                public void processFinished(Pipeline pipe, TransferProcess tp) {
                    Pipe p = m_pipes.get(pipe.getName());
                    p.processFinished(pipe, tp);
                }
            }
        );
        for (final Pipe pipe: m_pipes.values()) {
            m_courier.addDataReceiver(
                getReceiverName(pipe.m_name),
                new PoolObjectFactory() {
                    public Object getObject(ObjectPoolIntf pool) {
                        return pipe.getReceiver(getReceiverName(pipe.m_name));
                    }

                    public void returnObject(Object o) {
                        try {
                            ((SimpleReceiver)o).close();
                        } catch (CourierException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }


                    }

                    public boolean checkObject(Object o) {
                        return true;
                    }
                }
            );
        }
    }

    private void checkResults() {
        if (m_errors.size() > 0) {
            fail("Errors found:\n" + stringListToXml(m_errors, "error"));
        }
        for (Pipe p: m_pipes.values()) p.checkResults();
    }

    private void startPipes() {
        for (Pipe p: m_pipes.values()) {
            p.start();
        }
    }

    protected interface PipeListener {
        InitCustomizer getInitCustomizer();
        void onLaunchPortion(Pipe pipe);
        void onCourierStart();
        void processStarted(Pipe testPipe, Pipeline pipe, TransferProcess tp) throws Exception;
        void processFinished(Pipe testPipe, Pipeline pipe, TransferProcess tp) throws Exception;
        void customTargetProcess(Pipe testPipe, String request) throws CourierException;
        void check(Pipe testPipe);
    }

    protected class EmptyPipeListener implements PipeListener {
        public InitCustomizer getInitCustomizer() {
            return new NullInitCustomizer();
        }
        public void onLaunchPortion(Pipe pipe) {}
        public void onCourierStart() {}
        public void processStarted(
            Pipe testPipe, Pipeline pipe, TransferProcess tp) throws SQLException {}
        public void processFinished(
            Pipe testPipe, Pipeline pipe, TransferProcess tp) throws SQLException {}
        public void customTargetProcess(Pipe testPipe, String request) throws CourierException {}
        public void check(Pipe testPipe) {}

        protected String getIntervalValue(TransferProcess tp) throws CourierException {
            return tp.getIntervalValue();
        }

        protected MockTable getTable(String table) {
            return getMockDb().getTable(table);
        }

        protected void addRecordsFromData(String table, int count) throws SQLException {
            for (int i = 0; i < count; i++) {
                getTable(table).addRecordFromData();
            }
        }
    }

    protected final void launchTest(String fileName) throws CourierException {
        m_testName = fileName;
        Element conf = getDataXml(fileName);
        int launchCount = DomHelper.getIntNodeAttr(conf, "launch-count", 1);
        for (m_launchNumber = 0; m_launchNumber < launchCount; m_launchNumber++) {
            launchCourier(conf);
        }
    }

    protected final void stopTest() {
        m_stopped = true;
        while (m_doneSignal.getCount() > 0) {
            m_doneSignal.countDown();
        }
    }

    private void launchCourier(Node conf) throws CourierException {
        try {
            m_stopped = false;
            initCourier(conf);
            for (Map.Entry<String, PipeListener> e: m_customs.entrySet()) {
                if (m_pipes.containsKey(e.getKey())) {
                    m_pipes.get(e.getKey()).setListener(e.getValue());
                } else {
                    //fail("There is no pipe '" + e.getKey() + "' for custom handler");
                }
            }
            m_courier.start();
            startPipes();
            assertTrue(
                "Wait processes finish timeout expired",
                m_doneSignal.await(m_pipes.size()*600*1000, TimeUnit.MILLISECONDS)
            );
            m_courier.waitingStop(0, null, false);

            FileHelper.stringToFile(
                resultsToXmlString(),
                getTempFile(m_testName + "-results-" + m_launchNumber + ".out")
            );
        } catch (Exception e) {
            throw new CourierException(e);
        }
        checkResults();
        m_courier = null;
    }

    private String resultsToXmlString() {
        StringWriter resultsXml = new StringWriter(m_pipes.size()*128);
        for (Pipe p: m_pipes.values()) {
            resultsXml.write("\n<pipe name=\"" + p.m_name + "\">");
            resultsXml.write(p.actualResultsToString());
            resultsXml.write("\n</pipe>");
        }
        return resultsXml.toString();
    }

    //************************************************************************
    private static void stringListToXml(
        List<String> res, Writer out, String tagName
    ) {
        if (res == null) return;
        for (String portion: res) {
            try {
                out.write("\n<" + tagName + ">");
                out.write(portion);
                out.write("</"+ tagName +">");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String stringListToXml(List<String> res, String tagName) {
        StringWriter buffer = new StringWriter();
        stringListToXml(res, buffer, tagName);
        return buffer.toString();
    }

    private static void checkStringLists(
        String mesPrefix, List<String> exp, List<String> act, String tagName
    ) {
        assertEquals(
            mesPrefix + ": portion amounts differ"
            + "\n" + getStringListsCompareDump(exp, act, tagName) + "\n"
            ,
            exp.size(), act.size()
        );
        ListIterator<String> expPortionIt = exp.listIterator();
        ListIterator<String> actIt = act.listIterator();
        int i = 1;
        while (expPortionIt.hasNext()) {
            assertEquals(
                  mesPrefix
                + " portion = " + i + ": target portions differ"
                + "\n" + getStringListsCompareDump(exp, act, tagName) + "\n"
                ,
                expPortionIt.next(), actIt.next()
            );
            i++;
        }
    }

    private static String getStringListsCompareDump(
        List<String> exp, List<String> act, String tagName
    ) {
        return (
              "\n------------ EXPECTED ---------------------\n"
            + stringListToXml(exp, tagName)
            + "\n------------ ACTUAL -----------------------\n"
            + stringListToXml(act, tagName)
            + "\n-------------------------------------------\n"
        );
    }
    //************************************************************************

    private static final String c_pipeTemplate = "pipeline-template.xml";
    private static final String c_sourceProfileTemplate = "source-profile-template.xml";
    private static final String c_sourceRuleTemplate = "source-rule-template.xml";
    private static final String c_targetProfileTemplate = "target-profile-template.xml";
    private static final String c_topTemplate = "top-template.xml";
    private static final String c_mockDbTemplate = "mock-db-template.xml";

    protected String m_pipeTemplate;
    protected String m_sourceProfileTemplate;
    protected String m_sourceRuleTemplate;
    protected String m_targetProfileTemplate;
    protected String m_topTemplate;
    protected String m_mockDbTemplate;

    protected void courierSetUp() {
        setDeleteTempDir(true);
        m_pipes = new HashMap<String, Pipe>();
        m_customs = new HashMap<String, PipeListener>();
        m_timer = new Timer();
        m_pipeTemplate = c_pipeTemplate;
        m_sourceProfileTemplate = c_sourceProfileTemplate;
        m_sourceRuleTemplate = c_sourceRuleTemplate;
        m_targetProfileTemplate = c_targetProfileTemplate;
        m_topTemplate = c_topTemplate;
        m_mockDbTemplate = c_mockDbTemplate;
    }

    protected void courierTearDown() {
        m_timer.cancel();
        m_timer = null;
        m_customs = null;
        m_pipes = null;
        Logger.getLogger(c_attachLogger).removeHandler(m_courierLogHandler);
    }

    protected final MockDatabase getMockDb() {
        return m_mockdb;
    }

    protected final int getLaunchNumber() {
        return m_launchNumber;
    }

    protected final void addListener(String name, PipeListener l) {
        m_customs.put(name, l);
    }

    protected final String errorsToString() {
        return stringListToXml(m_errors, "error");
    }
}
