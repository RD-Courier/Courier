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

import ru.rd.courier.scripting.test.TestContext;
import ru.rd.courier.logging.ConsoleCourierLogger;
import ru.rd.courier.jdbc.mock.MockTable;
import ru.rd.courier.jdbc.mock.MockDatabase;
import ru.rd.courier.jdbc.CorrectUpdateResult;

import java.util.Date;
import java.util.List;
import java.sql.SQLException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.IOException;

/**
 * User: AStepochkin
 * Date: 18.07.2005
 * Time: 11:26:54
 */
public class SimpleTest extends CourierTestCase {
    public void test() throws CourierException {
        PipeListener pl;

/*
        pl = new EmptyPipeListener() {
            private final int[] m_recordsPerLaunch = new int[] {5, 1};
            public void processStarted(Pipe testPipe, Pipeline pipe, TransferProcess tp) throws SQLException {
                addRecordsFromData(
                    "FreshSourceMode",
                    m_recordsPerLaunch[testPipe.getLaunchNumber()]
                );
            }
        };
        addListener("fresh-source-mode", pl);
*/


        pl = new EmptyPipeListener() {
            private static final long c_clearVarsPeriod = 2000;
            public void onLaunchPortion(Pipe pipe) {
                switch (pipe.getLaunchNumber()) {
                    case 2: {
                        try {
                            long startDiff = System.currentTimeMillis() - m_lastStartTime;
                            if (startDiff < c_clearVarsPeriod) {
                                Thread.sleep(c_clearVarsPeriod - startDiff);
                            }
                        } catch (InterruptedException e) {
                            pipe.addPipeError(e.getMessage());
                        }
                        break;
                    }
                }
            }

            private void checkIntervalValue(Pipe testPipe, TransferProcess tp, String value) {
                try {
                    String iv = getIntervalValue(tp);
                    if (!value.equals(iv)) {
                        testPipe.addPipeError(
                            "launch = " + testPipe.getLaunchNumber() +
                            ": Wrong interval value: expected " +
                            value + " actual = " + iv
                        );
                    }
                } catch (CourierException e) {
                    testPipe.addPipeError(e.getMessage());
                }
            }

            private long m_lastStartTime;

            public void processStarted(Pipe testPipe, Pipeline pipe, TransferProcess tp) {
                long startDiff = System.currentTimeMillis() - m_lastStartTime;
                String v = null;
                String initialIntegerValue;
                try {
                    initialIntegerValue = pipe.getIntegerInitialIntervalValue().calculate(
                        new TestContext(new ConsoleCourierLogger(""), pipe.getDateFormat())
                    );
                } catch (CourierException e) {
                    testPipe.addPipeError(e.getMessage());
                    return;
                }

                v = (testPipe.getLaunchNumber() == 0 || startDiff > c_clearVarsPeriod) ?
                    initialIntegerValue : m_intervalValue;

                System.out.println(
                    testPipe.getLaunchNumber() +
                    " StartDiff = " + startDiff + " Expected interval = " + v
                );

                switch (testPipe.getLaunchNumber()) {
                    case 0: {
                        checkIntervalValue(testPipe, tp, initialIntegerValue);
                        break;
                    }
                    case 1: {
                        checkIntervalValue(testPipe, tp, v);
                        break;
                    }
                    case 2: {
                        checkIntervalValue(testPipe, tp, initialIntegerValue);
                        break;
                    }
                }

                m_lastStartTime = System.currentTimeMillis();
            }

            private String m_intervalValue;
            public void processFinished(Pipe testPipe, Pipeline pipe, TransferProcess tp) {
                try {
                    m_intervalValue = getIntervalValue(tp);
                } catch (CourierException e) {
                    testPipe.addPipeError(e.getMessage());
                }
            }

            public void check(Pipe testPipe) {
            }
        };
        addListener("clear-vars-date-format", pl);

        pl = new EmptyPipeListener() {
            public void onCourierStart() {
                try {
                    MockTable tbl = getTable("ScheduledClearVars");
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

            public void check(Pipe testPipe) {
                List<CourierTestCase.Process> procs = ((ConsecutivePipe)testPipe).getProcesses();

                int[] expSizes = new int[] {1, 0, 1};
                int i = 0;
                for (CourierTestCase.Process p: procs) {
                    List<String> data = p.getData();
                    assertEquals(expSizes[i], data.size());
                    i++;
                }
            }            
        };
        addListener("scheduled-clear-vars", pl);


        launchTest("test-one-launch.xml");
    }
}
