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
package ru.rd.courier.scripting.dataaccess;

import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.CorrectUpdateResult;
import ru.rd.courier.jdbc.ResultSets.StringListResultSet;
import ru.rd.courier.jdbc.UpdateResult;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.DateColumnInfo;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.utils.*;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolException;
import ru.rd.pool.SynchObjectPool;
import ru.rd.thread.ThreadFactory;
import ru.rd.utils.FileNameMatcher;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.io.*;
import java.net.InetAddress;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 08.11.2007
 * Time: 12:43:20
 */
public class FileSystemSource extends TimedStringReceiver implements DataSource {
    private final CourierLogger m_logger;
    private final boolean m_ownTreadPool;
    private final ObjectPoolIntf m_threadPool;
    private final boolean m_showExecOutput;
    private final boolean m_noErrorStreamException;

    public FileSystemSource(CourierLogger logger, ObjectPoolIntf threadPool, boolean showExecOutput, boolean noErrorStreamException) {
        m_logger = logger;
        if (threadPool == null) {
            m_ownTreadPool = true;
            m_threadPool = new SynchObjectPool("FileSystemSource", m_logger, new ThreadFactory(m_logger, "FileSystemSource"));
            try {
                m_threadPool.start();
            } catch (PoolException e) {
                throw new RuntimeException(e);
            }
        } else {
            m_ownTreadPool = false;
            m_threadPool = threadPool;
        }
        m_showExecOutput = showExecOutput;
        m_noErrorStreamException = noErrorStreamException;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        generalRequestSafe(operation);
        return null;
    }

    protected List<LinkWarning> timedFlush() {
        return null;
    }

    protected void timedClose() throws CourierException {
        try {
            if (m_ownTreadPool) m_threadPool.close();
        } catch (PoolException e) {
            throw new CourierException(e);
        }
    }
    public void setTimeout(int timeout) throws CourierException {}
    public void cancel() throws CourierException {}

    public ResultSet request(String query) throws CourierException {
        return generalRequestSafe(query);
    }

    private ResultSet generalRequestSafe(String request) {
        try {
            return generalRequest(request);
        } catch (SQLException e) {
            throw new CourierException(e);
        }
    }

    public ResultSet generalRequest(String request) throws SQLException {
        try {
            return unsafeRequest(request);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            SQLException sqlExc = new SQLException(e.getMessage());
            sqlExc.initCause(e);
            throw sqlExc;
        }
    }

    private ResultSet unsafeRequest(String request)
        throws SQLException, CourierException, IOException, InterruptedException
    {
        ResultSet res = null;
        final StringSimpleParser p = new StringSimpleParser();
        p.setText(request);
        p.skipBlanks();
        p.ensureNotEnd();
        String requestCommand = p.shiftWord().toLowerCase();
        p.skipBlanks();
        if (requestCommand.equals("check")) {
        } else if (requestCommand.equals("exec")) {
            execCommand(
                p.endSubstr(), m_logger, m_threadPool, null, null, m_showExecOutput, m_noErrorStreamException
            );
        } else {
            final List<String> pars = new LinkedList<String>();
            while (!p.beyondEnd()) {
                pars.add(p.shiftWordOrBracketedString("\'\""));
                p.skipBlanks();
            }
            if (requestCommand.equals("list")) {
                res = listFiles(pars);
            } else if (requestCommand.equals("delete")) {
                deleteCommand(pars);
            } else if (requestCommand.equals("single-delete")) {
                singleDeleteCommand(pars);
            } else if (requestCommand.equals("rename-bulk")) {
                bulkRenameCommand(pars);
            } else if (requestCommand.equals("rename")) {
                renameCommand(pars);
            } else if (requestCommand.equals("file-exist")) {
                res = fileExists(pars);
            } else if (requestCommand.equals("host-name")) {
                res = hostName();
            } else if (requestCommand.equalsIgnoreCase("rfc822")) {
                res = rfc822(pars);
            } else {
                throw new SQLException("Unknown command '" + requestCommand + "'");
            }
        }
        return res;
    }

    private ResultSet rfc822(List<String> pars) {
        if (pars.size() < 1) throw new RuntimeException("Check severity not specified");
        boolean strict = pars.get(0).equalsIgnoreCase("strict");
        if (pars.size() < 2) throw new RuntimeException("Address not specified");
        String addrStr = pars.get(1);
        boolean valid = true;
        String error = null;
        String personal = null;
        String localname = null;
        String domain = null;
        try {
            InternetAddress addr = new InternetAddress(addrStr, strict);
            //if (strict) addr.validate();
            personal = addr.getPersonal();
            int p = addr.getAddress().indexOf('@');
            if (p >= 0) {
                localname = addr.getAddress().substring(0, p);
                domain = addr.getAddress().substring(p + 1);
            } else {
                localname = addr.getAddress();
            }
        } catch (AddressException e) {
            valid = false;
            error = e.getMessage();
        }

        return new StringListResultSet(
            new String[] {"AddrValid", "AddrError", "Personal", "LocalName", "Domain"},
            new String[] {valid ? "1" : "0", error, personal, localname, domain}
        );
    }

    private ResultSet fileExists(List<String> pars) throws SQLException {
        if (pars.size() < 1) throw new SQLException("File not specified");
        DataBufferResultSet rs = new DataBufferResultSet();
        rs.addColumn(new StringColumnInfo("FileExist", 1));
        rs.addRecord();

        File f = new File(pars.get(0));
        rs.updateString(1, f.exists() ? "1" : "0");
        rs.beforeFirst();
        //rs.setStatement(stmt);
        return rs;
    }

    private ResultSet hostName() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return new StringListResultSet(new String[] {"HostName"}, new String[] {host});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResultSet listFiles(List<String> pars) throws SQLException {
        if (pars.size() < 1) throw new SQLException("Catalog not specified");
        String dirName = pars.get(0);
        FilenameFilter nameFilter = null;
        if (pars.size() > 1) nameFilter = new FileNameMatcher(pars.get(1));
        File dir = new File(dirName);
        DataBufferResultSet rs = new DataBufferResultSet();
        rs.addColumn(new StringColumnInfo("AbsolutePath", 512));
        rs.addColumn(new DateColumnInfo("ModificationDate", new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS")));
        rs.addColumn(new StringColumnInfo("Size", 32));
        rs.addColumn(new StringColumnInfo("FileName", 256));

        if (!dir.exists()) {
            return rs;
            //throw new SQLException(
            //    "Directory does not exist: " + dir.getAbsolutePath());
        }
        File[] listFiles;
        if (nameFilter == null) {
            listFiles = dir.listFiles();
        } else {
            listFiles = dir.listFiles(nameFilter);
        }

        if (listFiles != null) {
            for (File listFile : listFiles) {
                rs.addRecord();
                rs.updateString(1, listFile.getAbsolutePath());
                rs.updateDate(2, new Date(listFile.lastModified()));
                rs.updateString(3, Long.toString(listFile.length()));
                rs.updateString(4, listFile.getName());
            }
        }
        rs.beforeFirst();
        //rs.setStatement(stmt);
        return rs;
    }

    public static void execCommand(
        String cmd, CourierLogger logger, ObjectPoolIntf threadPool,
        OutputStream addOutput, File workDir,
        boolean showExecOutput, boolean noErrorStreamException
    ) throws IOException, InterruptedException {
        processCommandOutput(
            new OsProcess(cmd, null, workDir), logger, threadPool, addOutput, showExecOutput, noErrorStreamException
        );
    }

    public static void processCommandOutput(
        OsCommand proc, CourierLogger logger, ObjectPoolIntf threadPool,
        OutputStream addOutput,
        boolean showExecOutput, boolean noErrorStreamException
    ) throws IOException, InterruptedException {
        proc.start();
        CompositeOutputStream actErrOutput = new CompositeOutputStream(logger);
        if (addOutput != null) actErrOutput.addOutput(addOutput);
        if (showExecOutput) actErrOutput.addOutput(new NotClosingOutputStream(System.out));
        ByteArrayOutputStream errForLog = new ByteArrayOutputStream();
        final int cWarnLinesCount = 3;
        LinesLimitedOutputStream errForLogLimiter = new LinesLimitedOutputStream(errForLog, cWarnLinesCount);
        actErrOutput.addOutput(errForLogLimiter);

        StreamTransferring errTransfer = new StreamTransferring(proc.getErrorStream(), actErrOutput);
        ThreadFactory.launchWork(threadPool, errTransfer);

        CompositeOutputStream actOutput = new CompositeOutputStream(logger);
        if (addOutput != null) actOutput.addOutput(addOutput);
        if (showExecOutput) actOutput.addOutput(new NotClosingOutputStream(System.out));
        StreamHelper.transfer(proc.getInputStream(), actOutput, 4*1024);

        int procCode = proc.waitFor();
        if (errTransfer.getException() != null) throw new RuntimeException(errTransfer.getException());
        String warn = null;
        if (!noErrorStreamException && errForLog.size() > 0) {
            warn = new String(errForLog.toByteArray());
            if (addOutput != null && errForLogLimiter.getLineCount() > cWarnLinesCount) {
                warn += "\n==== SEE MORE ERRORS IN DATA LOG ====";
            }
        }
        if (procCode != 0) {
            logger.warning(warn);
            throw new CourierException("Shell command returned code " + procCode);
        } else {
            if (warn != null) {
                throw new CourierException("Shell command returned errors:\n" + warn);
            }
        }
    }

    private void renameCommand(List<String> pars) throws SQLException {
        if (pars.size() < 2) {
            throw new SQLException(
                "Rename requires 2 parameters but only " + pars.size() + " provided");
        }
        File fFrom = new File(pars.get(0));
        File fTo = new File(pars.get(1));
        File fToDir = fTo.getParentFile();
        if (!fToDir.exists()) {
            if (!fToDir.mkdirs()) {
                throw new SQLException("Failed to create dir '" + fToDir.getPath() + "'");
            }
        }
        if (fTo.exists()) {
            if (!fTo.delete()) throw new SQLException("Failed deleting " + fTo.getAbsolutePath());
        }
        if (!fFrom.renameTo(fTo)) {
            throw new SQLException(
                "Failed to rename file '" + fFrom.getPath() +
                 "' to '" + fTo.getPath() + "'"
            );
        }
    }

    private UpdateResult singleDeleteCommand(List<String> pars) throws SQLException {
        if (pars.size() < 1) throw new SQLException("File not specified");
        File f = new File(pars.get(0));
        if (f.exists()) {
            if (!f.delete()) {
                throw new SQLException(
                    "Failed to delete file '" + f.getPath() + "'"
                );
            }
        }
        return new CorrectUpdateResult(1);
    }

    private UpdateResult bulkRenameCommand(List<String> pars) throws SQLException {
        if (pars.size() < 2) throw new SQLException("Folders not specified");
        final File sdir = new File(pars.get(0));
        if (!sdir.exists()) return new CorrectUpdateResult(0);

        final File tdir = new File(pars.get(1));
        final String nameRe = (pars.size() > 2) ? pars.get(2) : ".*";
        int updateCount = 0;
        if (!tdir.exists()) {
            if (!tdir.mkdirs()) throw new SQLException("Failed to create: " + tdir.getAbsolutePath());
        }

        FilenameFilter nameFilter = new FileNameMatcher(nameRe);
        File[] fl = sdir.listFiles(nameFilter);
        if (fl == null) return new CorrectUpdateResult(0);

        for (File listFile : fl) {
            File fTo = new File(tdir, listFile.getName());
            if (fTo.exists()) fTo.delete();
            if (listFile.renameTo(fTo)) {
                updateCount++;
            } else {
                if (fTo.exists()) {
                    m_logger.warning("Rename call returned false but file exists: " + listFile.getAbsolutePath());
                } else {
                    m_logger.error("Could not rename " + listFile.getAbsolutePath());
                }
            }
        }
        return new CorrectUpdateResult(updateCount);
    }

    private UpdateResult deleteCommand(List<String> pars) throws SQLException {
        int updateCount = 0;
        if (pars.size() < 1) {
            throw new SQLException("Files to delete not specified");
        }
        for (String fullTemplate : pars) {
            int posSlash = fullTemplate.lastIndexOf('/');
            posSlash = Math.max(posSlash, fullTemplate.lastIndexOf('\\'));
            String nameTemplate = fullTemplate.substring(posSlash + 1);
            File dir = new File(fullTemplate.substring(0, posSlash));
            File[] listFiles;
            if (nameTemplate.length() > 0) {
                nameTemplate = StringHelper.replace(nameTemplate, '.', "\\.");
                nameTemplate =
                        "^" + StringHelper.replace(nameTemplate, '*', ".*") + "$";
                FilenameFilter nameFilter = new FileNameMatcher(nameTemplate);
                listFiles = dir.listFiles(nameFilter);
            } else {
                listFiles = dir.listFiles();
            }
            for (File listFile : listFiles) {
                if (listFile.delete()) {
                    updateCount++;
                } else {
                    m_logger.warning("Could not delete " + listFile.getAbsolutePath());
                }
            }
        }
        return new CorrectUpdateResult(updateCount);
    }

}
