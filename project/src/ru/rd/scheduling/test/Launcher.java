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
package ru.rd.scheduling.test;

import ru.rd.scheduling.Scheduler;

public class Launcher {

    public static void main(final String[] args) {
        final Launcher test = new Launcher();
        test.startTest();
    }

    private void startTest() {
        final Scheduler sch = new Scheduler();
        Object gid = sch.addTaskGroup("group1");
        TestWork w;

        w = new TestWork("task-1.1", 0, 100, 10, this);
        sch.addWork(gid, w);

        w = new TestWork("task-1.2", 150, 200, 8, this);
        sch.addWork(gid, w);

        gid = sch.addTaskGroup("group2");
        sch.addWork(gid, new TestWork("task-2.1", 150, 300, 5, null));
        sch.addWork(gid, new TestWork("task-2.2", 300, 500, 5, null));

        //System.out.println();
        //System.out.println(sch);
        //System.out.println();

        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sch.removeTaskGroup(gid);
        System.out.println("**** group2 removed");
        try {
            synchronized(this) {
                this.wait();
            }

            gid = sch.addTaskGroup("group3");

            w = new TestWork("task-3.1", 0, 300, 5, null);
            sch.addWork(gid, w);

            w = new TestWork("task-3.2", 150, 500, 5, null);
            sch.addWork(gid, w);

        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
    }
}
