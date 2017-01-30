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
package ru.rd.courier.manager;

import ru.rd.courier.manager.ui.AdminConsole;
import ru.rd.courier.utils.DomHelper;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import org.w3c.dom.Element;

/**
 * User: AStepochkin
 * Date: 26.09.2008
 * Time: 17:09:10
 */
public class ServerTest {
    public static void main(String[] args) throws Exception {
        File confFile = new File(args[0]);
        File spath = confFile.getParentFile();
        Element conf = DomHelper.parseXmlFile(confFile).getDocumentElement();
        final Manager m = new Manager(spath, conf);
        final AdminConsole console = new AdminConsole(m, "CourierManager", m);
        console.setSize(300, 300);
        console.setVisible(true);
        console.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        console.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                m.stop();
            }
        });

        m.addListener(new ManagerListener() {
            public void courierAdded(ManagedCourier courier) {
                console.addCourier(courier);
            }

            public void courierRemoved(ManagedCourier courier) {
                console.removeCourier(courier);
            }
        });
        m.start();
    }
}
