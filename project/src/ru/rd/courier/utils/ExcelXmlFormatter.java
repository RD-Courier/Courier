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
package ru.rd.courier.utils;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.biff.DisplayFormat;
import jxl.biff.FontRecord;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.*;
import jxl.format.Colour;
import jxl.format.Pattern;
import jxl.format.VerticalAlignment;
import jxl.write.Boolean;
import jxl.write.*;
import jxl.write.Number;
import jxl.write.biff.WritableWorkbookImpl;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Iterator;

/**
 * User: AStepochkin
 * Date: 04.04.2005
 * Time: 11:46:48
 */
public class ExcelXmlFormatter {
    public ExcelXmlFormatter() {}

    private static void cloneBorder(CellFormat f1, WritableCellFormat f2, Border b) throws WriteException {
        f2.setBorder(b, f1.getBorder(b), f1.getBorderColour(b));
    }

    private static class CellFormatEx {
        private final WritableCellFormat m_format;
        private final DisplayFormat m_dateFormat;
        private final DisplayFormat m_numberFormat;

        public CellFormatEx(WritableCellFormat format, DisplayFormat dateFormat, DisplayFormat numberFormat) {
            m_format = format;
            m_dateFormat = dateFormat;
            m_numberFormat = numberFormat;
        }

        public WritableCellFormat getFormat() {
            return m_format;
        }

        public DisplayFormat getDateFormat() {
            return m_dateFormat;
        }

        public DisplayFormat getNumberFormat() {
            return m_numberFormat;
        }
    }

    private interface DFBuilder {
        DisplayFormat create(String format);
        DisplayFormat fromFormatEx(CellFormatEx f);
    }

    private static class NumberBuilder implements DFBuilder {
        public DisplayFormat create(String format) {
            return new NumberFormat(format);
        }

        public DisplayFormat fromFormatEx(CellFormatEx f) {
            return f.getNumberFormat();
        }
    }

    private static class DateBuilder implements DFBuilder {
        public DisplayFormat create(String format) {
            return new DateFormat(format);
        }

        public DisplayFormat fromFormatEx(CellFormatEx f) {
            return f.getDateFormat();
        }
    }

    private static void cloneFormat(CellFormat f1, WritableCellFormat f2) throws WriteException {
        f2.setAlignment(f1.getAlignment());
        f2.setBackground(f1.getBackgroundColour(), f1.getPattern());
        cloneBorder(f1, f2, Border.LEFT);
        cloneBorder(f1, f2, Border.TOP);
        cloneBorder(f1, f2, Border.RIGHT);
        cloneBorder(f1, f2, Border.BOTTOM);
        f2.setFont((FontRecord)f1.getFont());
        f2.setIndentation(f1.getIndentation());
        f2.setLocked(f1.isLocked());
        f2.setOrientation(f1.getOrientation());
        f2.setShrinkToFit(f1.isShrinkToFit());
        f2.setVerticalAlignment(f1.getVerticalAlignment());
        f2.setWrap(f1.getWrap());
    }

    private static WritableCellFormat inheritFormat(CellFormat parent, DisplayFormat df) throws WriteException {
        WritableCellFormat res;
        if (df == null) {
            if (parent  == null) {
                res = null;
            } else {
                res = new WritableCellFormat(parent);
            }
        } else {
            res = new WritableCellFormat(df);
            if (parent != null) cloneFormat(parent, res);
        }
        return res;
    }

    private static WritableCellFormat createFormat(WritableCellFormat parent, Element fe) throws WriteException {
        WritableCellFormat res = inheritFormat(parent, null);
        if (fe != null) {
            if (res == null) res = new WritableCellFormat();
            formatFromXml(res, fe);
        }
        return res;
    }

    private static WritableCellFormat createCellFormat(CellFormatEx parent, Element e, DFBuilder b) throws WriteException {
        final Element fe = DomHelper.getChild(e, "format", false);
        DisplayFormat df = null;
        if (fe != null) {
            if (fe.hasAttribute("date-display-format")) {
                df = new DateFormat(fe.getAttribute("date-display-format"));
            } if (fe.hasAttribute("number-display-format")) {
                df = new NumberFormat(fe.getAttribute("number-display-format"));
            } else if (fe.hasAttribute("display-format")) {
                df = b.create(fe.getAttribute("display-format"));
            }
        }
        if (df == null && b != null) df = b.fromFormatEx(parent);
        WritableCellFormat res = inheritFormat(parent.getFormat(), df);
        if (fe != null) {
            if (res == null) res = new WritableCellFormat();
            formatFromXml(res, fe);
        }
        if (res == null) res = WritableWorkbookImpl.NORMAL_STYLE;
        return res;
    }

    private static CellFormatEx createFormatEx(CellFormatEx parent, Element e) throws WriteException {
        final Element fe = DomHelper.getChild(e, "format", false);
        DisplayFormat dateFormat;
        if (fe != null && fe.hasAttribute("date-display-format")) {
            dateFormat = new DateFormat(fe.getAttribute("date-display-format"));
        } else if (parent != null) {
            dateFormat = parent.getDateFormat();
        } else {
            dateFormat = null;
        }
        DisplayFormat numberFormat;
        if (fe != null && fe.hasAttribute("number-display-format")) {
            numberFormat = new NumberFormat(fe.getAttribute("number-display-format"));
        } else if (parent != null) {
            numberFormat = parent.getNumberFormat();
        } else {
            numberFormat = null;
        }
        WritableCellFormat f = createFormat(parent == null ? null : parent.getFormat(), fe);
        return new CellFormatEx(f, dateFormat, numberFormat);
    }

    private static NumberBuilder s_numberBuilder = new NumberBuilder();
    private static DateBuilder s_dateBuilder = new DateBuilder();

    public void fromXml(Element data, OutputStream out) throws WriteException, IOException, ParseException {
        java.text.DateFormat dateFormat = null;
        if (data.hasAttribute("date-format")) {
            dateFormat = new SimpleDateFormat(data.getAttribute("date-format"));
        }
        WorkbookSettings settings = initSettings(data);
        WritableWorkbook book;
        book = Workbook.createWorkbook(out, settings);

        Element[] sheets = DomHelper.getChildrenByTagName(data, "worksheet", false);
        for (int shi = 0; shi < sheets.length; shi++) {
            Element shc = sheets[shi];
            CellFormatEx shf = createFormatEx(null, shc);
            WritableSheet sh = book.createSheet(
                shc.hasAttribute("name") ? shc.getAttribute("name") : "sheet" + shi, shi
            );
            int rowi = 0;
            for (Iterator<Element> rit = new DomHelper.ElementIterator(shc, "row"); rit.hasNext();) {
                Element rowe = rit.next();
                CellFormatEx rowf = createFormatEx(shf, rowe);
                int coli = 0;
                for (Iterator<Element> cit = new DomHelper.ElementIterator(rowe, "col"); cit.hasNext();) {
                    Element colc = cit.next();
                    if (colc.hasAttribute("width")) {
                        sh.setColumnView(coli, Integer.parseInt(colc.getAttribute("width")));
                    }
                    String cellData = DomHelper.getNodeValue(DomHelper.getChild(colc, "data", true));
                    WritableCell cell = null;
                    if (colc.hasAttribute("type")) {
                        String type = colc.getAttribute("type");
                        if (type.equalsIgnoreCase("number")) {
                            cell = new Number(
                                coli, rowi, Double.parseDouble(cellData),
                                createCellFormat(rowf, colc, s_numberBuilder)
                            );
                        } else if (type.equalsIgnoreCase("datetime")) {
                            if (dateFormat != null) {
                                cell = new DateTime(
                                    coli, rowi, dateFormat.parse(cellData),
                                    createCellFormat(rowf, colc, s_dateBuilder)
                                );
                            }
                        } else if (type.equalsIgnoreCase("boolean")) {
                            cell = new Boolean(
                                coli, rowi, cellData.equalsIgnoreCase("true"), createCellFormat(rowf, colc, null)
                            );
                        } else if (type.equalsIgnoreCase("formula")) {
                            cell = new Formula(coli, rowi, cellData, createCellFormat(rowf, colc, null));
                        }
                    }
                    if (cell == null) {
                        cell = new Label(coli, rowi, cellData, createCellFormat(rowf, colc, null));
                    }

                    sh.addCell(cell);
                    coli++;
                }
                rowi++;
            }
        }
        book.write();
        book.close();
    }

    private WorkbookSettings initSettings(Element data) {
        WorkbookSettings settings = new WorkbookSettings();
        if (data.hasAttribute("locale-lang")) {
            String localeLang, localeCountry = null;
            localeLang = data.getAttribute("locale-lang");
            if (data.hasAttribute("locale-country")) {
                localeCountry = data.getAttribute("locale-country");
            }
            if (localeCountry == null) settings.setLocale(new Locale(localeLang));
            else settings.setLocale(new Locale(localeLang, localeCountry));
        }
        return settings;
    }

    private static Object fieldByName(Class cl, String name) {
        try {
            return cl.getField(name.toUpperCase()).get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(cl.getName() + " '" + name + "' not found");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Colour colorByName(String name) {
        return (Colour)fieldByName(Colour.class, name);
    }

    private static Alignment alignmentByName(String name) {
        return (Alignment)fieldByName(Alignment.class, name);
    }

    private static VerticalAlignment vAlignmentByName(String name) {
        return (VerticalAlignment)fieldByName(VerticalAlignment.class, name);
    }

    private static void initBackground(WritableCellFormat format, Element values) throws WriteException {
        Colour c = colorByName(DomHelper.getNodeAttr(values, "color", true));
        Pattern p;
        if (values.hasAttribute("pattern-name")) {
            p = (Pattern)fieldByName(Pattern.class, values.getAttribute("pattern-name"));
        } else if (values.hasAttribute("pattern-code")) {
            p = Pattern.getPattern(Integer.parseInt(values.getAttribute("pattern-code")));
        } else {
            p = null;
        }
        if (p == null) {
            format.setBackground(c);
        } else {
            format.setBackground(c, p);
        }
    }

    private static void initBorders(WritableCellFormat format, Element values) throws WriteException {
        Element[] ee = DomHelper.getChildrenByTagName(values, "border", false);
        for (Element be: ee) {
            Border b = (Border) fieldByName(
                    Border.class, DomHelper.getNodeAttr(be, "sides", true)
            );
            BorderLineStyle ls = (BorderLineStyle) fieldByName(
                    BorderLineStyle.class, DomHelper.getNodeAttr(be, "line-style", true)
            );
            Colour c = null;
            if (be.hasAttribute("color")) {
                c = colorByName(DomHelper.getNodeAttr(be, "color"));
            }
            if (c == null) {
                format.setBorder(b, ls);
            } else {
                format.setBorder(b, ls, c);
            }
        }
    }

    private static void formatFromXml(
        WritableCellFormat format, Element values
    ) throws WriteException {
        if (format == null) format = new WritableCellFormat();
        NamedNodeMap vv = values.getAttributes();
        for (int i = 0; i < vv.getLength(); i++) {
            Node vn = vv.item(i);
            String name = vn.getNodeName();
            String value = vn.getNodeValue();

            if (name.equalsIgnoreCase("wrap")) {
                format.setWrap(value.equalsIgnoreCase("yes"));
            } else if (name.equalsIgnoreCase("align")) {
                format.setAlignment(alignmentByName(value));
            } else if (name.equalsIgnoreCase("valign")) {
                format.setVerticalAlignment(vAlignmentByName(value));
            } else if (name.equalsIgnoreCase("orientation")) {
                format.setOrientation(
                    (Orientation)fieldByName(Orientation.class, value)
                );
            } else if (name.equalsIgnoreCase("indentation")) {
                format.setIndentation(Integer.parseInt(value));
            } else if (name.equalsIgnoreCase("locked")) {
                format.setLocked(value.equalsIgnoreCase("yes"));
            } else if (name.equalsIgnoreCase("shrink-to-fit")) {
                format.setShrinkToFit(value.equalsIgnoreCase("yes"));
            }
            //else {
            //    throw new RuntimeException("Unknown format attribute '" + name + "'");
            //}
        }

        Element e = DomHelper.getChild(values, "background", false);
        if (e != null) initBackground(format, e);

        initBorders(format, values);

        e = DomHelper.getChild(values, "font", false);
        if (e != null) initFont(format, e);
    }

    private static void initFont(
        WritableCellFormat format, Element values
    ) throws WriteException {
        final String trueAttrValue = "yes";
        WritableFont font = values.hasAttribute("name") ?
            new WritableFont(
                WritableFont.createFont(
                    DomHelper.getNodeAttr(values, "name")
                )
            ) :
            new WritableFont(WritableFont.ARIAL);
        NamedNodeMap vv = values.getAttributes();
        for (int i = 0; i < vv.getLength(); i++) {
            Node vn = vv.item(i);
            String name = vn.getNodeName();
            String value = vn.getNodeValue();

            if (name.equalsIgnoreCase("point-size")) {
                font.setPointSize(Integer.parseInt(value));
            } else if (name.equalsIgnoreCase("bold-weight")) {
                if (value.equalsIgnoreCase(trueAttrValue)) font.setBoldStyle(WritableFont.BOLD);
                else font.setBoldStyle(WritableFont.NO_BOLD);
            } else if (name.equalsIgnoreCase("italic")) {
                font.setItalic(value.equalsIgnoreCase(trueAttrValue));
            } else if (name.equalsIgnoreCase("underline")) {
                font.setUnderlineStyle(
                    (UnderlineStyle)fieldByName(UnderlineStyle.class, value)
                );
            } else if (name.equalsIgnoreCase("color")) {
                font.setColour(colorByName(value));
            } else if (name.equalsIgnoreCase("script")) {
                font.setScriptStyle(
                    (ScriptStyle)fieldByName(ScriptStyle.class, value)
                );
            } else {
                throw new RuntimeException("Unknown font attribute '" + name + "'");
            }
        }
        format.setFont(font);
    }
}
