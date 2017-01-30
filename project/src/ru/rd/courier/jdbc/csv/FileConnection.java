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

import ru.rd.courier.jdbc.ConnectionDrivenJdbcStatement;
import ru.rd.courier.jdbc.ConnectionSkeleton;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.jdbc.databuffer.StringColumnInfo;
import ru.rd.courier.logging.DaysFileLogHandler;
import ru.rd.courier.utils.LineReader;
import ru.rd.courier.utils.StringHelper;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.Charset;

public abstract class FileConnection extends ConnectionSkeleton {
    public static final int c_PseudoFieldsCount = 1;
    private static int s_lastConNumber = 0;

    private final File m_dir;
    private final FileSelector m_fileSelector;
    private final boolean m_holdFileUntilNew;
    private final int m_fullReadEvery;
    private final long m_fullReadInterval;
    protected LineSplitter m_lineSplitter;
    private final int m_headerRow;
    private final boolean m_addAbsentAsNull;
    private final LineFilter m_filter;
    private final String m_titleVarName;
    private final String m_charSet;

    protected String[] m_cols;
    private String m_curFile = null;
    protected long m_curPos;
    private int m_curLineNum;
    private long m_fileInitTime;
    private int m_sameFileReadCount;
    private String m_title;
    private long m_lastLength;

    public static final String c_dirParam = "DirName";
    public static final String c_filePrefixParam = "FilePrefix";
    public static final String c_filePostfixParam = "FilePostfix";
    public static final String c_dateFormatParam = "DateFormat";
    public static final String c_holdFileUntilNewParam = "HoldFileUntilNew";
    public static final String c_fullReadEveryParam = "FullReadEvery";
    public static final String c_fullReadInterval = "FullReadInterval";
    public static final String c_lineSplitter = "LineSplitter";
    public static final String c_stringBracketParam = "StringBracket";
    public static final String c_separatorParam = "Separator";
    public static final String c_needToTrimParam = "NeedToTrim";
    public static final String c_nullWordParam = "NullWord";
    public static final String c_charSetName = "charset";
    public static final String c_headerRowParam = "HeaderRow";
    public static final String c_titleVarName = "TitleVarName";
    public static final String c_addAbsentAsNull = "AddAbsentAsNull";
    public static final String c_skipTemplate = "SkipTemplate";
    public static final String c_numbericColumnTitles = "NumbericColumnTitles";
    public static final String c_colTitlePrefix = "ColumnTitlePrefix";

    public static final String c_lineNumberTitle = "LineNumber";

    protected static void checkParam(Properties props, String parName) throws SQLException {
        if (!props.containsKey(parName)) {
            throw new SQLException("Unspecified parameter '" + parName + "'");
        }
    }

    public FileConnection(Properties props) throws SQLException {
        super(++s_lastConNumber);

        checkParam(props, c_dirParam);

        m_dir = new File(props.getProperty(c_dirParam));
        if (!m_dir.exists() || !m_dir.isDirectory()) {
            throw new SQLException("Invalid catalog '" + m_dir.getAbsolutePath() + "'");
        }

        m_logger = getLoggerFromUrl(props);
        if (m_logger == null) {
            m_logger = Logger.getAnonymousLogger();
            m_logger.setUseParentHandlers(false);
            m_logger.addHandler(new DaysFileLogHandler(
                m_dir, "yyyy-MM-dd", 2, "log_", ".log", true
            ));
        }

        checkParam(props, c_filePrefixParam);
        String filePrefix = props.getProperty(c_filePrefixParam);
        String filePostfix = StringHelper.stringParam(props, c_filePostfixParam, "");

        DateFormat dateFormat;
        if (props.containsKey(c_dateFormatParam)) {
            dateFormat = new SimpleDateFormat(props.getProperty(c_dateFormatParam));
        } else {
            dateFormat = new SimpleDateFormat("yyyyMMdd");
        }

        m_fileSelector = new StdFileSelector(filePrefix, filePostfix, dateFormat);

        if (props.containsKey(c_holdFileUntilNewParam)) {
            m_holdFileUntilNew = props.getProperty(
                c_holdFileUntilNewParam).equalsIgnoreCase("yes");
        } else {
            m_holdFileUntilNew = false;
        }

        m_fullReadEvery = StringHelper.intParam(props, c_fullReadEveryParam, 0);
        m_fullReadInterval = StringHelper.intParam(props, c_fullReadInterval, 0);
        m_headerRow = StringHelper.intParam(props, c_headerRowParam, 0);
        m_titleVarName = StringHelper.stringParam(props, c_titleVarName, null);

        if ((m_headerRow > 1) && (m_titleVarName == null)) {
            throw new SQLException(
                c_titleVarName + " required if " + c_headerRowParam + " is specified"
            );
        }

        m_addAbsentAsNull = StringHelper.boolParam(props, c_addAbsentAsNull, false);

        if (props.containsKey(c_skipTemplate)) {
            m_filter = new TemplateFilter(props.getProperty(c_skipTemplate));
        } else {
            m_filter = new FakeFilter();
        }
        m_charSet = StringHelper.stringParam(
            props, c_charSetName, Charset.defaultCharset().name()
        );
        m_lastLength = 0;
    }

    interface FileSelector {
        String getFileName();
    }

    private class StdFileSelector implements FileSelector {
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

    private static Reader getReader(File file, String charSet) throws FileNotFoundException, UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(
            new FileInputStream(file), charSet
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
        return (f.length() > m_lastLength);
    }

    private boolean isNewFileName(String fileName) {
        return (m_curFile == null) || !m_curFile.equals(fileName);
    }

    private class LineCounter {
        private int m_linesCount;
        private long m_startPos;
        private long m_lastLineEndPos;

        public LineCounter(
            Reader r, final long startPos, boolean lastChunk
        ) throws IOException {
            if (r == null)
                throw new IllegalArgumentException("Reader cannot be null");
            init(r, startPos, lastChunk);
        }

        private String readLine(Reader r) throws IOException {
            StringBuffer sb = new StringBuffer();
            int d;
            while (true) {
                d = r.read();
                if (d < 0 || d == 10 || d == 13) break;
                sb.append((char)d);
            }
            return sb.toString();
        }

        private void init(
            Reader r, final long startPos, boolean lastChunk
        ) throws IOException {
            m_linesCount = 0;
            m_lastLineEndPos = startPos;

            long pos = startPos;
            int d;

            // skip end line chars
            while (true) {
                d = r.read();
                if (d < 0) return;
                pos++;
                if ((d != 10) && (d != 13)) break;
            }

            m_startPos = pos - 1;
            StringBuffer sb =  new StringBuffer();
            long lastLineBegPos = m_startPos;
            do {
                d = r.read();
                if (d < 0) break;
                pos++;
                if (d == 10) {
                    m_linesCount++;
                    lastLineBegPos = pos;
                    sb.setLength(0);
                } else if (d != 13) {
                    sb.append((char)d);
                }
            } while(true);

            m_lastLineEndPos = pos;
            // check last line completeness
            if (pos <= lastLineBegPos) return;

            if (lastChunk) {
                m_linesCount++;
            } else {
                String lastLine = sb.toString();
                if ((m_filter == null) || !m_filter.skip(lastLine)) {
                    String[] fields = null;
                    try {
                        fields = m_lineSplitter.parse(new StringBuffer(lastLine));
                    } catch (Exception e) {
                        m_logger.log(Level.WARNING, e.getMessage(), e);
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
        m_logger.fine("Enter FileConnection.initFile: fileName = " + fileName);

        setFileProps(null, null);
        File f = getFile(fileName);
        if (!fileExists(f)) {
            m_logger.fine(
                "FileConnection.initFile: file '" +
                f.getAbsolutePath() + "' does not exist"
            );
            fileName = null;
        }
        if (fileName == null) return;
        LineReader lr = new LineReader(getReader(f, m_charSet));
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
                m_logger.fine(
                    "FileConnection.initFile: file does not have full title"
                );
                return;
            }
            StringHelper.trimLFCR(sb);
            title = sb.toString();
        }
        m_curPos = lr.getBytesRead();
        if (initColumns(lr)) setFileProps(fileName, title);
    }

    protected abstract boolean initColumns(LineReader lr) throws IOException;

    private void setFileProps(String fileName, String title) {
        m_sameFileReadCount = 0;
        m_fileInitTime = System.currentTimeMillis();
        m_title = title;
        m_curLineNum = 1;
        m_curFile = fileName;
    }

    private ResultSet getNextChunk(Statement stmt, boolean lastChunk)
    throws IOException, SQLException {
        m_logger.fine(
            "Enter FileConnection.getNextChunk: lastChunk = " +
            lastChunk + " cur-file=" + m_curFile
        );

        if (m_curFile == null) return null;
        m_sameFileReadCount++;
        File f = getFile(m_curFile);

        Reader r = new InputStreamReader(
            new BufferedInputStream(new FileInputStream(f)), m_charSet
        );
        LineCounter lc;
        int oldLineNum;
        Properties constFields;
        try {
            long skipped = r.skip(m_curPos);
            if (skipped < m_curPos) return null;


            lc = new LineCounter(r, m_curPos, lastChunk);
            m_curPos = lc.getLastLinePos();
            oldLineNum = m_curLineNum;
            m_curLineNum += lc.getLineCount();
            constFields = new Properties();
            if (m_title != null) {
                constFields.put(m_titleVarName, m_title);
            }
        } finally {
            r.close();
        }
        r = new InputStreamReader(
            new BufferedInputStream(new FileInputStream(f)), m_charSet
        );
        r.skip(lc.getStartPos());
        return createResultSet(
            m_logger, stmt,
            new ResultSetInfo(
                c_lineNumberTitle, m_cols,
                lc.getLineCount(), r,
                m_lineSplitter,
                oldLineNum, m_addAbsentAsNull, m_filter,
                constFields
            )
        );
    }

    protected abstract ResultSet createResultSet(
        Logger logger, Statement stmt, ResultSetInfo rsInfo
    ) throws SQLException;

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

    private synchronized ResultSet getResultSet(Statement stmt) throws IOException, SQLException {
        String newName = getFileName();

        m_logger.fine(
            "FileConnection.getResultSetInfo enter:" +
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

        if (m_curFile == null) {
            m_lastLength = 0;
        } else {
            m_lastLength = getFile(m_curFile).length();
        }

        m_logger.fine(
            "FileConnection.getResultSetInfo exit: cur-file=" + m_curFile +
            " cur-pos=" + m_curPos + " line-num=" + m_curLineNum
        );

        return rs;
    }

    private static ResultSet getEmptyResultSet(Statement stmt) {
        DataBufferResultSet rs = new DataBufferResultSet();
        rs.setStatement(stmt);
        return rs;
    }

    private void checkConnection() throws SQLException {
        if (!m_dir.exists()) {
            throw new SQLException("GLTrade connection failed");
        }
    }

    protected List innerRequest(ConnectionDrivenJdbcStatement stmt, String request) throws SQLException {
        checkConnection();
        try {
            List<Object> res = new LinkedList<Object>();
            request = request.trim().toLowerCase();

            if (request.equals("check")) {
                res.add(getEmptyResultSet(stmt));
                return res;
            } else if (request.equals("last-file")) {
                DataBufferResultSet rs = new DataBufferResultSet();
                rs.addColumn(new StringColumnInfo("AbsolutePath", 512));
                rs.addRecord();
                synchronized (this) {
                    rs.updateString(
                        1,
                        m_curFile == null ? null : getFile(m_curFile).getAbsolutePath()
                    );
                }
                rs.beforeFirst();
                rs.setStatement(stmt);
                res.add(rs);
                return res;
            }

            ResultSet rs = getResultSet(stmt);
            if (rs == null) rs = getEmptyResultSet(stmt);
            res.add(rs);
            return res;
        } catch (IOException e) {
            SQLException ne = new SQLException(e.getMessage());
            ne.initCause(e);
            throw ne;
        }
    }

    /*
    private void fillDataBuffer(Reader r, DataBuffer db) throws IOException, SQLException {
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
                throw new SQLException("Wrong fields count in line '" + line + "'");
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
}
