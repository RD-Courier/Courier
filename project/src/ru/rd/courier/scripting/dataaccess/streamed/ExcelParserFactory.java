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

import org.w3c.dom.Node;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.ReqExpChecker;
import ru.rd.courier.utils.StringChecker;

import java.util.List;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 12.12.2006
 * Time: 16:00:04
 */
public class ExcelParserFactory implements StreamParserFactory {
    private final String m_sheetName;
    private final int m_leftColumn;
    private final int m_rightColumn;
    private final int m_captionRow;
    private final int m_dataRow;
    private final List<ExcelParser.ConstField> m_constFields;
    private final boolean m_needToTrim;
    private final String m_skipTemplate;
    private final boolean m_stopAtBlankCaption;
    private final boolean m_blankCaptionAsNumber;
    private final String m_blankCaptionPrefix;
    private final String m_dateFormat;
    private final String m_numberFormat;

    public static int adjustDim(int v) {
        return v - 1;
    }

    public static int unadjustDim(int v) {
        return v + 1;
    }

    private static int getRawColumnIndex(
        Node conf, String name, boolean required, int def
    ) {
        final String nameSuffix = "-letters";
        String strIndex;

        strIndex = DomHelper.getNodeAttr(conf, name, false);
        if (strIndex != null) return Integer.parseInt(strIndex);

        strIndex = DomHelper.getNodeAttr(conf, name + nameSuffix, false);
        if (strIndex != null) {
            return ExcelParser.columnStrPosToNumber(strIndex) + 1;
        }

        if (required) {
            throw new RuntimeException(
                "Column option '" + name + "' or '" + name + nameSuffix + "' is required");
        }
        return def;
    }

    private static int getColumnIndex(
        Node conf, String name, boolean required, int def
    ) {
        return adjustDim(getRawColumnIndex(conf, name, required, def));
    }

    private static final int absentCaptionRow = 0;

    private static int getFinishRow(Node conf, String name, int def) {
        if (!DomHelper.hasAttr(conf, name)) return def;
        String v = DomHelper.getNodeAttr(conf, name);
        if (v.equalsIgnoreCase("unlimited")) return -1;
        return Integer.parseInt(v);
    }

    private static int getFinishCol(Node conf, String name, int def) {
        String v = DomHelper.getNodeAttr(conf, name, null);
        if (v != null && v.equalsIgnoreCase("unlimited")) return -1;
        return getColumnIndex(conf, name, false, unadjustDim(def));
    }

    public ExcelParserFactory(Node conf) {
        m_sheetName = DomHelper.getNodeAttr(conf, "sheet-name", null);
        m_leftColumn = getColumnIndex(conf, "left-column", false, 1);
        m_rightColumn = getColumnIndex(conf, "right-column", false, 0);
        int captionRow = DomHelper.getIntNodeAttr(conf, "caption-row", absentCaptionRow);
        int dataRow = DomHelper.getIntNodeAttr(conf, "data-row", captionRow + 1);
        m_captionRow = adjustDim(captionRow);
        m_dataRow = adjustDim(dataRow);
        m_constFields = new LinkedList<ExcelParser.ConstField>();
        Node n = DomHelper.getChild(conf, "const-fields", false);
        if (n != null) {
            for (Node fn: DomHelper.getChildrenByTagName(n, "field", false)) {
                ExcelParser.ConstField cf = new ExcelParser.ConstField(
                    DomHelper.getNodeAttr(fn, "name"),
                    DomHelper.getNodeAttr(fn, "sheet-name", null),
                    getColumnIndex(fn, "column", true, 0),
                    adjustDim(DomHelper.getIntNodeAttr(fn, "row"))
                );
                cf.setFinishRow(getFinishRow(fn, "finish-row", cf.getFinishRow()));
                cf.setFinishCol(getFinishCol(fn, "finish-column", cf.getFinishCol()));
                cf.setPattern(DomHelper.getNodeAttr(fn, "pattern", cf.getPattern()));
                m_constFields.add(cf);
            }
        }
        m_needToTrim = DomHelper.getBoolYesNo(conf, "need-to-trim", true);
        m_stopAtBlankCaption = DomHelper.getBoolYesNo(conf, "stop-at-blank-caption", false);
        m_skipTemplate = DomHelper.getNodeAttr(conf, "skip-caption-reg-exp", false);
        m_blankCaptionAsNumber = DomHelper.getBoolYesNo(conf, "caption-as-number", false);
        m_blankCaptionPrefix = DomHelper.getNodeAttr(conf, "caption-prefix", "Column");
        m_dateFormat = DomHelper.getNodeAttr(conf, "date-format", null);
        m_numberFormat = DomHelper.getNodeAttr(conf, "number-format", null);
    }

    public StreamParser createParser() {
        return new ExcelParser(
            m_sheetName, m_leftColumn, m_rightColumn,
            m_captionRow, m_dataRow, m_constFields, m_needToTrim,
            m_stopAtBlankCaption,
            m_skipTemplate == null ?
                new StringChecker() {
                    public boolean isTrue(String str) { return false; }
                }
                : new ReqExpChecker(m_skipTemplate),
            m_blankCaptionAsNumber, m_blankCaptionPrefix,
            m_dateFormat, m_numberFormat
        );
    }
}
