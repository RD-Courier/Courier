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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.rd.courier.logging.ConsoleCourierLogger;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.logging.data.DaysFileLog;
import ru.rd.courier.manager.ui.AdminConsole;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.StringHelper;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * User: AStepochkin
 * Date: 13.10.2008
 * Time: 13:05:48
 */
public class ManagerStarter {
    private final Manager m_manager;
    private final AdminConsole m_console;
    private Timer m_logTimer;
    private MemLogger m_memlog;

    private class MemLogger {
        private final CourierLogger m_logger;
        private final DateFormat m_df = new SimpleDateFormat("dd-MM HH:mm:ss");
        private final DaysFileLog m_writer;
        private boolean m_first = true;

        public MemLogger(
            File dir, String encoding, String dateFormat, int days,
            String prefix, String postfix, boolean deleteUnknownFiles
        ) {
            m_logger = ConsoleCourierLogger.instance();
            m_writer = new DaysFileLog(
                m_logger, dir, encoding, false, dateFormat, days,
                prefix, postfix, deleteUnknownFiles, true, 0
            );
        }

        public void log() {
            Runtime r = Runtime.getRuntime();
            String sep;
            if (m_first) {
                m_first = false;
                sep = "";
            } else {
                sep = "\n";
            }
            m_writer.log(
                sep + m_df.format(new Date())
                + " Memory: Free=" + r.freeMemory()
                + " | Manager: " + m_manager.toDebugString()
                + " | Console: " + m_console.toDebugString()
            );
        }

        public void close() {
            try { m_writer.close(); } catch (Exception e) { m_logger.error(e); }
        }
    }

    public ManagerStarter(File confFile) throws Exception {
        File spath = confFile.getParentFile();
        Element conf = DomHelper.parseXmlFile(confFile).getDocumentElement();

        m_manager = new Manager(spath, conf);
        m_console = new AdminConsole(m_manager, "CourierEye", m_manager);
        initConsole(DomHelper.getChild(conf, "m_console", false), m_console);
        m_console.setSize(500, 300);
        m_console.setVisible(true);
        //m_console.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final String debugLog = System.getProperty("CourierManagerDebugLog");
        if (debugLog != null) {
            long logInterval = StringHelper.parseTime(
                System.getProperty("CourierManagerDebugInterval", "5m"), "s"
            );
            m_memlog = new MemLogger(
                new File(debugLog), "cp1251", "yyyyMMdd", 4, "", ".log", true
            );
            m_logTimer = new Timer(true);
            m_logTimer.schedule(
                new TimerTask() {
                    public void run() {
                        try {
                            m_memlog.log();
                        } catch (Exception e) {
                            m_manager.getLogger().warning(e);
                        }
                    }
                },
                0, logInterval
            );
        }

        m_console.addWindowListener(new WindowAdapter() {
            public void windowDeactivated(WindowEvent e) {
                if (!e.getWindow().isShowing()) {
                    try {
                        close();
                    } catch(Exception err) {
                        err.printStackTrace(System.err);
                    }
                    ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
                    System.exit(0);
                }
            }
        });

        m_manager.addListener(new ManagerListener() {
            public void courierAdded(ManagedCourier courier) {
                m_console.addCourier(courier);
            }

            public void courierRemoved(ManagedCourier courier) {
                m_console.removeCourier(courier);
            }
        });

        m_manager.start();
    }

    public void close() {
        if (m_logTimer != null) try { m_logTimer.cancel(); } catch (Exception e) { e.printStackTrace(); }
        if (m_memlog != null) try { m_memlog.close(); } catch (Exception e) { e.printStackTrace(); }
        try { m_manager.dispose(); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) throws Exception {
        File confFile = new File(args[0]).getAbsoluteFile();
        new ManagerStarter(confFile);
    }

    private static void initConsole(Node conf, AdminConsole console) {
        if (conf == null) return;
        console.setTitle(DomHelper.getNodeAttr(conf, "title", "CourierEye"));
        console.setMaxRows(DomHelper.getIntNodeAttr(conf, "stat-max-rows"));
        console.setHiddenAlivePeriod(DomHelper.getTimeNodeAttr(conf, "stat-frame-alive-period", 3000, "ms"));
    }
}
