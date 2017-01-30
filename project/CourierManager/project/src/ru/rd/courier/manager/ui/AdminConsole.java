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

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.manager.ManagedCourier;
import ru.rd.courier.manager.Manager;
import ru.rd.courier.utils.ErrorHelper;
import ru.rd.courier.utils.FileHelper;
import ru.rd.net.ServerContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

/**
 * User: STEPOCHKIN
 * Date: 29.09.2008
 * Time: 22:45:21
 */
public class AdminConsole extends JFrame {
    private final CourierLogger m_logger;
    private final CourierTable m_table;
    private final Manager m_manager;

    public AdminConsole(ServerContext ctx, String title, Manager manager) {
        super(title);
        m_manager = manager;
        m_logger = ctx.getLogger("");
        m_table = new CourierTable(ctx);
        m_table.setMaxRows(100);
        getContentPane().add(new JScrollPane(m_table));
        JPanel buttons = new JPanel();

        JButton btnAdd = new JButton("Threads");
        btnAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    showThreads(null);
                } catch (IOException e1) {
                    m_logger.warning(e1);
                }
            }
        });
        buttons.add(btnAdd);

        JButton btnDiscon = new JButton("Disconnect");
        btnDiscon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    disconnectCourier();
                } catch (Exception e1) {
                    m_logger.warning(e1);
                }
            }
        });
        buttons.add(btnDiscon);

        JButton btnWriteCouriersFile = new JButton("WriteCouriers");
        btnWriteCouriersFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    writeCouriers();
                } catch (Exception e1) {
                    m_logger.warning(e1);
                }
            }
        });
        buttons.add(btnWriteCouriersFile);

        JButton btnFreeMem = new JButton("FreeMem");
        btnFreeMem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Runtime.getRuntime().gc();
                } catch (Exception e1) {
                    m_logger.warning(e1);
                }
            }
        });
        buttons.add(btnFreeMem);

        getContentPane().add(buttons, "South");
    }

    public void setMaxRows(int value) {
        m_table.setMaxRows(value);
    }

    public void setHiddenAlivePeriod(long value) {
        m_table.setHiddenAlivePeriod(value);
    }

    @SuppressWarnings({"UNUSED_SYMBOL"})
    private void debug(String message) {
        m_logger.debug(message);
    }

    public void addCourier(final ManagedCourier courier) {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    m_table.addCourier(courier);
                }
            });
        } catch (Exception e) {
            m_logger.warning(e);
        }
    }

    public void removeCourier(final ManagedCourier courier) {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    m_table.removeCourier(courier);
                }
            });
        } catch (Exception e) {
            m_logger.warning(e);
        }
    }

    public static void main(String[] args) {
        AdminConsole mf = new AdminConsole(null, "", null);
        mf.setSize(300, 300);
        mf.setVisible(true);
    }

    private void showThreads(String fileName) throws IOException {
        String st = ErrorHelper.stackTracesToString(Thread.currentThread().getThreadGroup());
        m_logger.debug(st);
        if (fileName != null) {
            FileHelper.stringToFile(st, new File(fileName));
        }
    }

    private void disconnectCourier() {
        ManagedCourier courier = m_table.getCurrentCourier();
        if (courier != null) {
            courier.dispose();
        }
    }

    private void writeCouriers() throws IOException {
        if (m_manager == null) return;
        Writer out = new BufferedWriter(new FileWriter("Couriers.txt"));
        try {
            m_manager.writeCouriers(out);
        } finally {
            out.close();
        }
    }

    public String toDebugString() {
        return m_table.toDebugString();
    }
}
