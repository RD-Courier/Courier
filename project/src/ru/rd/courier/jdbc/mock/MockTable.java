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
package ru.rd.courier.jdbc.mock;

import org.w3c.dom.Element;
import ru.rd.courier.CourierException;
import ru.rd.courier.jdbc.GenericSqlException;
import ru.rd.courier.jdbc.databuffer.DataBuffer;
import ru.rd.courier.jdbc.databuffer.DataBufferResultSet;
import ru.rd.courier.utils.DomHelper;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MockTable extends DataBuffer {
    private Logger m_logger;
    private String m_name;
    private final long m_newRecordInterval;
    private Timer m_timer;
    private List<Element> m_data;

    public MockTable(Logger logger, String name, Element e) throws SQLException {
        if (logger == null) throw new IllegalArgumentException("Logger cannot be null");
        m_logger = logger;
        m_name = (name == null) ? DomHelper.getNodeAttr(e, "name") : name;
        int initialRecordsCount = -1;
        m_newRecordInterval = DomHelper.getLongNodeAttr(e, "new-record-interval", -1);
        initialRecordsCount = DomHelper.getIntNodeAttr(e, "initial-records-count", -1);
        init(DomHelper.getChildrenByTagName(e, "column"));
        Element data = DomHelper.getChild(e, "data", false);
        if (data != null) {
            Element[] rl = DomHelper.getChildrenByTagName(data, "record", false);
            if (initialRecordsCount < 0) initialRecordsCount = rl.length;
            initialRecordsCount = Math.min(initialRecordsCount, rl.length);
            int i;
            for (i = 0; i < initialRecordsCount; i++) {
                addRecord(rl[i]);
            }
            if (i < rl.length) {
                m_data = new LinkedList<Element>();
                for (; i < rl.length; i++) {
                    m_data.add(rl[i]);
                }
            }
        }

        if (m_newRecordInterval > 0) {
            m_timer = new Timer(m_name + "-MockTableTimer");
            m_timer.schedule(
                new TimerTask(){
                    public void run() {
                        try {
                            if ((m_data != null) && (m_data.size() > 0)) {
                                addRecordFromData();
                            }
                            if ((m_data == null) || (m_data.size() == 0)) {
                                m_data = null;
                                m_timer.cancel();
                            }
                        } catch (SQLException e1) {
                            m_logger.log(Level.SEVERE, e1.getMessage(), e1);
                        }
                    }
                },
                0, m_newRecordInterval
            );
        }
    }

    public MockTable(Logger logger, Element e) throws SQLException {
        this(logger, null, e);
    }

    public String getName() {
        return m_name;
    }

    public void addRecordFromData() throws SQLException {
        addRecord(m_data.remove(0));
    }

    public List<Element> getData() {
        return m_data;
    }

    public synchronized DataBufferResultSet selectAll() throws SQLException {
        DataBufferResultSet ret = new DataBufferResultSet();
        ret.importMetaInfo(this);
        beforeFirst();
        while(next()) {
            ret.importRecord(this);
        }
        ret.beforeFirst();
        return ret;
    }

    private abstract class DataExtractor {
        public final DataBufferResultSet getResultSet() throws SQLException {
            DataBufferResultSet ret = new DataBufferResultSet();
            ret.importMetaInfo(MockTable.this);
            beforeFirst();
            while(next()) {
                if (matches()) ret.importRecord(MockTable.this);
            }
            ret.beforeFirst();
            return ret;
        }

        protected abstract boolean matches() throws SQLException;
    }

    public synchronized DataBufferResultSet selectWhereFieldGreater(
        final int colIdx, final int value
    ) throws SQLException {
        DataExtractor de = new DataExtractor() {
            protected boolean matches() throws SQLException {
                return getInt(colIdx) > value;
            }
        };
        return de.getResultSet();
    }

    public synchronized DataBufferResultSet selectWhereFieldGreater(
        final int colIdx, final Date value
    ) throws SQLException {
        DataExtractor de = new DataExtractor() {
            protected boolean matches() throws SQLException {
                return getDate(colIdx).after(value);
            }
        };
        return de.getResultSet();
    }

    public synchronized DataBufferResultSet selectWhereFieldGreater(
        String colName, Date value
    ) throws SQLException {
        return selectWhereFieldGreater(findColumn(colName), value);
    }

    public synchronized DataBufferResultSet selectWhereFieldGreater(
        String colName, int value
    ) throws SQLException {
        return selectWhereFieldGreater(findColumn(colName), value);
    }

    public synchronized void close() throws SQLException {
        if (m_timer != null) {
            m_timer.cancel();
            m_timer = null;
        }
        super.close();
    }
}
