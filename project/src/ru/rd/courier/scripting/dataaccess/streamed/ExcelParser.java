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
package ru.rd.courier.scripting.dataaccess.streamed;

import jxl.*;
import jxl.read.biff.BiffException;
import ru.rd.courier.jdbc.ResultSets.IterColumnInfo;
import ru.rd.courier.jdbc.ResultSets.StringBufferedResultSet;
import ru.rd.courier.utils.StringChecker;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.courier.utils.ReqExpChecker;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: AStepochkin
 * Date: 12.12.2006
 * Time: 14:00:48
 */
public class ExcelParser implements StreamParser {
    private final String m_sheetName;
    private int m_leftColumn;
    private final int m_rightColumn;
    private int m_captionRow;
    private int m_dataRow;
    private final List<ConstField> m_constFields;
    private final boolean m_needToTrim;
    private final boolean m_stopAtBlankCaption;
    private final StringChecker m_skipCaptionSelector;
    private final boolean m_blankCaptionAsNumber;
    private final String m_blankCaptionPrefix;
    private final String m_dateFormat;
    private final String m_numberFormat;

    public static class ConstField {
        private final String m_name;
        private final String m_sheetName;
        private final int m_col, m_row;
        private int m_finishCol, m_finishRow;
        private String m_pattern;

        public ConstField(String _name, String _sheetName, int _col, int _row) {
            m_name = _name;
            m_sheetName = _sheetName;
            m_col = _col;
            m_row = _row;
            m_finishCol = m_col;
            m_finishRow = m_row;
            m_pattern = null;
        }

        private static final StringChecker s_anyString = new StringChecker() {
            public boolean isTrue(String str) { return true; }
        };

        public String getValue(ExcelResultSet rs, Workbook b) {
            Sheet s = (m_sheetName == null) ? rs.getSheet() : b.getSheet(m_sheetName);
            StringChecker checker = (m_pattern == null) ? s_anyString : new ReqExpChecker(m_pattern);

            int fRow = m_finishRow;
            if (fRow < 0 || fRow >= s.getRows()) fRow = s.getRows() - 1;
            int fCol = m_finishCol;
            if (fCol < 0 || fCol >= s.getColumns()) fCol = s.getColumns() - 1;

            for (int r = m_row; r <= fRow; r++) {
                for (
                    int c = (r == m_row) ? m_col : 0;
                    c <= ((r == fRow) ? fCol : s.getColumns() - 1);
                    c++
                ) {
                    Cell cell = s.getCell(c, r);
                    String cont = rs.getCellContent(cell);
                    if (checker.isTrue(cont)) return cont;
                }
            }
            return "";
        }

        public String getName() {
            return m_name;
        }

        public void setFinishRow(int row) {
            m_finishRow = row;
        }

        public int getFinishRow() {
            return m_finishRow;
        }

        public void setFinishCol(int col) {
            m_finishCol = col;
        }

        public int getFinishCol() {
            return m_finishCol;
        }

        public void setPattern(String pattern) {
            m_pattern = pattern;
        }

        public String getPattern() {
            return m_pattern;
        }
    }

    public ExcelParser(
        String sheetName, int leftColumn, int rightColumn,
        int captionRow, int dataRow,
        List<ConstField> constFields, boolean needToTrim,
        boolean stopAtBlankCaption, StringChecker skipCaptionSelector,
        boolean blankCaptionAsNumber, String blankCaptionPrefix,
        String dateFormat, String numberFormat
    ) {
        m_sheetName = sheetName;
        m_leftColumn = leftColumn;
        m_rightColumn = rightColumn;
        m_captionRow = captionRow;
        m_dataRow = dataRow;
        m_constFields = constFields;
        m_needToTrim = needToTrim;
        m_stopAtBlankCaption = stopAtBlankCaption;
        m_skipCaptionSelector = skipCaptionSelector;
        m_blankCaptionAsNumber = blankCaptionAsNumber;
        m_blankCaptionPrefix = blankCaptionPrefix;
        m_dateFormat = dateFormat;
        m_numberFormat = numberFormat;
    }

    private static final TimeZone gmtZone = TimeZone.getTimeZone("GMT");

    private static int intParam(Properties props, String name, int def) {
        return ExcelParserFactory.adjustDim(
            StringHelper.intParam(props, name, ExcelParserFactory.unadjustDim(def))
        );
    }

    public void parseProperties(StringSimpleParser p) {
        Properties props = p.getProperties(null, '\'', "|");
        m_leftColumn = intParam(props, "left-column", m_leftColumn);
        m_captionRow = intParam(props, "caption-row", m_captionRow);
        m_dataRow = intParam(props, "data-row", m_dataRow);
    }

    private static class ExcelResultSet extends StringBufferedResultSet {
        private Sheet m_sheet;
        private int m_row;
        private final boolean m_needToTrim;
        private final int[] m_positions;
        private final DateFormat m_df;
        private final NumberFormat m_nf;

        public ExcelResultSet(
            Workbook b, Sheet sheet, int top,
            List<IterColumnInfo> infos, List<Integer> positions,
            List<ConstField> consts, boolean needToTrim,
            String dateFormat, String numberFormat
        ) {
            super(null);
            if (infos.size() != positions.size()) {
                throw new RuntimeException("ColInfo count != Positions count");
            }
            m_sheet = sheet;
            m_row = top;
            m_needToTrim = needToTrim;
            m_positions = intListToArray(positions);
            if (dateFormat == null) {
                m_df = null;
            } else {
                m_df = new SimpleDateFormat(dateFormat);
                m_df.setTimeZone(gmtZone);
            }
            if (numberFormat == null) {
                m_nf = null;
            } else {
                m_nf = new DecimalFormat(numberFormat);
            }
            initWithConsts(
                infos.toArray(new IterColumnInfo[infos.size()]),
                formConstFields(b, consts)
            );
        }

        public Sheet getSheet() {
            return m_sheet;
        }

        private Map<String, String> formConstFields(Workbook b, List<ConstField> descs) {
            Map<String, String> res = new HashMap<String, String>();
            for (ConstField f: descs) res.put(f.getName(), f.getValue(this, b));
            return res;
        }

        private static int[] intListToArray(List<Integer> list) {
            int[] res = new int[list.size()];
            int i = 0;
            for (int p: list) {
                res[i] = p;
                i++;
            }
            return res;
        }

        protected boolean needToClearNulls() {
            return false;
        }

        private String getCellContent(Cell cell) {
            String data;
            if (cell.getType() == CellType.NUMBER) {
                double v = ((NumberCell)cell).getValue();
                if (m_nf == null) {
                    data = Double.toString(v);
                } else {
                    data = m_nf.format(v);
                }
            } else if (cell.getType() == CellType.DATE) {
                if (m_df == null) {
                    data = cell.getContents();
                } else {
                    data = m_df.format(((DateCell)cell).getDate());
                }
            } else {
                data = cell.getContents();
            }

            if (m_needToTrim) data = data.trim();
            return data;
        }

        protected boolean getRecord() throws SQLException {
            if (m_row >= m_sheet.getRows()) return false;

            for (int i = 0; i < getDynColCount(); i++) {
                Cell cell = m_sheet.getCell(m_positions[i], m_row);
                updateString(i + 1, getCellContent(cell));
            }
            m_row++;
            return true;
        }

        protected int skipRecords(int count) throws SQLException {
            int oldRow = m_row;
            m_row = Math.min(m_row + count, m_sheet.getRows());
            return m_row - oldRow;
        }
    }

    private static final String cAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static String columnPosToStr(int pos) {
        pos++;
        StringBuffer ret = new StringBuffer(1);
        int r = cAlphabet.length();
        while (pos > 0) {
            int chpos = pos % r - 1;
            if (chpos < 0) chpos = chpos + r;
            ret.insert(0, cAlphabet.charAt(chpos));
            pos = pos / r;
        }
        return ret.toString();
    }

    public static int columnStrPosToNumber(String strPos) {
        strPos = strPos.toUpperCase();
        int pos = 0;
        int i = 0;
        while (i < strPos.length()) {
            int chpos = cAlphabet.indexOf(strPos.charAt(i));
            if (chpos < 0) {
                throw new RuntimeException(
                    "Prohibited char '" + strPos.charAt(i) + "'");
            }
            pos = pos * cAlphabet.length() + chpos + 1;
            i++;
        }
        return pos - 1;
    }

    private void formCols(
        Sheet s, List<IterColumnInfo> cols, List<Integer> positions
    ) {
        for (int c = m_leftColumn; c < s.getColumns(); c++) {
            if (m_rightColumn >= 0 && c > m_rightColumn) break;
            String t = "";
            if (m_captionRow >= 0) {
                t = s.getCell(c, m_captionRow).getContents();
                t = (t == null) ? "" : t.trim();
                if (m_skipCaptionSelector.isTrue(t)) continue;
                if (m_stopAtBlankCaption) break;
            }
            if (t.length() == 0) {
                if (m_blankCaptionAsNumber) {
                    t = m_blankCaptionPrefix + Integer.toString(c);
                } else {
                    t = m_blankCaptionPrefix + columnPosToStr(c);
                }
            }
            cols.add(new IterColumnInfo(t));
            positions.add(c);
        }
    }

    public ResultSet parse(InputStream is) throws IOException {
        try {
            Workbook b;
            try {
              b = Workbook.getWorkbook(is);
            } finally {
                is.close();
            }
            Sheet s = (m_sheetName == null) ? b.getSheet(0) : b.getSheet(m_sheetName);
            List<IterColumnInfo> cols = new LinkedList<IterColumnInfo>();
            List<Integer> positions = new LinkedList<Integer>();
            formCols(s, cols, positions);
            return new ExcelResultSet(
                b, s, m_dataRow, cols, positions,
                m_constFields, m_needToTrim, m_dateFormat, m_numberFormat
            );
        } catch (BiffException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel() {}
}
