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

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: AStepochkin
 * Date: 18.07.2005
 * Time: 10:44:17
 */
public class TwoLaunchesTest extends CourierTestCase {
    private class FreshPipeListener extends EmptyPipeListener {
        private final String m_tableName;

        public FreshPipeListener(String tableName) {
            m_tableName = tableName;
        }

        public void onLaunchPortion(Pipe pipe) {
            addRecord();
        }

        public void onCourierStart() {
            int addRecCount = getLaunchNumber() * 2;
            //for (int i = 0; i < addRecCount; i++) {
            //    addRecord();
            //}
        }

        private void addRecord() {
            try {
                getMockDb().getTable(m_tableName).addRecordFromData();
            } catch (SQLException e) {
                addError(e.getMessage());
            }
        }
    }

    public void testTwoLaunches()
    throws CourierException, InterruptedException, IOException, SQLException {
        PipeListener pl = new FreshPipeListener("TestFresh");
        addListener("fresh-source-mode", pl);
        pl = new FreshPipeListener("ClearVarsOnStart");
        addListener("clear-vars-on-start", pl);
        launchTest("test-two-launches.xml");
    }
}
