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

import org.apache.mina.common.ExceptionMonitor;
import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import ru.rd.courier.jdbc.mock.MockDatabase;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.LogProvider;
import ru.rd.courier.logging.LoggerAdapter;
import ru.rd.courier.logging.SameDiscardLogHandler;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.manager.ManagerClient;
import ru.rd.courier.manager.message.CourierInfoMessage;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.courier.schedule.*;
import ru.rd.courier.scripting.*;
import ru.rd.courier.scripting.statements.ObjectStatementCaller;
import ru.rd.courier.utils.*;
import ru.rd.courier.datalinks.*;
import ru.rd.pool.*;
import ru.rd.pool.jdbc.ConnectionPool;
import ru.rd.pool2.DefaultObjectPool2;
import ru.rd.pool2.ObjectPool2;
import ru.rd.scheduling.leveled.*;
import ru.rd.thread.ThreadFactory;
import ru.rd.thread.WorkThread;
import ru.rd.utils.Disposable;
import ru.rd.utils.SafeSystemRunnable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Logger;

public final class Application implements CourierContext, ProcedureProvider {
    private LoggerAdapter m_logger;
    private String m_state;
    private String m_code;
    private File m_appDir;
    private ResourceProvider m_sysres;
    private LogProvider m_logProvider;
    private ScriptStatement m_stdSourceStmt;
    private ScriptStatement m_stdPipesStmt;
    private ScriptStatement m_commonStmt;
    private ScriptContext m_commonContext;
    private Map<String, ObjectPoolIntf> m_dbPools = new HashMap<String, ObjectPoolIntf>();
    private Map<String, String> m_receiverTypes = new HashMap<String, String>();
    private Map<String, Map<String, String>> m_driverInfos = new HashMap<String, Map<String, String>>();
    private Properties m_params;
    private Properties m_scriptParams;
    private SystemDb m_sysDb = null;
    private XmlStatementFactory m_stmtFactory = null;
    private Map<String, SourceProfile> m_sProfiles = new HashMap<String, SourceProfile>();
    private Map<String, TargetProfile> m_tProfiles = new HashMap<String, TargetProfile>();
    private Map<String, Pipeline> m_pipes = new HashMap<String, Pipeline>();
    private StartStopListenerSet m_scheduler = null;
    private ObjectPoolIntf m_threadPool = null;
    private Timer m_timer = null;
    private Collection<CourierListener> m_listeners = new LinkedList<CourierListener>();
    private CountDownLatch m_stopSignal;
    private Thread m_shutdownHook;
    private AccountProvider m_aprovider = null;

    public static interface ResourceProvider {
        InputStream getResource(String name) throws Exception;
    }

    public static class FolderResourceProvider implements ResourceProvider {
        private final File m_folder;

        public FolderResourceProvider(File folder) {
            m_folder = folder;
        }

        public InputStream getResource(String name) throws Exception {
            return new FileInputStream(new File(m_folder, name));
        }
    }

    public static class ClassLoaderResourceProvider implements ResourceProvider {
        private final ClassLoader m_cl;
        private final String m_package;

        public ClassLoaderResourceProvider(ClassLoader cl, String packageName) {
            m_cl = cl;
            m_package = packageName;
        }

        public InputStream getResource(String name) throws Exception {
            String rname = m_package + "/" + name;
            return m_cl.getResourceAsStream(rname);
        }
    }

    private static final String c_PoolLoggerName = "pool";

    private static final String c_initState = "INIT";
    private static final String c_stoppedState = "STOPPED";
    private static final String c_startingState = "STARTING";
    private static final String c_runningState = "RUNNING";
    private static final String c_stoppingState = "STOPPING";

    public Application(
        String code, final InputStream confData, File defaultAppDir, boolean needToCheckDatabases
    ) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String rname = getClass().getPackage().getName().replace('.', '/') + "/resources";
        ResourceProvider sysres = new ClassLoaderResourceProvider(cl, rname);
        init(code, confData, defaultAppDir, sysres, needToCheckDatabases);
    }

    public Application(
        String code, InputStream conf, File defaultAppDir, File systemDir,
        boolean needToCheckDatabases
    ) throws CourierException {
        init(code, conf, defaultAppDir, systemDir, needToCheckDatabases);
    }

    public Application(final String confFileName, File systemDir) throws CourierException {
        init(confFileName, systemDir);
    }

    public Application(File config) throws FileNotFoundException {
        this(config.getAbsoluteFile().getPath(), new FileInputStream(config), config.getParentFile(), false);
    }

    public static void main(final String[] args) {
        if (args.length > 0) {
            try {
                final CourierContext appl = new Application(
                    args[0], args.length > 1 ? new File(args[1]) : null
                );
                appl.start();
            } catch (CourierException e) {
                e.printStackTrace();
            }
        } else {
            System.exit(1);
        }
    }

    public final void addListener(CourierListener l) {
        m_listeners.add(l);
    }

    final Collection<CourierListener> getListeners() {
        return m_listeners;
    }

    public final boolean hasPool(final String dbProfileName) {
        return getObjectPool(dbProfileName) != null;
    }

    private NamedConfigProvider m_dbsConf;

    public ObjectPoolIntf getObjectPool(String name) {
        name = name.toUpperCase();
        ObjectPoolIntf op = m_dbPools.get(name);
        if (op == null && m_dbsConf != null) {
            try {
                Element conf = m_dbsConf.getNamedConfig(name);
                if (conf != null) {
                    op = initDbProfile(false, conf);
                    op.start();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return op;
    }

    public final PooledObjectHolder getPooledObject(final String dbProfileName) throws CourierException {
        final ObjectPoolIntf p = getObjectPool(dbProfileName);
        if (p == null) {
            throw new CourierException("There is no pool '" + dbProfileName + "'");
        }
        return new ReleaseHelper(p);
    }

    public final String getPooledObjectHost(String name) {
        final ObjectPoolIntf p = getObjectPool(name);
        if (p == null) return null;
        PoolObjectFactory pof = p.getObjectFactory();
        if (pof instanceof HostProvider) {
            return ((HostProvider)pof).getHost();
        }
        return null;
    }

    public boolean hasSourceProfile(String name) {
        return m_sProfiles.containsKey(name);
    }

    public SourceProfile getSourceProfile(final String name) throws CourierException {
        SourceProfile ret = m_sProfiles.get(name);
        if (ret == null) {
            throw new CourierException("Source profile '" + name + "' not found");
        }
        return ret;
    }

    public boolean hasTargetProfile(String name) {
        return m_tProfiles.containsKey(name);
    }

    public TargetProfile getTargetProfile(String name) throws CourierException {
        TargetProfile tp = m_tProfiles.get(name);
        if (tp == null) {
            throw new CourierException("Target profile '" + name + "' not found");
        }
        return tp;
    }

    public boolean hasPipeline(final String name) {
        return m_pipes.containsKey(name);
    }

    public Pipeline getPipeline(final String name) throws CourierException {
        Pipeline ret = m_pipes.get(name);
        if (ret == null) {
            throw new CourierException("Pipeline '" + name + "' not found");
        }
        return ret;
    }

    public XmlStatementFactory getStmtFactory() {
        return m_stmtFactory;
    }

    public ObjectPoolIntf getThreadPool() {
        return m_threadPool;
    }

    public Timer getTimer() {
        if (m_timer == null) {
            m_timer = new Timer("CourierMainTimer");
        }
        return m_timer;
    }

    public synchronized SystemDb getSystemDb() {
        return m_sysDb;
    }

    public void setSysdbStorage(Storage factory) {
        getSystemDb().setStorage(factory);
    }

    public ScriptStatement getSourceTopStatement() {
        return m_stdSourceStmt;
    }

    public ScriptStatement getPipesTopStatement() {
        return m_stdPipesStmt;
    }

    public Document parseXmlFile(final File f)
    throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder parser;
        parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return parser.parse(f);
    }

    public Document parseXml(final InputStream data)
    throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder parser;
        parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return parser.parse(data);
    }

    public Document parseSysXml(String name) throws Exception {
        return parseXml(getSysResource(name));
    }

    private File fileToAbsForm(File file) {
        if (file.isAbsolute()) return file;
        return file.getAbsoluteFile();
    }

    private void init(final String confFileName, File systemDir) throws CourierException {
        try {
            File confFile = new File(confFileName);
            if (!confFile.exists()) {
                throw new CourierException(
                    "Conf. file '" + confFileName + "' does not exist"
                );
            }

            confFile = fileToAbsForm(confFile);
            init(
                confFile.getPath(),
                new FileInputStream(confFile), confFile.getParentFile(),
                systemDir, true
            );
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public void start(Context ctx) throws CourierException {}
    public void finish(Context ctx) throws CourierException {}
    public ScriptProcedure getProcedure(String name) throws CourierException {
        if (name.equals(getScriptParam("courier-stop-stmt-name"))) {
            return new ScriptProcedure() {
                public void start(Context ctx) {}
                public void finish(Context ctx) {}
                public void exec(Context ctx, Object[] pars) {
                    String timeout = "0";
                    Integer status = null;
                    if (pars.length > 0) timeout = (String)pars[0];
                    if (pars.length > 1) status = (Integer)pars[1];

                    stop(StringHelper.parseTime(timeout, "s"), status);
                }
            };
        }
        return null;
    }

    private static class XslErrorListener implements ErrorListener {
        public void warning(TransformerException exception) {
            printError(exception);
        }

        public void error(TransformerException exception) {
            printError(exception);
        }

        public void fatalError(TransformerException exception) {
            printError(exception);
        }

        private void printError(TransformerException exception) {
            System.out.println(exception.getMessageAndLocation());
        }
    }

    private Document xslTransform(Document xml, File xsl) throws TransformerException {
        return xslTransform(xml, new StreamSource(xsl));
    }

    private Document xslTransform(Document xml, Source xsl) throws TransformerException {
        final TransformerFactory tFactory = new TransformerFactoryImpl();
        Transformer transformer = tFactory.newTransformer(xsl);
        transformer.setParameter("app-dir", m_appDir.getAbsolutePath());
        transformer.setErrorListener(new XslErrorListener());
        DOMResult confResult = new DOMResult();
        transformer.transform(new DOMSource(xml), confResult);
        return (Document)confResult.getNode();
    }

    private void init(
        String code, final InputStream confData, File defaultAppDir, File sysdir,
        boolean needToCheckDatabases
    ) throws CourierException {
        init(code, confData, defaultAppDir, new FolderResourceProvider(sysdir), needToCheckDatabases);
    }

    private void init(
        String code, final InputStream confData, File defaultAppDir, ResourceProvider sysres,
        boolean needToCheckDatabases
    ) throws CourierException {
        m_state = c_stoppedState;
        m_sysres = sysres;
        m_logger = null;
        m_code = code;
        try {
            Document confDoc;
            try {
                //confDoc = parseXml(confData);
                confDoc = DomHelper.parseStream(confData);
            } catch (Exception e) {
                throw new CourierException("Unable to parse configuration file", e);
            }
            Element root = confDoc.getDocumentElement();

            final String appDirName = DomHelper.getNodeAttr(root, "app-dir", false);
            File appDir = (appDirName == null) ? defaultAppDir : new File(appDirName);

            if (!appDir.exists()) {
                throw new CourierException(
                    "Application catalog not found - " + m_appDir.getAbsolutePath()
                );
            }
            m_appDir = fileToAbsForm(appDir);
            System.setProperty("CourierAppDir", m_appDir.getAbsolutePath());

            final String sysConfFilename = DomHelper.getNodeAttr(root, "sys-config", null);
            InputStream sysConf;
            if (sysConfFilename == null) {
                if (m_sysres == null) throw new RuntimeException("Failed to determine system dir");
                sysConf = m_sysres.getResource("sys-config.xml");
            } else {
                File sysConfFile = getAppFile(sysConfFilename);
                sysConf = new FileInputStream(sysConfFile);
                m_sysres = new FolderResourceProvider(sysConfFile.getParentFile());
            }

            AppConfAssembler confHelp = new AppConfAssembler(
                new AppConfAssembler.FileResolver() {
                    public File getFile(String name) { return getAppFile(name); }
                }, "COURIER_DEFAULT_CONFIG"
            );
            confHelp.resolve(getSysResource("base-structure.xml"), root);

            Node customXslNode = DomHelper.getChild(root, "custom-xsl", false);
            if (customXslNode != null) {
                Document custDoc = parseSysXml("custom-tmpl.xsl");
                //DomHelper.importChildren(customXslNode, custDoc.getDocumentElement());
                DomHelper.importChildrenInstead(customXslNode, custDoc.getDocumentElement(), "insert-custom-xsl");
                ////custDoc.getDocumentElement().appendChild(custDoc.importNode(customXslNode, true));
                root.removeChild(customXslNode);
                //serializeConf(custDoc, getAppFile("custom____.xsl").getAbsolutePath());
                DOMSource ds = new DOMSource(custDoc);
                ds.setSystemId("file:///" + getAppFile("custom-config.xsl").getAbsolutePath());
                confDoc = xslTransform(confDoc, ds);
            } else {
                String customXsl = DomHelper.getNodeAttr(root, "custom-xsl", false);
                if (customXsl != null) {
                    confDoc = xslTransform(confDoc, getAppFile(customXsl));
                }
            }

            //confDoc = xslTransform(confDoc, getSysFile("resolve-named-tags.xsl"));
            root = confDoc.getDocumentElement();
            confHelp.resolve(null, root);
            String assembledConf = DomHelper.getNodeAttr(root, "transformed-conf-file", false);
            if (assembledConf != null) {
                serializeConf(confDoc, getAppFile(assembledConf).getAbsolutePath());
            }


            m_logProvider = new LogProvider(this, DomHelper.getChild(root, "logging"));
            m_logger = m_logProvider.getLogger("");

            try {
                m_state = c_initState;
                initWithLogging(confDoc, sysConf, needToCheckDatabases);
            } catch (Throwable e) {
                m_logger.error("Initialization error", e);
                stop(1000, 1);
                throw new CourierException(e);
            }

            // !!!! reconsider
            for (Handler h: m_logger.getInnerLogger().getHandlers()) {
                if (h instanceof SameDiscardLogHandler) {
                    SameDiscardLogHandler mh = (SameDiscardLogHandler)h;
                    if (mh.getSameMessageBufferSize() < 1) {
                        mh.setSameMessageBufferSize(m_pipes.size() * 20);
                    }
                }
            }
            m_state = c_stoppedState;
        } catch (Throwable e) {
            throw new CourierException(e);
        }
    }

    /*
    private static Set substractSets(Set from, Set what) {
        Set res = new HashSet(from);
        res.removeAll(what);
        return res;
    }

    private static void incCounter(Map<String, Integer> counters, String name) {
        if (counters.containsKey(name)) {
            counters.put(name, counters.get(name) + 1);
        } else {
            counters.put(name, 1);
        }
    }
    */

    private void callInitBlocks(Node conf) throws CourierException {
        if (conf == null) return;
        try {
            for (Node n : DomHelper.getChildrenByTagName(conf, "call", false)) {
                Class cl = Class.forName(DomHelper.getNodeAttr(n, "class"));
                Method m = cl.getMethod(
                    DomHelper.getNodeAttr(n, "method"), Application.class, Node.class
                );
                m.invoke(null, this, n);
            }
        } catch (Exception e) {
            throw new CourierException(e);
        }
    }

    private void initWithLogging(
        Document confDoc, InputStream sysConf, boolean needToCheckDatabases
    ) throws Throwable {
        Element root = confDoc.getDocumentElement();
        callInitBlocks(DomHelper.getChild(root, "init-calls", false));
        m_scheduler = new StartStopContainer("Courier schedule", getStdLogger());

        long t = System.currentTimeMillis();
        final TransformerFactory tFactory = new TransformerFactoryImpl();
        Transformer transformer;
        DOMResult confResult;

        Document sysConfDoc = parseXml(sysConf);
        Source xsl = new StreamSource(getSysResource("config.xsl"));
        transformer = tFactory.newTransformer(xsl);
        confResult = new DOMResult();
        transformer.setParameter("app-dir", m_appDir.getAbsolutePath());
        transformer.setParameter("system-conf", sysConfDoc);
        transformer.setErrorListener(new XslErrorListener());
        transformer.transform(new DOMSource(confDoc), confResult);

        m_logger.info("XSL transform time = " + (System.currentTimeMillis() - t));

        root = ((Document)confResult.getNode()).getDocumentElement();
        String assembledConf = DomHelper.getNodeAttr(root, "assembled-conf-file", false);
        if (assembledConf != null) {
            serializeConf(confResult.getNode(), getAppFile(assembledConf).getAbsolutePath());
        }

        m_params = new Properties();
        m_params.putAll(DomHelper.getAttrParams(root));

        Node sysRoot = sysConfDoc.getDocumentElement();
        initSystemServices(sysRoot);

        initSchedules(root);

        m_stdSourceStmt = getStmtFactory().getStatement(
            DomHelper.getChild(root, "source-main-statement").getFirstChild(), null
        );

        m_stdPipesStmt = getStmtFactory().getStatement(
            DomHelper.getChild(root, "pipes-main-statement").getFirstChild(), null
        );

        Node startScriptNode = DomHelper.getChild(root, "start-script", false);
        if (startScriptNode == null) {
            m_commonStmt = null;
        } else {
            m_commonStmt = getStmtFactory().getStatement(startScriptNode.getFirstChild(), null);
        }

        final String commonDatabases = DomHelper.getNodeAttr(root, "common-databases", false);
        if (commonDatabases == null) {
            m_dbsConf = null;
        } else {
            m_dbsConf = new ChunkNamedConfigProvider(
                m_logger, getTimer(), getThreadPool(), new File(commonDatabases), null, "name");
        }

        m_skipUnusedDb = DomHelper.getBoolYesNo(root, "skip-unused-pools", false);
        m_checkUsedDb = DomHelper.getBoolYesNo(root, "check-used-databases", false);

        initAccountProvider(root);
        initManagerClient(sysRoot, root);
        initDbProfiles(getChild(root, "db-profiles"));
        initTargetProfiles(getChild(root, "target-profiles"));
        initSourceProfiles(getChild(root, "source-profiles"));
        initPipelines(getChild(root, "pipelines"));
        initScheduler(root);
        if (needToCheckDatabases) checkDatabases(root);
    }

    static final String ACCOUNTS_CRYPT = "DefaultRdCourier";

    private void initAccountProvider(Node conf) throws IOException, SAXException {
        XmlFileAccountProvider ret = null;

        String cfs = DomHelper.getNodeAttr(conf, "common-accounts-file", null);
        if (cfs != null) {
            File cf = getAppFile(cfs);
            if (!cf.exists()) throw new RuntimeException("Common Accounts file '" + cf.getPath() + "' does not exist");
            ret = new XmlFileAccountProvider();
            ret.addAccountsFromXml(cf, ACCOUNTS_CRYPT);
        }

        File f = null;
        String fs = DomHelper.getNodeAttr(conf, "accounts-file", null);
        if (fs != null) {
            f = getAppFile(fs);
            if (!f.exists()) throw new RuntimeException("Accounts file '" + f.getPath() + "' does not exist");
        }

        if (f == null) {
            f = getAppFile("WestDoor");
            if (!f.exists()) f = null;
        }
        if (f != null) {
            if (ret == null) {
                ret = new XmlFileAccountProvider();
            }
            ret.addAccountsFromXml(f, ACCOUNTS_CRYPT);
        }
        m_aprovider = ret;
    }

    private class ManagerClientListener implements CourierListener {
        private final ManagerClient m_client;

        public ManagerClientListener(ManagerClient client) {
            m_client = client;
        }

        public void processStarted(Pipeline pipe, TransferProcess tp) {}
        public void processFinished(Pipeline pipe, TransferProcess tp) {
            ProcessResult pr = new ProcessResult();
            pr.setId(tp.getId());
            pr.setPipe(pipe.getName());
            pr.setRecordCount(tp.getRecordCount());
            pr.setErrorCount(tp.getErrorCount());
            pr.setError(tp.getErrorText());
            pr.setErrorStack(tp.getErrorStack());

            String url;
            String name;
            PoolObjectFactory f;

            name = tp.getSourceDbName();
            pr.setSourceDbName(name);
            f = tp.getPoolFactory(name);
            pr.setSourceDbType(f.getClass().getSimpleName());
            if (f instanceof JdbcObjectFactory) {
                url = ((JdbcObjectFactory)f).getUrl();
            } else {
                url = null;
            }
            pr.setSourceDbUrl(url);

            name = tp.getTargetDbName();
            pr.setTargetDbName(name);
            f = tp.getPoolFactory(name);
            pr.setTargetDbType(f.getClass().getSimpleName());
            if (f instanceof JdbcObjectFactory) {
                url = ((JdbcObjectFactory)f).getUrl();
            } else {
                url = null;
            }
            pr.setTargetDbUrl(url);

            pr.setStartTime(tp.getStartTime());
            pr.setTotalTime(tp.getTotalTime());
            pr.setSourceTime(tp.getSourceTime());
            pr.setTargetTime(tp.getTargetTime());

            m_client.sendStat(pr);
        }

        public void courierStarting() throws NoSuchMethodException {
            m_client.start();
            m_client.shouldBeStarted();
        }

        public void courierStopped() throws Exception {
            m_client.stop(m_client.getTimeout());
            //Thread.sleep(1);
        }
    }

    private static final String cManagerDefaultHost = "universe1";
    private static final int cManagerDefaultPort = 4448;
    private static final String cManagerTag = "manager";

    private void initManagerClient(Node sconf, Element rconf) {
        Element conf = DomHelper.getChild(rconf, cManagerTag, false);
        if (conf == null) {
            conf = DomHelper.getChild(sconf, cManagerTag, false);
            if (conf == null) {
                conf = rconf.getOwnerDocument().createElement(cManagerTag);
            }
        }
        if (DomHelper.getBoolYesNo(conf, "cancel")) return;
        final CourierLogger logger = m_logProvider.getLogger("manager");
        ExceptionMonitor.setInstance(new ExceptionMonitor() {
            public void exceptionCaught(Throwable cause) {
                logger.warning(cause);
            }
        });
        ManagerClient mclient = new ManagerClient(
            logger, getThreadPool(), getTimer(),
            DomHelper.getNodeAttr(conf, "host", cManagerDefaultHost),
            DomHelper.getIntNodeAttr(conf, "port", cManagerDefaultPort),
            new CourierInfoMessage(m_code)
        );
        mclient.setDesc("ManagerClient");
        mclient.setTimeout(DomHelper.getTimeNodeAttr(conf, "timeout", 5000));
        mclient.setCheckInterval(DomHelper.getTimeNodeAttr(conf, "try-connect-interval", 5000));
        mclient.setMaxCount(DomHelper.getIntNodeAttr(conf, "max-count", 1000));
        mclient.setToLogState(DomHelper.getBoolYesNo(conf, "log-state", false));

        mclient.setStatCheckInterval(DomHelper.getTimeNodeAttr(conf, "stat-check-interval", 5000));
        mclient.setStatBufferInterval(DomHelper.getTimeNodeAttr(conf, "stat-buffer-interval", 30000));

        addListener(new ManagerClientListener(mclient));
    }

    private void initScheduler(Element root) {
        final StartStopListener is = new StartStopListener() {
            public void start(Date parentStart) {
                try { startPipelines(); }
                catch (CourierException e) { throw new RuntimeException(e); }
            }

            public void stop() {
                for(Pipeline p: m_pipes.values()) {
                    try { p.stop(); }
                    catch(Throwable e) { m_logger.error(e); }
                }
            }
        };

        Element scheduleConf = DomHelper.getChild(
            getChild(root, "pipelines"), "schedule", false);

        if (scheduleConf != null) {
            ScheduleInitializer scheduleInit = new ScheduleInitializer("pipe-schedule") {
                protected final Logger getLogger() { return getStdLogger(); }
                protected final Timer getTimer() { return Application.this.getTimer(); }
                protected StartStopListener createInnerSchedule(Node conf) { return is; }
                protected Runnable createWork(Node conf, StartStopSet parent) { return null; }
            };

            scheduleInit.initScheduleLevel(scheduleConf, m_scheduler);
        } else {
            m_scheduler.addListener(is);
        }
    }

    private void checkDatabases(Node root) throws CourierException {
        Map<String, Integer> dbCounters = getDbCounters();
        boolean enablePoolsRestrict = DomHelper.getBoolYesNo(
            root, "enable-database-pools-restrictions"
        );

        for (Map.Entry<String, ObjectPoolIntf> poolEntry: m_dbPools.entrySet()) {
            if (dbCounters.containsKey(poolEntry.getKey())) {
                if (enablePoolsRestrict) {
                    int mcap = dbCounters.get(poolEntry.getKey());
                    ObjectPoolIntf pool = poolEntry.getValue();
                    if (pool instanceof ObjectPool) {
                        ((ObjectPool)pool).setMaxCapacity(mcap);
                    } else {
                        ((ObjectPool2)pool).setMaxCapacity(mcap);
                    }
                }
            }
        }

        /*
        Set<String> unusedDatabases = new HashSet<String>();
        unusedDatabases.addAll(m_dbPools.keySet());
        unusedDatabases.removeAll(dbCounters.keySet());
        if (unusedDatabases.size() > 0) {
            m_logger.info("Removing unused databases: " + unusedDatabases);
            for (String dbName: unusedDatabases) {
                if (m_dbPools.containsKey(dbName)) {
                    m_dbPools.remove(dbName);
                }
            }
        }

        Set undefinedDatabases = dbCounters.keySet();
        undefinedDatabases.removeAll(m_dbPools.keySet());
        if (undefinedDatabases.size() > 0) {
            throw new CourierException(
                "Undefined databases: " + undefinedDatabases);
        }
        */
    }

    private void ensureState(String state) {
        if (m_state != state) {
            throw new RuntimeException(
                "Illegal state: current = " + m_state + "expected = " + state
            );
        }
    }

    private void startPipelines() throws CourierException {
        Map<String, Set<String>> unstartedPipes =
            new HashMap<String, Set<String>>();
        for(Pipeline pipe: m_pipes.values()) {
            boolean hasUndefDb = false;
            Set<String> undefDbs = null;
            if (m_checkUsedDb) {
                undefDbs = getUndefinedDbs(pipe.getUsedDbNames());
                hasUndefDb = !undefDbs.isEmpty();
            }
            if (pipe.isDisabled() || hasUndefDb) {
                unstartedPipes.put(pipe.getName(), hasUndefDb ? undefDbs : null);
            } else {
                try {
                    pipe.start();
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Error starting pipeline '" + pipe.getName() + "'", e
                    );
                }
            }
        }
        if (unstartedPipes.size() > 0) {
            StringBuffer sb = new StringBuffer();
            for (Map.Entry<String, Set<String>> ent: unstartedPipes.entrySet()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(ent.getKey());
                if (ent.getValue() != null) {
                    sb.append(": undefined databases - ").append(ent.getValue());
                }
            }
            m_logger.error(
                "Pipelines could not be started\n" + sb
            );
        }
    }

    public AbstractContext getCommonContext() {
        return m_commonContext;
    }

    public AccountProvider getAccountProvider() {
        return m_aprovider;
    }

    public synchronized void start() {
        ensureState(c_stoppedState);
        m_state = c_startingState;

        try {
            //m_threadPool.start();
            for(ObjectPoolIntf pool: m_dbPools.values()) pool.start();

            m_commonContext = new ScriptContext(this, getDatabaseDateFormat());
            if (m_commonStmt != null) m_commonContext.exec(m_commonStmt);

            register();

            getSystemDb().start();
            m_hostSchedules.start();
            m_mainPipeSchedules.start();
            m_state = c_runningState;
            for (CourierListener l: getListeners()) l.courierStarting();
            m_scheduler.start(new Date());

            m_shutdownHook = new Thread(){
                public void run() {
                    try {
                        if (m_shutdownHook == null) return;
                        m_shutdownHook = null;
                        Runtime.getRuntime().removeShutdownHook(this);
                        Application.this.stop(15*1000, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(m_shutdownHook);

            WorkThread kbThread = new WorkThread(m_logger, "Keyboard Listener", true);
            kbThread.setDaemon(true);
            kbThread.start();
            kbThread.launchWork(new Runnable() {
                public void run() {
                    try { handleProcessInput(); }
                    catch (Exception e) { m_logger.error(e); }
                }
            });

            WorkThread fileThread = new WorkThread(m_logger, "Command File Listener", true);
            fileThread.setDaemon(true);
            fileThread.start();
            fileThread.launchWork(new Runnable() {
                public void run() {
                    try { handleCommandFile(); }
                    catch (Exception e) { m_logger.error(e); }
                }
            });

            m_logger.info("Courier  (" + Version.getDesc() + ")  started");
        } catch(Exception e) {
            m_logger.error(e);
            try { stop(); } catch(Throwable e1) { m_logger.error(e1); }
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static String mapToString(Map<?,?> map) {
        StringBuffer buf = new StringBuffer();
        for (Map.Entry e: map.entrySet()) {
            buf.append(e.getKey().toString());
            buf.append('=');
            buf.append(e.getValue().toString());
            buf.append('\n');
        }
        return buf.toString();
    }

    private void handleProcessInput() throws CourierException, IOException, ParseException {
        LineReader lr = new LineReader(new InputStreamReader(System.in));
        StringSimpleParser p = new StringSimpleParser();
        while(true) {
            long br = lr.getBytesRead();
            String line = lr.readLine();
            if (br == lr.getBytesRead()) break;
            p.setText(line);
            if (!processCommand(p)) break;
        }
    }

    private void handleCommandFile() throws CourierException, InterruptedException, IOException {
        String filePath = getParam("console-command-file", false);
        long checkInterval = StringHelper.parseTime(getParam("console-command-file-check-interval", "5s"));
        StringSimpleParser p = new StringSimpleParser();
        while(filePath != null) {
            File file = getAppFile(filePath);            
            Thread.sleep(checkInterval);
            //System.out.println("console-command-file: " + filePath);
            //System.out.println("console-command-file-check-interval: " + String.valueOf(checkInterval));
            
            if (file.exists()){
                RandomAccessFile in = new RandomAccessFile(file, "rw");
                try {
                    java.nio.channels.FileLock lock = in.getChannel().lock();
                    try {
                        String line = null;
                        while ((line = in.readLine()) != null) {
                            p.setText(line);
                            if (!processCommand(p)) break;
                        }
                    } finally {
                        lock.release();
                    }
                } catch(Exception e){
                    m_logger.warning(e);
                } finally {
                    in.close();
                }
                file.delete();
            }
        }
    }

    private boolean processCommand(StringSimpleParser p) throws CourierException, IOException, ParseException{
        String cmd = p.shiftWord().toLowerCase();
        p.skipBlanks();
        if (cmd.equals("stop")) {
            long timeout = 15*1000;
            if (!p.beyondEnd()) {
                String timeoutStr = p.shiftDigits();
                try { timeout = StringHelper.parseTime(timeoutStr, "s"); }
                catch (Exception e) { m_logger.error(e); }
            }
            stop(timeout, null);
            return false;
        } else if (cmd.equals("launch")) {
            String pipe = p.shiftWordOrBracketedString('\'');
            p.skipBlanks();
            String rule = p.shiftWordOrBracketedString('\'');
            getSystemDb().registerProcessRequest(
                new TransferRequest(pipe, rule, 0, null, null, new Pipeline.StdProcessFactory(null))
            );
        } else if (cmd.equals("version")) {
            System.out.println(Version.getDesc());
        } else if (cmd.equals("threads")) {
            PrintStream out;
            boolean needToClose;
            if (p.beyondEnd()) {
                out = System.out;
                needToClose = false;
            } else {
                String file = p.shiftWordOrBracketedString('\'');
                out = new PrintStream(new FileOutputStream(file));
                needToClose = true;
            }
            String[] spoiledThreadsInfo = WorkThread.getHangingThreadsInfo();
            out.println("Spoiled threads:");
            out.println("  discarded info count = " + WorkThread.getDiscardedInfoCount());
            out.println("  quantity = " + spoiledThreadsInfo.length);
            for (String th: spoiledThreadsInfo) {
                out.println(th);
                out.println();
            }

            out.println("\nAll threads:");
            out.println(
                ErrorHelper.stackTracesToString(
                    Thread.currentThread().getThreadGroup()
                )
            );

            if (needToClose) out.close();
        } else if (cmd.equals("pools")) {
            PrintStream out;
            boolean needToClose;
            if (p.beyondEnd()) {
                out = System.out;
                needToClose = false;
            } else {
                String file = p.shiftWordOrBracketedString('\'');
                out = new PrintStream(new FileOutputStream(file));
                needToClose = true;
            }
            out.println("DB pools:");
            out.println(m_dbPools.values());
            out.println("Threads:");
            out.println(m_threadPool.toString());
            if (needToClose) out.close();
        }
        return true;
    }
    
    public void stop() throws CourierException {
        stop(0, null);
    }

    public void stop(final long timeout, final Integer status) {
        synchronized (this) {
            if (m_state == c_stoppedState || m_state == c_stoppingState) return;
        }

        WorkThread wt = new WorkThread(null, "Courier stop launcher", true);
        wt.setDaemon(false);
        wt.start();
        wt.launchWork(
            new SafeSystemRunnable() {
                public void safeRun() throws Throwable {
                    waitingStop(timeout, status, true);
                }
            }
        );
    }

    public void waitingStop(final long timeout, Integer status, boolean forcedExit) throws InterruptedException {
        synchronized (this) {
            if (m_state == c_stoppedState || m_state == c_stoppingState) return;
            m_logger.info("Courier stop request ...");
            m_state = c_stoppingState;
        }
        int apCount = 0;
        for(Pipeline p: m_pipes.values()) {
            try {
                if (p.isRunning()) apCount++;
            } catch(Throwable e) { m_logger.error(e); }
        }

        m_stopSignal = new CountDownLatch(apCount);
        WorkThread wt = new WorkThread(null, "Courier stopper");
        wt.launchWorkAndWait(
            new SafeSystemRunnable() {
                public void safeRun() throws Throwable {
                    tryToStop(timeout);
                }
            },
            timeout
        );
        wt.close(1000);

        long hangPipes = m_stopSignal.getCount();
        if (hangPipes > 0) {
            if (status != null) {
                System.err.println(
                    "Changing requested exit status " + status + " to 1 because some pipelines have not stopped");
            }
            status = 88888888;
        } else {
            if (status == null) {
                status = 0;
            }
        }

        String threadsFile = StringHelper.stringParam(m_params, "exit-threads-file", null);
        if (threadsFile != null) {
            Thread.sleep(10);
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            String stackTraces = ErrorHelper.stackTracesToString(tg, false);
            if (stackTraces.length() > 0) {
                try {
                    FileHelper.stringToFile(stackTraces, getAppFile(threadsFile));
                } catch (Exception e) {
                    m_logger.error(e);
                }
            }
        }
        getThreadPool().close();
        if (forcedExit) System.exit(status);
    }

    private void tryToStop(long timeout) throws InterruptedException, SQLException {
        m_scheduler.stop();

        if (m_shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(m_shutdownHook);
        }

        if (!waitStopped(timeout)) {
            m_logger.warning("Several pipelines (" + m_stopSignal.getCount() + ") have not finished");
        }

        synchronized (this) {
            m_logger.info("Courier stopping ...");

            for (CourierListener l: getListeners()) {
                try { l.courierStopped(); } catch(Throwable e) { m_logger.warning(e); }
            }

            try { m_sysDb.stop(); }
            catch(Throwable e) { m_logger.warning(e); }

            m_logger.debug("System DB stopped");

            m_mainPipeSchedules.stop();
            m_hostSchedules.stop();

            for(ObjectPoolIntf db: m_dbPools.values()) {
                try { db.close(); }
                catch(Throwable e) { m_logger.warning(e); }
            }

            MockDatabase.closeGlobalDatabase();

            m_logger.debug("Data sources stopped");

            if (m_dbsConf != null && m_dbsConf instanceof Disposable) {
                try { ((Disposable)m_dbsConf).dispose(); }
                catch(Throwable e) { m_logger.warning(e); }
                m_logger.debug("Common Databases stopped");
            }

            if (m_rtTimer != null) {
                m_rtTimer.cancel();
            }

            if (m_threadPool != null) {
                try {
                    if (m_threadPool instanceof ObjectPool2) ((ObjectPool2)m_threadPool).setCloseTimeout(2000);
                    m_threadPool.close();
                } catch(Throwable e) { m_logger.warning(e); }
            }

            m_logger.debug("Thread pool stopped");

            if (m_timer != null) {
                try { m_timer.cancel(); }
                catch(Throwable e) { m_logger.warning(e); }
            }

            /*
            if (m_keyboardListenerThread != null) {
                if (m_keyboardListenerThread.close(100)) {
                    m_logger.debug("Keyboard Listener Thread stopped");
                } else {
                    m_logger.warning("Keyboard Listener Thread timed out");
                }
            }
            */

            m_logger.info("Courier stopped");
            m_logProvider.close();
        }
    }

    synchronized void pipelineStopped(Pipeline pipe) {
        /*
        m_logger.debug(
            "pipelineStopped:" +
            " name = " + pipe.getName() +
            " state = " + m_state +
            " m_stopSignal.count = " + m_stopSignal.getCount()
        );
        */
        if (m_state == c_stoppingState) {
            if (m_stopSignal.getCount() == 1) m_state = c_stoppedState;
            m_stopSignal.countDown();
        }
    }

    boolean waitStopped(long timeout) throws InterruptedException {
        synchronized (this) {
            if (m_state == c_stoppedState) return true;
            ensureState(c_stoppingState);
        }
        if (m_stopSignal.getCount() > 0) {
            int activeCount = 0;
            for (Pipeline p: m_pipes.values()) {
                activeCount += p.getActiveProcCount();
            }
            if (timeout <= 0) {
                m_logger.info(
                    "Courier waiting pipelines (" + activeCount +
                    " processes) to stop ..."
                );
                m_stopSignal.await();
                return true;
            } else {
                m_logger.info(
                    "Courier waiting pipelines (" + activeCount +
                    " processes)  to stop " + timeout + " ms ..."
                );
                return m_stopSignal.await(timeout, TimeUnit.MILLISECONDS);
            }
        } else {
            return true;
        }
    }

    private void register() throws CourierException {
        for (Pipeline p : m_pipes.values()) {
            p.register();
        }
    }

    private File getFile(File path, String name, boolean mustExist) throws CourierException {
        File ret;
        if (name == null) return path;
        ret = new File(name);
        if (ret.isAbsolute()) {
            if (ret.exists() && mustExist) {
                throw new CourierException("System file does not exist: " + name);
            }
        } else {
            String absName = path.getAbsolutePath() + File.separator + name;
            ret = new File(absName);
            if (!ret.exists() && mustExist) {
                throw new CourierException("System file does not exist: " + absName);
            }
        }
        return fileToAbsForm(ret);
    }

    public File getAppFile(final String name) throws CourierException {
        return getFile(m_appDir, name, false);
    }

    private InputStream getSysResource(String name) throws Exception{
        return m_sysres.getResource(name);
    }

    private void serializeConf(final Node confRoot, final String where) throws IOException {
        final Serializer confSer = SerializerFactory.getSerializer(
            OutputPropertiesFactory.getDefaultMethodProperties("xml")
        );
        confSer.setOutputStream(new FileOutputStream(where));
        confSer.asDOMSerializer().serialize(confRoot);
    }

    private void initJdbcDriversInfo(Node n) throws Exception {
        Element[] nl = DomHelper.getChildrenByTagName(n, "driver", false);
        if (nl != null) {
            for (Element node : nl) {
                m_driverInfos.put(
                    DomHelper.getNodeAttr(node, "class"),
                    DomHelper.getAllParamMap(node)
                );
            }
        }
    }

    private void initSystemServices(Node root) throws Exception {
        m_params.putAll(DomHelper.getAttrParams(root));
        m_params.setProperty("host", InetAddress.getLocalHost().getHostName());

        m_scriptParams = DomHelper.getAttrParams(DomHelper.getChild(root, "scripting"));

        DefaultObjectPool2 threadPool = new DefaultObjectPool2(
            m_logProvider.getLogger(c_PoolLoggerName),
            "CourierThreadPool",
            new ThreadFactory(
                m_logger, Thread.currentThread().getThreadGroup(),
                "Courier", StringHelper.boolParam(m_params, "com-enabled", false)
            )
        );
        threadPool.setTimer(new Timer("ThreadPoolTimer"));
        threadPool.setCapacityPars(1, 1, -1, -1);
        threadPool.setShrinkPars(5*60*1000, 4, 5*60*1000);
        threadPool.setCheckPars(-1);
        m_threadPool = threadPool;
        m_threadPool.start();

        m_stmtFactory = new XmlStatementFactory(parseSysXml("StatementFactoryConf.xml"), null);

        Node n = DomHelper.getChild(root, "jdbc-drivers", false);
        if (n != null) initJdbcDriversInfo(n);

        n = DomHelper.getChild(root, "receiver-types", false);
        if (n != null) {
            for (Element n1 : DomHelper.getChildrenByTagName(n, "type", false)) {
                m_receiverTypes.put(
                    DomHelper.getNodeAttr(n1, "name"),
                    DomHelper.getNodeAttr(n1, "class")
                );
            }
        }

        n = getChild(root, "sys-db");
        m_sysDb = new LocalSystemDb(this, m_logProvider.getLogger("sys-db"), n, null);

        /*
        Class sysDbClass = Class.forName(
            "ru.rd.courier." + DomHelper.getNodeAttr(n, "class-name")
        );

        Constructor sysDbConstructor = sysDbClass.getConstructor(new Class[] {
            Application.class, CourierLogger.class, Node.class, Timer.class
        });

        m_sysDb = (SystemDb)sysDbConstructor.newInstance(new Object[] {
            this, m_logProvider.getLogger("sys-db"), n, null
        });
        */
    }

    private void initSchedules(Node root) {
        Logger logger = getStdLogger("schedule");
        long checkInterval = DomHelper.getTimeNodeAttr(root, "schedule-files-check-interval", 5000);
        String schfile = DomHelper.getNodeAttr(root, "host-schedule-file", null);
        if (schfile == null) {
            m_hostSchedules = new NullHostSchedules(logger);
        } else {
            FileHostSchedules hs = new FileHostSchedules(
                logger,
                getAppFile(schfile),
                getAppFile(DomHelper.getNodeAttr(root, "work-days-file", "work-days.cfg")),
                getTimer()
            );
            hs.getDetector().setCheckInterval(checkInterval);
            m_hostSchedules = hs;
        }
        m_scheduler.addListener(m_hostSchedules.getParent());
        schfile = DomHelper.getNodeAttr(root, "important-pipes-file", null);
        if (schfile == null) {
            m_mainPipeSchedules = new NullMainPipesSchedules();
        } else {
            FileMainPipesSchedules sch = new FileMainPipesSchedules(
                logger,
                getAppFile(schfile),
                getTimer(),
                m_code.toUpperCase()
            );
            sch.getDetector().setCheckInterval(checkInterval);
            m_mainPipeSchedules = sch;
        }
    }

    public String getParam(String name, boolean mustExist) throws CourierException {
        String p = m_params.getProperty(name);
        if (mustExist && (p == null)) {
            throw new CourierException("Application parameter '" + name + "' not found");
        }
        return p;
    }

    public String getParam(String name) throws CourierException {
        return getParam(name, true);
    }

    public String getParam(String name, String def) throws CourierException {
        String p = m_params.getProperty(name);
        if (p == null) return def;
        return p;
    }

    public String getScriptParam(String name, boolean mustExist) throws CourierException {
        String p = m_scriptParams.getProperty(name);
        if (mustExist && (p == null)) {
            throw new CourierException("Scripting parameter '" + name + "' not found");
        }
        return p;
    }

    public Map<String,String> getDriverParams(String driverName) {
        return m_driverInfos.get(driverName);
    }

    public String getScriptParam(String name) throws CourierException {
        return getScriptParam(name, true);
    }

    public ObjectPoolIntf initJdbcSourcePool(
        boolean isPool2,
        String name, String poolName,
        final CourierLogger poolLogger, ObjectPoolIntf threadPool,
        final Node n, Properties appProps, Map<String, Map<String, String>> driverInfos
    ) throws PoolException {
        return ConfHelper.asynchPoolFromXmlEx(
            poolLogger, threadPool, getTimer(), poolName, n,
            new JdbcSourceFactory(poolLogger, n, name, appProps, driverInfos, m_aprovider), isPool2
        );
    }

    public static ConnectionPool connectionPoolFromXml(
        ObjectPool threadPool,
        String dbProfileName, Node n, Properties appProps,
        Map driverInfos, CourierLogger poolLogger
    ) throws PoolException {
        final Properties info = new Properties();
        info.setProperty("user", DomHelper.getNodeAttr(n, "username"));
        info.setProperty("password", DomHelper.getNodeAttr(n, "password"));
        String driverClass = DomHelper.getNodeAttr(n, "driver");
        String progNameParamName = null;
        if (driverInfos.containsKey(driverClass)) {
            Properties di = (Properties)driverInfos.get(driverClass);
            if (di.containsKey("host-param-name")) {
                info.setProperty(
                    di.getProperty("host-param-name"),
                    appProps.getProperty("host")
                );
            }
            if (di.containsKey("program-param-name")) {
                progNameParamName = di.getProperty("program-param-name");
                info.setProperty(
                    progNameParamName,
                    appProps.getProperty("program-name")
                    + ":" + dbProfileName
                );
            }
        }

        info.putAll(DomHelper.getElementParams(n));

        int initialCapacity = DomHelper.getIntNodeAttr(n, "initial-capacity", 1);
        int incrementCapacity = DomHelper.getIntNodeAttr(n, "increment-capacity", 1);
        int maxCapacity = DomHelper.getIntNodeAttr(n, "max-capacity", -1);
        int shrinkIntervalMin = DomHelper.getIntNodeAttr(n, "shrink-interval-min", 5);
        int shrinkCapacity = DomHelper.getIntNodeAttr(n, "shrink-capacity", 1);
        int shrinkObsoleteIntervalMin = DomHelper.getIntNodeAttr(n, "shrink-obsolete-interval-min", 5);
        int checkIntervalMin = DomHelper.getIntNodeAttr(n, "check-interval-min", 2);
        int expirePeriodMin = DomHelper.getIntNodeAttr(n, "expire-period-min", 5*60);
        long allocateTimeout = DomHelper.getTimeNodeAttr(n, "allocate-timeout", 60*1000);

        return new ConnectionPool(
            poolLogger,
            dbProfileName + " connections", threadPool,
            driverClass, DomHelper.getNodeAttr(n, "url"),
            info, progNameParamName,
            initialCapacity, incrementCapacity, maxCapacity, allocateTimeout,
            shrinkIntervalMin*60*1000, shrinkCapacity, shrinkObsoleteIntervalMin*60*1000,
            checkIntervalMin*60*1000,
            DomHelper.getTimeNodeAttr(n, "check-timeout", 15*1000),
            expirePeriodMin*60*1000
        );
    }

    private boolean m_skipUnusedDb;
    private boolean m_checkUsedDb;

    private static final String s_datalinksPackage = "ru.rd.courier.datalinks";
    private Class getDbClass(Element n) throws ClassNotFoundException {
        final String type = DomHelper.getNodeAttr(n, "type", null);
        String className;
        if (type == null) {
            className = DomHelper.getNodeAttr(n, "class", true);
        } else {
            className = m_receiverTypes.get(type);
            if (className == null) {
                throw new CourierException(
                    "Class name for pool type '" + type + "' not specified");
            }
            className = s_datalinksPackage + "." + className;
        }
        return Class.forName(className);
    }

    private ObjectPoolIntf initDbProfile(boolean isPool2, Element n) throws Exception {
        final String name = DomHelper.getNodeAttr(n, "name");
        final String tagName = n.getTagName();
        String pname = name + "-Connections";
        if (tagName.equals("database")) {
            final CourierLogger poolLogger = m_logProvider.getLogger(c_PoolLoggerName);
            return initJdbcSourcePool(
                isPool2, name, pname, poolLogger, getThreadPool(),
                n, m_params, m_driverInfos
            );
        } else {
            final Class cl = getDbClass(n);
            PoolObjectFactory pof = null;

            Object[][][] sigs = {
                {
                    new Class[] {CourierLogger.class, ObjectPoolIntf.class, Node.class},
                    {m_logger, getThreadPool(), n}
                }, {
                    new Class[] {CourierLogger.class, Node.class},
                    {m_logger, n}
                }, {
                    new Class[] {CourierLogger.class, String.class, CourierContext.class, Node.class},
                    {m_logger, name, this, n}
                }, {
                    new Class[] {Node.class},
                    {n}
                }
            };

            for (Object[][] sigval: sigs) {
                Class[] sig = (Class[])sigval[0];
                try {
                    Constructor cntr = cl.getConstructor(sig);
                    pof = (PoolObjectFactory) cntr.newInstance(sigval[1]);
                } catch (NoSuchMethodException e) {
                    // OK
                }
            }

            if (pof == null) throw new RuntimeException("Failed to find proper constructor for class " + cl.getName());

            return ConfHelper.asynchPoolFromXmlEx(
                m_logProvider.getLogger(c_PoolLoggerName), getThreadPool(), getTimer(),
                pname, n, pof, isPool2
            );
        }
    }

    private Map<String, Integer> getDbCounters() {
        Map<String, Integer> dbCounters2 = new HashMap<String, Integer>();
        for (Pipeline pipe: m_pipes.values()) pipe.incDbCounter(dbCounters2);
        Map<String, Integer> dbCounters = new HashMap<String, Integer>();
        for (Map.Entry<String, Integer> e: dbCounters2.entrySet()) {
            dbCounters.put(e.getKey().toUpperCase(), e.getValue());
        }
        return dbCounters;
    }

    private void initDbProfiles(final Node ds) throws Exception {
        DriverManager.setLoginTimeout(DomHelper.getIntNodeAttr(ds, "jdbc-login-timeout-sec", 30));

        Map<String, Integer> dbCounters = getDbCounters();
        Set<String> unusedDbs = new HashSet<String>();

        boolean isPool2 = DomHelper.getBoolYesNo(ds, "enable-pool2", false);

        for (Node cn = ds.getFirstChild(); cn != null; cn = cn.getNextSibling()) {
            if (cn.getNodeType() == Node.ELEMENT_NODE) {
                final Element n = (Element)cn;
                final String name = DomHelper.getNodeAttr(n, "name").toUpperCase();
                if (!name.startsWith("$") && m_skipUnusedDb && !dbCounters.containsKey(name)) {
                    unusedDbs.add(name);
                    continue;
                }
                m_dbPools.put(name, initDbProfile(isPool2, n));
            }
        }

        //initPooledObjects(ds, "source", dbCounters, unusedDbs);
        //initPooledObjects(ds, "receiver", dbCounters, unusedDbs);

        if (m_skipUnusedDb && (unusedDbs.size() > 0)) {
            m_logger.info("Unused databases: " + unusedDbs);
        }
    }

    public boolean hasRequiredDbs(Set<String> dbs) {
        for (String dbName: dbs) {
            dbName = dbName.toUpperCase();
            if (!m_dbPools.containsKey(dbName)) return false;
        }
        return true;
    }

    public Set<String> getUndefinedDbs(Set<String> dbs) {
        Set<String> ret = new HashSet<String>();
        for (String dbName: dbs) {
            dbName = dbName.toUpperCase();
            if (!m_dbPools.containsKey(dbName)) ret.add(dbName);
        }
        return ret;
    }

    public void addDataReceiver(String name, PoolObjectFactory rf) {
        name = name.toUpperCase();
        AsynchObjectPool recPool = new AsynchObjectPool(
            name + " pool", m_logProvider.getLogger(c_PoolLoggerName),
            getThreadPool(), rf
        );
        m_dbPools.put(name, recPool);
    }

    private void initSourceProfiles(final Node n) throws CourierException {
        for (Node node : getChildren(n, "profile")) {
            final SourceProfile sp = new SourceProfile(this, m_logger, node);
            m_sProfiles.put(sp.getName(), sp);
        }
    }

    private void initTargetProfiles(final Node n) throws CourierException {
        for (Node node : getChildren(n, "profile")) {
            final TargetProfile tp = new TargetProfile(this, m_logger, node);
            m_tProfiles.put(tp.getName(), tp);
        }
    }

    private Logger getStdLogger() {
        return m_logger.getInnerLogger();
    }

    private Logger getStdLogger(String name) {
        return m_logProvider.getLogger(name).getInnerLogger();
    }

    public CourierLogger getLogger() {
        return m_logger;
    }

    private Node m_defaultPipeScheduleConf = null;

    public Node getPipeDefaultSheduleConf() {
        return m_defaultPipeScheduleConf;
    }

    public static ScriptStatement createWrapStatement(
        CourierContext courier, Node n, String scriptTag, final String scriptName
    ) throws CourierException {
        n = DomHelper.getChild(n, scriptTag, false);
        if (n == null) return new ObjectStatementCaller(
            TargetScriptProcess.c_ScriptsObjectName, scriptName
        );
        n = n.getFirstChild();
        return courier.getStmtFactory().getStatement(
            n, null
            /*
            new XmlStatementFactory.CustomTagProcessor() {
                public ScriptStatement process(XmlStatementFactory sf, Element n) throws CourierException {
                    if (n.getTagName().equals("inner-script")) {
                        return new ObjectStatementCaller(
                            TargetScriptProcess.c_ScriptsObjectName, scriptName
                        );
                    }
                    return null;
                }
            }
            */
        );
    }

    private ScriptStatement m_pipesScript;

    public ScriptStatement getPipesScript() {
        return m_pipesScript;
    }

    private void initPipelines(final Node n) throws CourierException {
        Node common = DomHelper.getChild(n, "common", false);
        if (common != null) {
            m_defaultPipeScheduleConf = DomHelper.getChild(
                common, "default-schedule", false
            );
        }

        m_pipesScript = createWrapStatement(
            this, n, "script", TargetScriptProcess.c_pipeScriptName
        );

        final Element[] nl = DomHelper.getChildrenByTagName(n, "pipeline");
        for (int i = 0; i < nl.length; i++) {
            try {
                Pipeline pipe = new Pipeline(this, m_logger, nl[i]);
                m_pipes.put(pipe.getName(), pipe);
            } catch (Exception e) {
                String name = DomHelper.getNodeAttr(nl[i], Pipeline.c_nameAttr, false);
                m_logger.error(
                    "Pipeline " + (name == null ? "number " + (i + 1) : name)
                    + " failed to initialize (for details see exception)", e
                );
            }
        }
    }

    public DataLogger getPipeLogger(String pipeName, boolean buffered) {
        return m_logProvider.getPipeLogger(pipeName, buffered);
    }

    public void syncProgress() throws CourierException {
        for (Pipeline pl : m_pipes.values()) {
            pl.syncProgress();
        }
    }

    public DateFormat getDatabaseDateFormat() throws CourierException {
        return new SimpleDateFormat(getParam("database-datetime-format", "yyyyMMdd HH:mm:ss.SSS"));
    }
    private Node[] getChildren(final Node n, final String tagName) {
        return DomHelper.getChildrenByTagName(n, tagName);
    }

    private Element getChild(final Node n, final String tagName) {
        return DomHelper.getChild(n, tagName);
    }

    //private MapStatementsProvider m_stmtProv = new MapStatementsProvider("Courier");

    private Timer m_rtTimer = null;
    public Timer getListeningSourceTimer() {
        if (m_rtTimer == null) m_rtTimer = new Timer("ListenSourceTimer", true);
        return m_rtTimer;
    }

    private MainPipesSchedules m_mainPipeSchedules;
    public MainPipesSchedules getMainPipesSchedules() {
        return m_mainPipeSchedules;
    }

    public StartStopListenerSet getHostEnableSchedule(String poolname) {
        String host = getPooledObjectHost(poolname);

        if (host == null) {
            StartStopListenerSet sss = new StartStopContainer(poolname + " host enable schedule", getStdLogger());
            sss.start(new Date());
            return sss;
        }

        return m_hostSchedules.getHostSchedule(host).getEnableSchedule();
    }

    private HostSchedules m_hostSchedules;
    public StartStopListenerSet getHostSchedule(String name) {
        String host = getPooledObjectHost(name);

        if (host == null) {
            StartStopListenerSet sss = new StartStopContainer(name + " host schedule", getStdLogger());
            //m_scheduler.addListener(sss);
            sss.start(new Date());
            return sss;
        }

        return m_hostSchedules.getHostSchedule(host).getMainSchedule();
    }

    public StartStopSet getSchedule() {
        return m_scheduler;
    }
}
