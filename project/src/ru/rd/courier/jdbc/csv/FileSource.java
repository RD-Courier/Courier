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
package ru.rd.courier.jdbc.csv;

import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.utils.LineReader;
import ru.rd.courier.utils.StringHelper;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 02.02.2006
 * Time: 18:32:02
 */
public abstract class FileSource extends TimedStringReceiver implements DataSource {
    private final CourierLogger m_logger;
    public static final int c_PseudoFieldsCount = 1;

    private final File m_dir;
    private final FileSelector m_fileSelector;
    private final boolean m_holdFileUntilNew;
    private final int m_fullReadEvery;
    private final long m_fullReadInterval;
    private final int m_headerRow;
    private final boolean m_addAbsentAsNull;
    private final LineFilter m_filter;
    private final String m_titleVarName;
    private final String m_charSet;
    protected LineSplitter m_lineSplitter;

    protected String[] m_cols;
    private String m_curFile = null;
    protected long m_curPos;
    private int m_curLineNum;
    private long m_fileInitTime;
    private int m_sameFileReadCount;
    private String m_title;

    public static final String c_lineNumberTitle = "LineNumber";

    protected static void checkParam(Properties props, String parName) throws CourierException {
        if (!props.containsKey(parName)) {
            throw new CourierException("Unspecified parameter '" + parName + "'");
        }
    }

    public FileSource(
        CourierLogger logger, String dir,
        FileSelector fileSelector, boolean holdFileUntilNew,
        int fullReadEvery, long fullReadInterval,
        int headerRow, String titleVarName,
        boolean addAbsentAsNull, LineFilter filter, String charSet
    ) throws CourierException {
        super();
        m_logger = logger;

        m_dir = new File(dir);
        if (!m_dir.exists() || !m_dir.isDirectory()) {
            throw new CourierException("Invalid catalog '" + m_dir.getAbsolutePath() + "'");
        }

        m_fileSelector = fileSelector;
        m_holdFileUntilNew = holdFileUntilNew;

        m_fullReadEvery = fullReadEvery;
        m_fullReadInterval = fullReadInterval;
        m_headerRow = headerRow;
        m_titleVarName = titleVarName;

        if ((m_headerRow > 1) && (m_titleVarName == null)) {
            throw new CourierException(
                "TitleVarName required if HeaderRowParam is specified"
            );
        }

        m_addAbsentAsNull = addAbsentAsNull;
        m_filter = filter;
        m_charSet = charSet;
    }

    public interface FileSelector {
        String getFileName();
    }

    public static class StdFileSelector implements FileSelector {
        private final String m_filePrefix;
        private final String m_filePostfix;
        private final DateFormat m_dateFormat;

        public StdFileSelector(
            String filePrefix, String filePostfix, DateFormat dateFormat
        ) {
            m_filePrefix = filePrefix;
            m_filePostfix = filePostfix;
            m_dateFormat = dateFormat;
        }

        public String getFileName() {
            return m_filePrefix + m_dateFormat.format(new Date()) + m_filePostfix;
        }
    }

    public static class ConstFileSelector implements FileSelector {
        private final String m_fileName;

        public ConstFileSelector(String fileName) {
            m_fileName = fileName;
        }

        public String getFileName() {
            return m_fileName;
        }
    }

    String getFileName() {
        String newName = m_fileSelector.getFileName();
        if (
            m_holdFileUntilNew &&
            m_curFile != null &&
            !getFile(newName).exists() &&
            getFile(m_curFile).exists()
        ) {
            return m_curFile;
        }
        return newName;
    }

    private File getFile(String fileName) {
        return new File(m_dir, fileName);
    }

    private static Reader getReader(File file) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(
            new FileInputStream(file)
        ));
    }

    private static Reader getReader(RandomAccessFile file, String charSet) throws IOException {
        if (charSet == null) {
            return new BufferedReader(new InputStreamReader(
                new FileInputStream(file.getFD())
            ));
        } else {
            return new BufferedReader(new InputStreamReader(
                new FileInputStream(file.getFD()), charSet
            ));
        }
    }

    private boolean hasMoreData() {
        if (m_curFile == null) return false;
        File f = getFile(m_curFile);
        if (!fileExists(f)) return false;
        return (f.length() > m_curPos);
    }

    private boolean isNewFileName(String fileName) {
        return (m_curFile == null) || !m_curFile.equals(fileName);
    }

    private class LineCounter {
        private int m_linesCount;
        private long m_startPos;
        private long m_lastLineEndPos;

        public LineCounter(
            File file, final long startPos, boolean lastChunk
        ) throws IOException {
            if (file == null)
                throw new IllegalArgumentException("File cannot be null");
            if (startPos > file.length()) {
                throw new IllegalArgumentException(
                    "Start pos = " + startPos +
                    " > file length = " + file.length()
                );
            }

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            try {
                init(raf, startPos, lastChunk);
            } finally {
                raf.close();
            }
        }

        private void init(
            RandomAccessFile raf, final long startPos, boolean lastChunk
        ) throws IOException {
            m_linesCount = 0;
            m_lastLineEndPos = startPos;

            raf.seek(startPos);
            InputStream is = new BufferedInputStream(new FileInputStream(raf.getFD()));
            long pos = startPos;
            int d;

            // skip end line chars
            while (true) {
                d = is.read();
                if (d < 0) return;
                pos++;
                if ((d != 10) && (d != 13)) break;
            }

            m_startPos = pos - 1;
            long lastLineBegPos = m_startPos;
            do {
                d = is.read();
                if (d < 0) break;
                pos++;
                if (d == 10) {
                    m_linesCount++;
                    lastLineBegPos = pos;
                }
            } while(true);

            m_lastLineEndPos = pos;
            // check last line completeness
            if (pos <= lastLineBegPos) return;

            if (lastChunk) {
                m_linesCount++;
            } else {
                raf.seek(lastLineBegPos);
                String lastLine = raf.readLine();
                if ((m_filter == null) || !m_filter.skip(lastLine)) {
                    String[] fields = null;
                    try {
                        fields = m_lineSplitter.parse(new StringBuffer(lastLine));
                    } catch (Exception e) {
                        m_logger.warning(e.getMessage(), e);
                    }
                    if (
                        !m_addAbsentAsNull &&
                        ((fields == null) ||
                            (fields.length < (m_cols.length - c_PseudoFieldsCount)))
                    ) {
                        m_lastLineEndPos = lastLineBegPos;
                    } else {
                        m_linesCount++;
                    }
                } else {
                    m_linesCount++;
                }
            }
        }

        public int getLineCount() {
            return m_linesCount;
        }

        public long getStartPos() {
            return m_startPos;
        }

        public long getLastLinePos() {
            return m_lastLineEndPos;
        }
    }

    private void initFile(String fileName) throws IOException {
        m_logger.debug("Enter FileSource.initFile: fileName = " + fileName);

        m_curFile = null;
        File f = getFile(fileName);
        if (!fileExists(f)) {
            m_logger.debug(
                "FileSource.initFile: file '" +
                f.getAbsolutePath() + "' does not exist"
            );
            fileName = null;
        }
        if (fileName == null) {
            setFileProps(null, null);
            return;
        }
        LineReader lr = new LineReader(getReader(f));
        try {
            initFileFromLineReader(fileName, lr);
        } finally {
            lr.close();
        }
    }

    private void initFileFromLineReader(String fileName, LineReader lr) throws IOException {
        StringBuffer sb = new StringBuffer();
        String title = null;
        if (m_headerRow > 1) {
            if (lr.appendFullLines(sb, m_headerRow - 1) < (m_headerRow - 1)) {
                m_logger.debug(
                    "FileSource.initFile: file does not have full title"
                );
                return;
            }
            StringHelper.trimLFCR(sb);
            title = sb.toString();
        }
        m_curPos = lr.getBytesRead();
        initColumns(lr);
        setFileProps(fileName, title);
    }

    protected abstract void initColumns(LineReader lr) throws IOException;

    private void setFileProps(String fileName, String title) {
        m_sameFileReadCount = 0;
        m_fileInitTime = System.currentTimeMillis();
        m_title = title;
        m_curLineNum = 1;
        m_curFile = fileName;
    }

    private ResultSet getNextChunk(Statement stmt, boolean lastChunk)
        throws IOException, SQLException {
        m_logger.debug(
            "Enter FileSource.getNextChunk: lastChunk = " +
            lastChunk + " cur-file=" + m_curFile
        );

        m_sameFileReadCount++;
        if (!hasMoreData()) return null;
        File f = getFile(m_curFile);
        LineCounter lc = new LineCounter(f, m_curPos, lastChunk);
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        raf.seek(lc.getStartPos());
        m_curPos = lc.getLastLinePos();
        int oldLineNum = m_curLineNum;
        m_curLineNum += lc.getLineCount();
        Properties constFields = new Properties();
        if (m_title != null) {
            constFields.put(m_titleVarName, m_title);
        }

        return new CsvStringResultSet(
            m_logger, stmt,
            new ResultSetInfo(
                c_lineNumberTitle, m_cols,
                lc.getLineCount(), getReader(raf, m_charSet),
                m_lineSplitter,
                oldLineNum, m_addAbsentAsNull, m_filter,
                constFields
            )
        );
    }

    private boolean fileExists(File f) {
        f.getParentFile().list();
        return f.exists();
    }

    private boolean needToChangeFile(String name) {
        return isNewFileName(name);
    }

    private boolean rereadSameFile() {
        return
            (m_fullReadEvery > 0 && m_sameFileReadCount >= m_fullReadEvery)
        ||
            (   m_fullReadInterval > 0 &&
                (System.currentTimeMillis() - m_fileInitTime) >= m_fullReadInterval
            );
    }

    private ResultSet getResultSet(Statement stmt) throws IOException, SQLException {
        String newName = getFileName();

        m_logger.debug(
            "FileSource.getResultSetInfo enter:" +
            " old file=" + m_curFile + " new file=" + newName +
            " curPos=" + m_curPos + " lineNum=" + m_curLineNum
        );

        boolean lastChunk = false;
        String initName = null;
        if (needToChangeFile(newName)) {
            if (hasMoreData()) {
                lastChunk = true;
            } else {
                initName = newName;
            }
        } else {
            if (rereadSameFile()) initName = m_curFile;
        }

        if (initName != null) initFile(initName);
        ResultSet rs = getNextChunk(stmt, lastChunk);

        m_logger.debug(
            "FileSource.getResultSetInfo exit: cur-file=" + m_curFile +
            " cur-pos=" + m_curPos + " line-num=" + m_curLineNum
        );

        return rs;
    }

    private static ResultSet getEmptyResultSet(Statement stmt) {
        DataBufferResultSet rs = new DataBufferResultSet();
        rs.setStatement(stmt);
        return rs;
    }

    private void checkConnection() throws CourierException {
        if (!m_dir.exists()) {
            throw new CourierException("GLTrade connection failed");
        }
    }

    private ResultSet innerRequest(String request) throws CourierException, SQLException {
        checkConnection();
        try {
            request = request.trim().toLowerCase();

            if (request.equals("check")) {
                return getEmptyResultSet(null);
            } else if (request.equals("last-file")) {
                DataBufferResultSet rs = new DataBufferResultSet();
                rs.addColumn(new StringColumnInfo("AbsolutePath", 512));
                rs.beforeFirst();
                rs.updateString(
                    1,
                    m_curFile == null ? null : getFile(m_curFile).getAbsolutePath()
                );
                rs.setStatement(null);
                return rs;
            }

            ResultSet rs = getResultSet(null);
            if (rs == null) rs = getEmptyResultSet(null);
            return rs;
        } catch (IOException e) {
            CourierException ne = new CourierException(e.getMessage());
            ne.initCause(e);
            throw ne;
        }
    }

    /*
    private void fillDataBuffer(Reader r, DataBuffer db) throws IOException, CourierException {
        if (!initFile(r)) return;
        StringBuffer line = new StringBuffer(64);
        if (!StringHelper.readLine(r, line)) return;
        String[] fields;
        fields = m_headSplitter.parse(line);
        int colCount = fields.length;
        for (int i = 0; i < fields.length; i++) {
            db.addColumn(new StringColumnInfo(fields[i], 256));
        }
        int lineCount = 0;
        do {
            lineCount++;
            fields = m_headSplitter.parse(line);
            if (fields.length != colCount) {
                throw new CourierException("Wrong fields count in line '" + line + "'");
            }
            db.addRecord();
            db.updateString(1, Integer.toString(lineCount));
            for (int i = 0; i < fields.length; i++) {
                db.updateString(i + 2, fields[i]);
            }
        } while(StringHelper.readLine(r, line));
    }
    */

    public String toString() {
        return "dir = " + m_dir + " file = " + m_curFile;
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        throw new UnsupportedOperationException();
    }

    protected List timedFlush() {
        return null;
    }

    protected void timedClose() throws CourierException {
    }

    public void setTimeout(int timeout) throws CourierException {
    }

    public void cancel() throws CourierException {
    }

    public ResultSet request(String query) throws CourierException {
        try {
            return innerRequest(query);
        } catch (SQLException e) {
            throw new CourierException(e);
        }
    }
}
