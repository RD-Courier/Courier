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

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * User: AStepochkin
 * Date: 06.10.2008
 * Time: 18:20:22
 */
public class TaggedTableModel<T> extends AbstractTableModel {
    private final List<String> m_columns = new ArrayList<String>();
    private final List<TableRow<T>> m_data = new ArrayList<TableRow<T>>();

    public static class TableRow<T> {
        private List<Object> m_data;
        private T m_tag;

        public TableRow(List<Object> data, T tag) {
            if (data == null) throw new NullPointerException("data");
            if (tag == null) throw new NullPointerException("tag");
            m_data = data;
            m_tag = tag;
        }

        public List<Object> getData() {
            return m_data;
        }

        public void setData(List<Object> data) {
            m_data = data;
        }

        public T getTag() {
            return m_tag;
        }

        public Object getValue(int col) {
            if (col >= m_data.size()) return null;
            return m_data.get(col);
        }
    }

    public TaggedTableModel() {}

    public void setColumns(String[] columns) {
        m_columns.clear();
        for (String column: columns) {
            m_columns.add(column);
        }
    }

    public void addColumn(String column) {
        m_columns.add(column);
    }

    public String getColumnName(int column) {
        return m_columns.get(column);
    }

    public int getColumnCount() {
        return m_columns.size();
    }

    public synchronized int getRowCount() {
        return m_data.size();
    }

    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        return m_data.get(rowIndex).getValue(columnIndex);
    }

    public void insertRow(int rowIndex, T object, List<Object> data) {
        synchronized(this) {
            m_data.add(rowIndex, new TableRow<T>(data, object));
        }
        fireTableRowsInserted(rowIndex, rowIndex);
    }

    public synchronized void addRow(T object, List<Object> data) {
        int rowIndex = getRowCount();
        insertRow(rowIndex, object, data);
    }

    private static List<Object> arrayToList(Object[] data) {
        ArrayList<Object> ret = new ArrayList<Object>(data.length);
        Collections.addAll(ret, data);
        return ret;
    }

    public void addRow(T object, Object[] data) {
        addRow(object, arrayToList(data));
    }

    public void removeRow(int rowIndex) {
        synchronized(this) {
            m_data.remove(rowIndex);
        }
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public synchronized void removeRow(Object tag) {
        int rowIndex = findRow(tag);
        if (rowIndex >= 0) removeRow(rowIndex);
    }

    public synchronized TableRow<T> getRow(int rowIndex) {
        return m_data.get(rowIndex);
    }

    public List<TableRow<T>> getData() {
        return m_data;
    }

    public synchronized int findRow(Object tag) {
        if (tag == null) throw new NullPointerException("tag");
        int rowIndex = 0;
        for (TableRow<T> row: m_data) {
            if (row.getTag().equals(tag)) return rowIndex;
            rowIndex++;
        }
        return -1;
    }

    public synchronized T findTag(Object tag) {
        int ri = findRow(tag);
        if (ri < 0) return null;
        return getRow(ri).getTag();
    }
}
