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
package ru.rd.courier.manager.ui;

import ru.rd.courier.manager.ManagedCourier;
import ru.rd.courier.manager.ManagedCourierListener;
import ru.rd.courier.manager.message.ProcessResult;
import ru.rd.net.ServerContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * User: AStepochkin
 * Date: 06.10.2008
 * Time: 16:00:18
 */
public class StatViewer extends JFrame implements ManagedCourierListener {
    private final ServerContext m_ctx;
    private final DefaultTableModel m_dtm;
    private int m_maxRows = 100;

    private interface ColumnValueExtractor<T> {
        Object extract(T obj) throws Exception;
    }

    private static class ReflectColumnExtractor<T> implements ColumnValueExtractor<T> {
        private final Method m_method;

        public ReflectColumnExtractor(Class cls, String method) throws NoSuchMethodException {
            m_method = cls.getMethod(method);
        }

        public Object extract(T obj) throws Exception {
            return m_method.invoke(obj);
        }
    }

    private static final Map<String, ColumnValueExtractor<ProcessResult>> cExtractors = new HashMap<String, ColumnValueExtractor<ProcessResult>>();

    private static void addExtractor(String name, ColumnValueExtractor<ProcessResult> e) {
        cExtractors.put(name.toUpperCase(), e);
    }

    private static void addFieldExtractor(String name, Class cls, String field) {
        //addExtractor(name, e);
    }

    static {
        addExtractor("ID", new ColumnValueExtractor<ProcessResult>() {
            public Object extract(ProcessResult obj) {
                return obj.getId();
            }
        });
        /*
        addExtractor("", new ColumnValueExtractor<ProcessResult>() {
            public Object extract(ProcessResult obj) {
                return obj.get();
            }
        });
        */
    }

    public StatViewer(ServerContext ctx, ManagedCourier courier) {
        super(courier.getHost() + ":" + courier.getConfig());
        m_ctx = ctx;
        courier.addListener(this);
        m_dtm = new DefaultTableModel(){
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        m_dtm.setColumnIdentifiers(new Object[] {
            "Id", "Pipe",
            "Source", "SourceType", "SourceUrl",
            "Target", "TargetType", "TargetUrl",
            "StartTime", "TotalTime", "STime", "TTime",
            "Records", "Errors", "Error"
        });
        JTable t = new JTable(m_dtm);
        getContentPane().add(new JScrollPane(t));
    }

    private void debug(String m) {
        m_ctx.getLogger("").debug(m);
    }

    public void setMaxRows(int value) {
        m_maxRows = value;
        ensureMaxRows();
    }

    private void ensureMaxRows() {
        if (m_dtm.getRowCount() > m_maxRows) {
            m_dtm.setRowCount(m_maxRows);
        }
    }

    public void addResult(ProcessResult pr) {
        m_dtm.insertRow(0, new Object[] {
            pr.getId(), pr.getPipe(),
            pr.getSourceDbName(), pr.getSourceDbType(), pr.getSourceDbUrl(),
            pr.getTargetDbName(), pr.getTargetDbType(), pr.getTargetDbUrl(),
            pr.getStartTime(), pr.getTotalTime(),
            pr.getSourceTime(), pr.getTargetTime(),
            pr.getRecordCount(), pr.getErrorCount(),
            pr.getError(), pr.getErrorStack()
        });
        ensureMaxRows();
    }

    public void processResult(ProcessResult result){
        addResult(result);
    }

    public void courierClosing() {
        dispose();
    }

    public Vector getData() {
        return m_dtm.getDataVector();
    }
}
