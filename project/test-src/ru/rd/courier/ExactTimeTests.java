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
package ru.rd.courier;

import ru.rd.courier.jdbc.mock.MockDatabase;
import ru.rd.courier.jdbc.mock.MockTable;

import java.util.*;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 26.08.2005
 * Time: 15:55:40
 */
public class ExactTimeTests extends CourierTestCase {
    private static final int c_CourierStartDelaySeconds = 15;

    private Calendar getExactDate(int delaySeconds) {
        Calendar c = new GregorianCalendar();
        c.add(Calendar.SECOND, c_CourierStartDelaySeconds + delaySeconds);
        return c;
    }

    private String getScheduleDate(int delaySeconds) {
        Calendar dt = getExactDate(delaySeconds);
        return (
            "hour=\"" + dt.get(Calendar.HOUR_OF_DAY) + "\""
            + " minute=\"" + dt.get(Calendar.MINUTE) + "\""
            + " second=\"" + dt.get(Calendar.SECOND) + "\""
        );
    }

    private String getExactSchedule(int delaySeconds) {
        return
            "<time-table period=\"day\">" +
            "<start " + getScheduleDate(delaySeconds) + "/><launch-once/>" +
            "</time-table>";
    }

    public void test() throws CourierException {
        PipeListener pl;

        pl = new EmptyPipeListener() {
            public InitCustomizer getInitCustomizer() {
                return new NullInitCustomizer(){
                    public String getPipeCustomTags() {
                        return (
                            ""
                            //"<clear-vars>" + getExactSchedule(5) + "</clear-vars>"
                        );
                    }
                };
            }

            public void onCourierStart() {
                try {
                    MockTable tbl = MockDatabase.getGlobalDatabase().getTable("ScheduledClearVars");
                    tbl.addRecord();
                    Date dt = new Date();
                    tbl.updateDate("date", new java.sql.Date(dt.getTime()));
                    tbl.updateString("col1", "aaaa");
                } catch (SQLException e) {
                    addError(e.getMessage());
                    stopTest();
                }
            }

            public void processStarted(Pipe testPipe, Pipeline pipe, TransferProcess tp) {
                switch (testPipe.getLaunchNumber()) {
                    case 1: {
                        getCourier().getSystemDb().clearPipeVars(pipe.getName());
                    }
                }
            }
        };
        addListener("scheduled-clear-vars", pl);

        launchTest("test-exact-time.xml");
    }
}
