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
package ru.rd.courier.manager;

import org.apache.mina.common.*;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.manager.message.*;
import ru.rd.courier.scripting.DataReceiver;
import ru.rd.courier.scripting.PooledDataReceiver;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.NullNamedConfigProvider;
import ru.rd.courier.utils.StringExpression;
import ru.rd.courier.utils.templates.HashMapStringContext;
import ru.rd.courier.utils.templates.SimplePreparedTemplate;
import ru.rd.net.*;
import ru.rd.net.message.*;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.thread.AsynchProcessing;
import ru.rd.thread.PoolExecutorAdapter;
import ru.rd.thread.ThreadHelper;
import ru.rd.thread.PoolExecutor;
import ru.rd.utils.ObjectFactory;
import ru.rd.utils.SysUtils;
import ru.rd.utils.SafeLoggingRunnable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * User: AStepochkin
 * Date: 26.09.2008
 * Time: 12:23:21
 */
public class Manager implements ServerContext {
    private final CourierLogger m_logger;
    private final CourierLogger m_dataLogger;
    private final File m_sysPath;
    private final XmlLogProvider m_logProvider;
    private final ObjectPoolIntf m_threadPool;
    private final Timer m_timer;
    private final long m_storeStatePeriod;
    private TimerTask m_stateStoreTask;
    private final boolean m_isDebugMode;

    private long m_timeout;
    private long m_checkInterval;

    private int m_mainPort = -1;
    private SocketAcceptorConfig m_mainCfg;
    private IoAcceptor m_acceptor;

    private int m_statPort = -1;
    private SocketAcceptorConfig m_statCfg;

    private final Collection<ManagerListener> m_listeners = new LinkedList<ManagerListener>();
    private NamedPoolFactory m_dbpools;
    private StringExpression m_courierStateTmpl;
    private StringExpression m_statRequestTmpl;
    private StringExpression m_statRequestsPortionTmpl;
    private final StatProcessing m_statProcess;

    private final Map<Integer, ManagedCourier> m_couriers = new HashMap<Integer, ManagedCourier>();
    private volatile int m_lastCourierId = 1;

    private static StringExpression confTmpl(Element conf, String name) {
        String v = DomHelper.getChildValue(conf, name, null);
        if (v == null) return null;
        return new SimplePreparedTemplate(v);
    }

    public Manager(File sysPath, Element conf) throws Exception {
        //Element conf = DomHelper.parseXmlFile(new File(configFile)).getDocumentElement();
        if (sysPath == null) throw new NullPointerException("sysPath");
        if (conf == null) throw new NullPointerException("conf");
        m_sysPath = sysPath;
        m_isDebugMode = DomHelper.getBoolYesNo(conf, "debug-mode", false);
        m_logProvider = new XmlLogProvider(getSysPath().getAbsolutePath(), DomHelper.getChild(conf, "logging"));
        m_logger = m_logProvider.getLogger("");
        try {
            m_dataLogger = getLogger("data");
            m_timer = new Timer("CourierManagerTimer") {
                public void cancel() {
                    m_logger.debug("CourierManagerTimerCancel");
                    super.cancel();
                }
            };
            m_threadPool = DbConInitilizer.initPoolFromXml(
                PoolExecutorAdapter.createThreadPool2(m_logProvider.getLogger("thread"), "CourierManagerThreads"),
                DomHelper.getChild(conf, "thread-pool", false)
            );
            m_timeout = DomHelper.getLongNodeAttr(conf, "timeout", 4000);
            m_checkInterval = DomHelper.getLongNodeAttr(conf, "check-interval", 5000);
            m_statProcess = new StatProcessing(
                m_logger, new PoolExecutorAdapter(m_threadPool),
                DomHelper.getIntNodeAttr(conf, "max-stat-threads", 1)
            );
            m_statProcess.setTimer(m_timer);
            m_statProcess.setChunkSize(DomHelper.getIntNodeAttr(conf, "stat-portion-size", 1));
            m_statProcess.setMaxTargetCount(DomHelper.getIntNodeAttr(conf, "max-stat-buffer-size", 1000));
            initDbProfiles(conf);
            initMainAcceptor(conf);
            m_courierStateTmpl = confTmpl(conf, "state-request");
            m_statRequestTmpl = new SimplePreparedTemplate(DomHelper.getChildValue(conf, "stat-request"));
            String portionStr = DomHelper.getChildValue(conf, "stat-portion", null);
            if (portionStr != null) {
                m_statRequestsPortionTmpl = new SimplePreparedTemplate(portionStr);
            }
            m_storeStatePeriod = DomHelper.getTimeNodeAttr(conf, "couriers-store-state-interval", -1, "ms");
        } catch (Exception e) {
            try { dispose(); }
            catch (Throwable e1) { m_logger.error(e1.initCause(e)); }
            throw e;
        }
    }

    public File getSysPath() {
        return m_sysPath;
    }

    public void start() throws Exception {
        m_threadPool.start();
        m_dbpools.start();
        bindLocal(m_acceptor, m_mainPort, new MainIoHandler(), m_mainCfg);
        bindLocal(m_acceptor, m_statPort, new StatIoHandler(), m_statCfg);
        if (m_storeStatePeriod > 0) {
            m_stateStoreTask = new TimerTask() {
                public void run() {
                    execWork(new SafeLoggingRunnable(m_logger) {
                        protected void safeRun() throws Throwable {
                            storeState();
                        }
                    });
                }
            };
            m_timer.schedule(m_stateStoreTask, m_storeStatePeriod, m_storeStatePeriod);
        }
    }

    private void execWork(Runnable work) {
        PoolExecutor exec = new PoolExecutor(m_threadPool);
        exec.exec(work);
    }

    private boolean m_closed = false;

    public boolean stop() {
        if (m_closed) return true;
        m_closed = true;

        debug("Manager stop request");

        if (m_stateStoreTask != null) m_stateStoreTask.cancel();

        try { m_acceptor.unbindAll(); } catch (Exception e) { m_logger.warning(e); }
        debug("Acceptor unbound");

        try {
            while (true) {
                ManagedCourier courier;
                synchronized(m_couriers) {
                    if (m_couriers.size() == 0) break;
                    Integer key = m_couriers.keySet().iterator().next();
                    courier = m_couriers.remove(key);
                }
                try { courier.dispose(); } catch(Exception e) { m_logger.warning(e); }
            }
        } catch(Exception e) { m_logger.warning(e); }
        debug("Couriers disposed");

        if (m_statProcess != null) {
            m_statProcess.close(m_timeout);
        }
        debug("StatActivities stopped");

        try { m_dbpools.stop(); } catch (Exception e) { m_logger.warning(e); }
        debug("DbPools stopped");
        boolean stopped = SysUtils.dispose(m_threadPool, getLogger(), 2000);
        debug("ThreadPool " + (stopped ? "" : "not ") + "stopped");
        //if (!stopped) debug(m_threadPool.toString());

        m_timer.cancel();
        return stopped;
    }

    private void initDbProfiles(Element conf) throws Exception {
        DbConInitilizer dbpools = new DbConInitilizer(m_logger, m_threadPool, new NullNamedConfigProvider());
        dbpools.setTimer(getTimer());
        dbpools.setDriverInfo(DbConInitilizer.confDriverInfo(DomHelper.getChild(conf, "jdbc-drivers")));
        dbpools.setTypes(DbConInitilizer.confTypes(DomHelper.getChild(conf, "db-types")));
        m_dbpools = dbpools;
        Element dbconf = DomHelper.getChild(conf, "databases");
        for (Node cn = dbconf.getFirstChild(); cn != null; cn = cn.getNextSibling()) {
            if (cn.getNodeType() == Node.ELEMENT_NODE) {
                final Element n = (Element)cn;
                final String name = DomHelper.getNodeAttr(n, "name").toUpperCase();
                m_dbpools.initPool(name, n);
            }
        }
    }

    private SocketAcceptorConfig initAcceptorCfg(ProtocolCodecFactory codec) {
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.setReuseAddress(true);
        cfg.setThreadModel(ThreadModel.MANUAL);
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(codec));
        //cfg.getFilterChain().addLast(
        //    "threadPool", new ExecutorFilter(new PoolExecutorAdapter(m_threadPool)));
        return cfg;
    }

    private void initMainAcceptor(Element conf) throws NoSuchMethodException {
        ExceptionMonitor.setInstance(new ExceptionMonitor() {
            public void exceptionCaught(Throwable cause) {
                m_logger.warning(cause);
            }
        });

        m_acceptor = new SocketAcceptor(
            DomHelper.getIntNodeAttr(conf, "processor-count", Runtime.getRuntime().availableProcessors() + 1),
            new PoolExecutorAdapter(m_threadPool)
        );
        m_mainPort = DomHelper.getIntNodeAttr(conf, "main-port");
        m_mainCfg = initAcceptorCfg(buildMainCodec());
        m_statPort = DomHelper.getIntNodeAttr(conf, "stat-port");
        m_statCfg = initAcceptorCfg(buildStatCodec());
    }

    private static void bindLocal(IoAcceptor acceptor, int port, IoHandler handler, IoServiceConfig config) throws IOException {
        String lname = InetAddress.getLocalHost().getHostName();
        for (InetAddress addr: InetAddress.getAllByName(lname)) {
            acceptor.bind(new InetSocketAddress(addr, port), handler, config);
        }
        acceptor.bind(new InetSocketAddress("127.0.0.1", port), handler, config);
    }

    public CourierLogger getLogger() {
        return m_logger;
    }

    public int getStatPort() {
        return m_statPort;
    }

    public long getTimeout() {
        return m_timeout;
    }

    public void addListener(ManagerListener listener) {
        m_listeners.add(listener);
    }

    public void removeListener(ManagerListener listener) {
        m_listeners.remove(listener);
    }

    private void debug(String message) {
        if (isDebugMode()) m_logger.debug(message);
    }

    private void debug(String format, Object param) {
        if (isDebugMode()) m_logger.debug(MessageFormat.format(format, param));
    }

    private void debug(String format, Object param1, Object param2) {
        if (isDebugMode()) m_logger.debug(MessageFormat.format(format, param1, param2));
    }

    private abstract class SRunnable extends SafeLoggingRunnable {
        public SRunnable() {
            super(m_logger);
        }
    }

    public void courierConnected(final ManagedCourier courier) {
        if (m_closed) return;
        synchronized(m_couriers) {
            if (!courier.isActive()) return;
            m_logger.info("Courier connected: " + courier);
            m_couriers.put(courier.id, courier);
        }
        for (ManagerListener l: m_listeners) {
            try { l.courierAdded(courier); } catch (Exception e) { m_logger.warning(e); }
        }
        if (m_courierStateTmpl != null) {
            m_statProcess.addTarget(new SRunnable() {
                protected void safeRun() throws Throwable {
                    statExec(m_courierStateTmpl.calculate(courierStateContext(courier, "CONNECTED")));
                }
            });
        }
    }

    public void courierDisconnected(final ManagedCourier courier) {
        try {
            synchronized(m_couriers) {
                m_couriers.remove(courier.id);
            }
            for (ManagerListener l: m_listeners) {
                try { l.courierRemoved(courier); } catch (Exception e) { m_logger.warning(e); }
            }
        } catch (Exception e) {
            m_logger.warning(e);
        }
        m_logger.info("Courier disconnected: " + courier);
        if (courier.info != null && m_courierStateTmpl != null) {
            m_statProcess.addTarget(new SRunnable() {
                protected void safeRun() throws Throwable {
                    statExec(m_courierStateTmpl.calculate(courierStateContext(courier, "DISCONNECTED")));
                }
            });
        }
    }

    private void storeState() throws Exception {
        List<ManagedCourier> couriers;
        synchronized(m_couriers) {
            couriers = new ArrayList<ManagedCourier>(m_couriers.values());
        }
        for (ManagedCourier courier: couriers) {
            if (courier.isActive()) stateExec(courier);
        }
    }

    public void writeCouriers(Writer w) throws IOException {
        synchronized(m_couriers) {
            boolean first = true;
            for (ManagedCourier courier: m_couriers.values()) {
                if (first) first = false; else w.write("\n");
                w.write("ID=" + courier.id);
                w.write(" Host=" + courier.getHost());
                w.write(" Config=" + courier.getConfig());
                w.write(" Active=" + courier.isActive());
            }
        }
    }

    private void stateExec(ManagedCourier courier, String state) throws Exception {
        statExec(m_courierStateTmpl.calculate(courierStateContext(courier, state)));
    }

    private void stateExec(ManagedCourier courier) throws Exception {
        stateExec(courier, "CONNECTED");
    }

    private HashMapStringContext courierContext(ManagedCourier courier) {
        HashMapStringContext ctx = new HashMapStringContext();
        ctx.setVar("Host", courier.getHost());
        ctx.setVar("Config", courier.getConfig());
        return ctx;
    }

    private String processResultToRequest(ManagedCourier courier, ProcessResult message) {
        HashMapStringContext ctx = courierContext(courier);
        ctx.setVar("ID", Long.toString(message.getId()));
        ctx.setVar("Pipe", message.getPipe());
        ctx.setVar("RecordCount", Integer.toString(message.getRecordCount()));
        ctx.setVar("Error", message.getError());
        ctx.setVar("ErrorCount", Integer.toString(message.getErrorCount()));
        ctx.setVar("ErrorStack", message.getErrorStack());
        ctx.setVar("StartTime", Long.toString(message.getStartTime()));
        ctx.setVar("TotalTime", Long.toString(message.getTotalTime()));
        ctx.setVar("SourceTime", Long.toString(message.getSourceTime()));
        ctx.setVar("TargetTime", Long.toString(message.getTargetTime()));
        ctx.setVar("SourceDb", message.getSourceDbName());
        ctx.setVar("SourceDbType", message.getSourceDbType());
        ctx.setVar("SourceDbUrl", message.getSourceDbUrl());
        ctx.setVar("TargetDb", message.getTargetDbName());
        ctx.setVar("TargetDbType", message.getTargetDbType());
        ctx.setVar("TargetDbUrl", message.getTargetDbUrl());
        return m_statRequestTmpl.calculate(ctx);
    }

    private HashMapStringContext courierStateContext(ManagedCourier courier, String state) {
        HashMapStringContext ctx = courierContext(courier);
        ctx.setVar("State", state);
        return ctx;
    }

    private String resultsPortion(String portion) {
        if (m_statRequestsPortionTmpl == null) return portion;
        HashMapStringContext ctx = new HashMapStringContext();
        ctx.setVar("Portion", portion);
        return m_statRequestsPortionTmpl.calculate(ctx);
    }

    private static final String cStatDbName = "statistics";

    private void processResult(ManagedCourier courier, ProcessResult result) throws Exception {
        courier.processResult(result);
        statExec(resultsPortion(processResultToRequest(courier, result)));
    }

    private void processResult(List<ProcessResultWork> results) throws Exception {
        StringBuffer buf = new StringBuffer();
        for (ProcessResultWork work: results) {
            work.courier.processResult(work.result);
            buf.append(processResultToRequest(work.courier, work.result));
        }
        statExec(resultsPortion(buf.toString()));
    }

    private void dbexec(String name, String request) throws Exception {
        DataReceiver r = getReceiver(name);
        try {
            m_dataLogger.info("\n" + request);
            r.process(request);
            r.flush();
        } finally {
            try { r.close(); } catch (Exception e) { m_logger.warning(e); }
        }
    }

    private void statExec(String request) throws Exception {
        dbexec(cStatDbName, request);
    }

    private DataReceiver getReceiver(String name) throws Exception {
        return new PooledDataReceiver(m_dbpools.getPool(name));
    }

    public static final String cCourierInfoAttr = "CourierInfo";

    public Timer getTimer() {
        return m_timer;
    }

    public CourierLogger getLogger(String name) {
        return m_logProvider.getLogger(name);
    }

    public ObjectPoolIntf getThreadPool() {
        return m_threadPool;
    }

    public boolean isDebugMode() {
        return m_isDebugMode;
    }

    private static ManagedCourier findSessionCourier(IoSession session) {
        return (ManagedCourier)session.getAttribute(cCourierInfoAttr);
    }

    public static ManagedCourier sessionCourier(IoSession session) {
        ManagedCourier c = findSessionCourier(session);
        if (c == null) throw new RuntimeException("No sessioned courier");
        return c;
    }

    public void dispose() {
        stop();
        try { m_dbpools.dispose(); } catch (Exception e) { m_logger.warning(e); }
    }

    private void courierInfoReceived(IoSession session, final CourierInfoMessage info) throws Exception {
        final ManagedCourier courier = sessionCourier(session);
        ThreadHelper.exec(getThreadPool(), new Runnable() {
            public void run() {
                try {
                    courier.handShake(info);
                } catch (Exception e) {
                    m_logger.warning("Courier " + courier + ": handshake failed", e);
                    courier.dispose();
                }
            }
        }, getTimeout(), getLogger());
    }

    private int nextCourierId() {
        synchronized(m_couriers) {
            return m_lastCourierId++;
        }
    }

    private class MainIoHandler extends IoHandlerAdapter {
        public final void sessionOpened(IoSession session) throws Exception {
            debug("Main-SessionOpened");
            ManagedCourier courier = new ManagedCourier(Manager.this, nextCourierId(), session, m_checkInterval);
            session.setAttribute(cCourierInfoAttr, courier);
        }

        public final void sessionClosed(IoSession session) throws Exception {
            ManagedCourier courier = findSessionCourier(session);
            if (courier != null) courier.dispose();
            debug("Main-SessionClosed courier (" + courier + ")");
        }

        public final void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            ManagedCourier courier = findSessionCourier(session);
            m_logger.warning("Main-ExceptionCaught courier (" + courier + ")", cause);
            session.close();
        }

        public final void messageReceived(IoSession session, Object message) throws Exception {
            ManagedCourier courier = findSessionCourier(session);
            debug("Main-MessageReceived courier (" + courier + "): {0} {1}", message.getClass().getSimpleName(), message);
            sessionCourier(session).messageReceived(session, message);
        }

        public final void messageSent(IoSession session, Object message) throws Exception {
            ManagedCourier courier = findSessionCourier(session);
            debug("Main-MessageSent courier (" + courier + "): {0} {1}", message.getClass().getSimpleName(), message);
        }
    }

    private class StatIoHandler extends IoHandlerAdapter {
        public final void sessionOpened(IoSession session) throws Exception {
            debug("Stat-SessionOpened");
        }

        public final void sessionClosed(IoSession session) throws Exception {
            debug("Stat-SessionClosed");
        }

        public final void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            m_logger.warning("Stat error on session " + session, cause);
            session.close();
        }

        public final void messageReceived(IoSession session, Object message) throws Exception {
            debug("Stat-messageReceived: {0} {1}", message.getClass().getSimpleName(), message);
        }

        public final void messageSent(IoSession session, Object message) throws Exception {
            debug("Stat-messageSent: {0}", message);
        }
    }

    private ProtocolCodecFactory buildMainCodec() throws NoSuchMethodException {
        final Charset charset = Charset.forName("cp1251");
        MultiCodecFactory codec = new MultiCodecFactory();
        codec.registerEncoder(
            ManagerInfoMessage.class, 1,
            ReflectFactory.factoryOnCharset(ManagerInfoMessageEncoder.class, charset)
        );
        codec.registerEncoder(
            CommonAnswer.class, 2,
            ReflectFactory.factoryOnCharset(CommonAnswerEncoder.class, charset)
        );
        codec.registerEncoder(
            CheckMessage.class, 3,
            new ReflectFactory<ProtocolEncoder>(CheckMessageEncoder.class)
        );


        codec.registerDecoder(1, new ObjectFactory<MessageDecoder>() {
            public MessageDecoder create() throws Exception {
                return new CourierInfoMessageDecoder(charset) {
                    protected void customProcessing(IoSession session, CourierInfoMessage info) throws Exception {
                        debug("CourierInfoMessage received");
                        courierInfoReceived(session, info);
                    }
                };
            }
        });

        codec.registerDecoder(2, new NullMessageDecoderFactory() {
            protected void customProcessing(IoSession session) {
                WriteFuture f = session.write(CommonAnswer.OK_ANSWER);
                long timeout = getTimeout();
                if (!f.join(timeout)) getLogger().warning("Main close answer timeout " + timeout);
                try { sessionCourier(session).dispose(); } catch (Exception e) {getLogger().warning(e); }
            }
        });

        codec.registerDecoder(3, new ObjectFactory<MessageDecoder>(){
            public MessageDecoder create() throws Exception {
                return new CheckMessageDecoder();
            }
        });
        return new CumulativeDecoderProtocolFactory(codec);
    }

    private ProtocolCodecFactory buildStatCodec() throws NoSuchMethodException {
        final Charset charset = Charset.forName("cp1251");
        MultiCodecFactory codec = new MultiCodecFactory();
        codec.registerEncoder(
            CommonAnswer.class, 1,
            ReflectFactory.factoryOnCharset(CommonAnswerEncoder.class, charset)
        );
        codec.registerEncoder(
            CheckMessage.class, 2,
            new ReflectFactory<ProtocolEncoder>(CheckMessageEncoder.class)
        );

        codec.registerDecoder(
            1,
            new ObjectFactory<MessageDecoder>() {
                public MessageDecoder create() throws Exception {
                    return new ProcessResultDecoder(charset) {
                        protected void customProcessing(IoSession session, ProcessResult message) throws Exception {
                            String error = null;
                            try {
                                debug("ProcessResult: {0}", message);
                                ManagedCourier c = sessionCourier(session);
                                //processResult(c, message);
                                m_statProcess.addTarget(new ProcessResultWork(c, message));
                            } catch (Throwable e) {
                                m_logger.warning(e);
                                error = e.getMessage();
                            }
                            session.write(new CommonAnswer(error));
                        }
                    };
                }
            }
        );
        codec.registerDecoder(2, new ObjectFactory<MessageDecoder>(){
            public MessageDecoder create() throws Exception {
                return new CheckMessageDecoder() {
                    protected void customProcessing(IoSession session, CheckMessage message) {
                        session.write(message);
                    }
                };
            }
        });
        codec.registerDecoder(3, new ObjectFactory<MessageDecoder>(){
            public MessageDecoder create() throws Exception {
                return new ManagerInfoMessageDecoder(charset) {
                    protected void customProcessing(IoSession session, ManagerInfoMessage message) {
                        ManagedCourier c = m_couriers.get(message.getId());
                        if (c == null) {
                            debug("Invalid courier id: {0}", message.getId());
                            session.close();
                        } else {
                            session.setAttribute(cCourierInfoAttr, c);
                            c.addStatSession(session);
                            session.write(new CommonAnswer());
                        }
                    }
                };
            }
        });
        codec.registerDecoder(
            4,
            new ObjectFactory<MessageDecoder>() {
                public MessageDecoder create() throws Exception {
                    return new ProcessResultArrayDecoder(charset) {
                        protected void customProcessing(IoSession session, ProcessResultArray message) throws Exception {
                            String error = null;
                            try {
                                debug("ProcessResultArray: {0}", message);
                                ManagedCourier c = sessionCourier(session);
                                //processResult(c, message);
                                for (ProcessResult r: message.getResults()) {
                                    m_statProcess.addTarget(new ProcessResultWork(c, r));
                                }
                            } catch (Throwable e) {
                                m_logger.warning(e);
                                error = e.getMessage();
                            }
                            session.write(new CommonAnswer(error));
                        }
                    };
                }
            }
        );
        return new CumulativeDecoderProtocolFactory(codec);
    }

    private class ProcessResultWork extends SRunnable {
        public final ManagedCourier courier;
        public final ProcessResult result;

        public ProcessResultWork(ManagedCourier courier, ProcessResult result) {
            this.courier = courier;
            this.result = result;
        }

        protected void safeRun() throws Throwable {
            processResult(courier, result);
        }
    }

    private class StatProcessing extends AsynchProcessing<Manager, Runnable> {
        public StatProcessing(CourierLogger logger, Executor exec, int maxExec) {
            super(logger, exec, maxExec);
        }

        protected Manager findResource() throws Exception {
            return Manager.this;
        }

        protected boolean isResourceValid(Manager resource) throws Exception {
            return m_closed;
        }

        protected void releaseResource(Manager resource) throws Exception {}
        protected void releaseCancelledResource(Manager resource) throws Exception {}

        protected void process(Manager manager, Runnable work) throws Exception {
            work.run();
        }

        protected void process(Manager manager, List<Runnable> works) throws Exception {
            for (Runnable work: works) work.run();
        }
    }

    public String toDebugString() {
        String ds;
        synchronized(m_couriers) {
            ds = "Couriers=" + m_couriers.size() + " Listeners=" + m_listeners.size();
        }
        return ds + " StatProcess: " + m_statProcess.toDebugString();
    }
}
