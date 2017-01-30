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
package ru.rd.courier.datalinks;

import org.w3c.dom.Node;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.csv.*;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * User: AStepochkin
 * Date: 03.02.2006
 * Time: 14:36:17
 */
public class FixedColumnsFileSourceFactory extends ReceiverFactory {
    private final String m_dir;
    private final FileSource.FileSelector m_fileSelector;
    private final boolean m_holdFileUntilNew;
    private final int m_fullReadEvery;
    private final long m_fullReadInterval;
    private final int m_headerRow;
    private final boolean m_addAbsentAsNull;
    private final LineFilter m_filter;
    private final String m_titleVarName;
    private final String m_charSet;
    private final List<FixedColumnsFileSource.Field> m_fields;

    private static final String c_dir = "DirName";
    private static final String c_filePrefix = "file-prefix";
    private static final String c_filePostfix = "file-postfix";
    private static final String c_dateFormat = "file-date-format";
    private static final String c_holdFileUntilNew = "hold-until-new";
    private static final String c_fullReadEvery = "full-read-every";
    private static final String c_fullReadInterval = "full-read-interval";
    //private static final String c_needToTrimParam = "NeedToTrim";
    //private static final String c_nullWordParam = "NullWord";
    private static final String c_charSetName = "charset";
    private static final String c_headerRow = "header-row";
    private static final String c_titleVarName = "title-var-name";
    private static final String c_addAbsentAsNull = "absent-as-null";
    private static final String c_skipTemplate = "skip-template";

    public FixedColumnsFileSourceFactory(
        CourierLogger logger,
        String dir, FileSource.FileSelector fileSelector,
        boolean holdFileUntilNew, int fullReadEvery, long fullReadInterval,
        int headerRow, boolean addAbsentAsNull,
        LineFilter filter, String titleVarName, String charSet,
        List<FixedColumnsFileSource.Field> fields
    ) {
        super(logger, null);
        m_dir = dir;
        m_fileSelector = fileSelector;
        m_holdFileUntilNew = holdFileUntilNew;
        m_fullReadEvery = fullReadEvery;
        m_fullReadInterval = fullReadInterval;
        m_headerRow = headerRow;
        m_addAbsentAsNull = addAbsentAsNull;
        m_filter = filter;
        m_titleVarName = titleVarName;
        m_charSet = charSet;
        m_fields = fields;
    }

    public FixedColumnsFileSourceFactory(
        CourierLogger logger, ObjectPoolIntf threadPool, Node conf
    ) {
        super(logger, null);
        m_dir = DomHelper.getNodeAttr(conf, c_dir, true);
        String fileName = DomHelper.getNodeAttr(conf, "file-name", false);
        if (fileName != null) {
            m_fileSelector = new FileSource.ConstFileSelector(fileName);
        } else {
            m_fileSelector = new FileSource.StdFileSelector(
                  DomHelper.getNodeAttr(conf, c_filePrefix, true)
                , DomHelper.getNodeAttr(conf, c_filePostfix, true)
                , new SimpleDateFormat(DomHelper.getNodeAttr(conf, c_dateFormat, true))
            );
        }
        m_holdFileUntilNew = DomHelper.getBoolYesNo(conf, c_holdFileUntilNew, true);
        m_fullReadEvery = DomHelper.getIntNodeAttr(conf, c_fullReadEvery, 0);
        m_fullReadInterval = DomHelper.getTimeNodeAttr(conf, c_fullReadInterval, 0);
        m_headerRow = DomHelper.getIntNodeAttr(conf, c_headerRow, 0);
        m_addAbsentAsNull = DomHelper.getBoolYesNo(conf, c_addAbsentAsNull, false);
        m_filter = filterFromXml(conf);
        m_titleVarName = DomHelper.getNodeAttr(conf, c_titleVarName, false);
        m_charSet = DomHelper.getNodeAttr(conf, c_charSetName, false);
        m_fields = fieldsFromXml(conf);
    }

    public static LineFilter filterFromXml(Node conf) {
        String filter = DomHelper.getNodeAttr(conf, c_skipTemplate, null);
        if (filter == null) {
            return new FakeFilter();
        } else {
            return new TemplateFilter(filter);
        }
    }

    public static List<FixedColumnsFileSource.Field> fieldsFromXml(Node conf) {
        List<FixedColumnsFileSource.Field> res = new LinkedList<FixedColumnsFileSource.Field>();
        for (Node n: DomHelper.getChildrenByTagName(conf, "field")) {
            res.add(new FixedColumnsFileSource.Field(
                DomHelper.getNodeAttr(n, "name", true),
                DomHelper.getIntNodeAttr(n, "begin", true),
                DomHelper.getIntNodeAttr(n, "end", true)
            ));
        }

        return res;
    }

    public Object getObject(ObjectPoolIntf pool) {
        try {
            return new FixedColumnsFileSource(
                m_logger, m_dir, m_fileSelector, m_holdFileUntilNew,
                m_fullReadEvery, m_fullReadInterval,
                m_headerRow, m_titleVarName,
                m_addAbsentAsNull, m_filter, m_charSet, m_fields
            );
        } catch (CourierException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkObject(Object o) {
        try {
            ((DataSource)o).request("check");
        } catch (CourierException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
