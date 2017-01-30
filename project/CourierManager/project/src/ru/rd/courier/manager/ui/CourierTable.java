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
import static ru.rd.courier.manager.ui.TaggedTableModel.*;
import ru.rd.net.ServerContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.TimerTask;

/**
 * User: AStepochkin
 * Date: 06.10.2008
 * Time: 18:16:55
 */
public class CourierTable extends JTable {
    private final ServerContext m_ctx;
    private final TaggedTableModel<CourierRow> m_dtm;
    private int m_maxRows = 100;
    private long m_hiddenAlivePeriod;

    public class CourierRow {
        public final ManagedCourier courier;
        private StatViewer m_statViewer;
        private TimerTask m_closeTask;
        private long m_hiddenAlivePeriod;


        public CourierRow(ManagedCourier courier) {
            this.courier = courier;
            m_hiddenAlivePeriod = 3000;
        }

        public void setHiddenAlivePeriod(long value) {
            m_hiddenAlivePeriod = value;
        }

        public StatViewer getViewer() {
            return m_statViewer;
        }

        public void showStatViewer() {
            if (m_statViewer == null) {
                m_statViewer = new StatViewer(m_ctx, courier);
                Point lp = CourierTable.this.getLocationOnScreen();
                m_statViewer.setMaxRows(getMaxRows());
                m_statViewer.setLocation((int)lp.getX() + CourierTable.this.getWidth(), (int)lp.getY());
                m_statViewer.setSize(600, 300);
                m_statViewer.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                m_statViewer.addWindowListener(new WindowAdapter() {
                    public void windowClosed(WindowEvent e) {
                        removeViewer();
                    }

                    public void windowActivated(WindowEvent e) {
                        if (m_closeTask != null) {
                            m_closeTask.cancel();
                            m_closeTask = null;
                        }
                    }

                    public void windowDeactivated(WindowEvent e) {
                        setCloseStatTask();
                    }
                });
            }
            m_statViewer.setVisible(true);
        }

        private void setCloseStatTask() {
            if (m_closeTask == null && m_statViewer != null && !m_statViewer.isVisible()) {
                m_closeTask = new TimerTask() {
                    public void run() {
                        if (m_statViewer == null) return;
                        if(!m_statViewer.isVisible()) removeViewer();
                    }
                };
                m_ctx.getTimer().schedule(m_closeTask, m_hiddenAlivePeriod);
            }
        }

        public void removeViewer() {
            JFrame v = m_statViewer;
            m_statViewer = null;
            if (v != null) v.dispose();
        }

        public void dispose() {
            removeViewer();
            m_dtm.removeRow(this);
        }

        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj instanceof ManagedCourier) {
                return courier.id == ((ManagedCourier)obj).id;
            } else if (obj instanceof CourierRow) {
                return courier.id == ((CourierRow)obj).courier.id;
            } else {
                return false;
            }
        }
    }


    public CourierTable(ServerContext ctx) {
        m_ctx = ctx;
        m_dtm = new TaggedTableModel<CourierRow>();
        m_dtm.setColumns(new String[] {"id", "host", "config"});

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    CourierRow courier = findRow(e.getPoint());
                    if (courier == null) return;
                    courier.showStatViewer();
                }
            }
        });

        setModel(m_dtm);
    }

    public TaggedTableModel<CourierRow> getModel() {
        return m_dtm;
    }

    public int getMaxRows() {
        return m_maxRows;
    }

    public long getHiddenAlivePeriod() {
        return m_hiddenAlivePeriod;
    }

    public void setHiddenAlivePeriod(long value) {
        m_hiddenAlivePeriod = value;
    }

    public void setMaxRows(int value) {
        m_maxRows = value;
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private void debug(String m) {
        m_ctx.getLogger(null).debug(m);
    }

    public void addCourier(ManagedCourier courier) {
        CourierRow cr = new CourierRow(courier);
        cr.setHiddenAlivePeriod(getHiddenAlivePeriod());
        m_dtm.addRow(cr, new Object[] {
            Integer.toString(courier.id),
            courier.getHost(),
            courier.info.getCode()
        });
    }

    public void removeCourier(ManagedCourier courier) {
        CourierRow row = findRow(courier);
        if (row != null) row.dispose();
    }

    private CourierRow findRow(ManagedCourier courier) {
        return m_dtm.findTag(courier);
    }

    public CourierRow findRow(Point p) {
        int ri = rowAtPoint(p);
        if (ri < 0) return null;
        TableRow<CourierRow> trow = getModel().getRow(ri);
        return trow.getTag();
    }

    public ManagedCourier findCourier(Point p) {
        CourierRow trow = findRow(p);
        if (trow == null) return null;
        return trow.courier;
    }

    public ManagedCourier getCurrentCourier() {
        int ri = getSelectedRow();
        if (ri < 0) return null;
        TableRow<CourierRow> trow = getModel().getRow(ri);
        return trow.getTag().courier;
    }

    public String toDebugString() {
        int statCount = 0;
        synchronized(m_dtm) {
            for (TableRow<CourierRow> r: m_dtm.getData()) {
                StatViewer v = r.getTag().getViewer();
                if (v != null) {
                    statCount += v.getData().size();
                }
            }
            return "Couriers=" + m_dtm.getData().size() + " Stats=" + statCount;
        }
    }
}
