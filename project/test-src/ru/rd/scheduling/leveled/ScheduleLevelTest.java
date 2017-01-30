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
package ru.rd.scheduling.leveled;

import junit.framework.TestCase;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.Timer;
import java.util.Date;

import ru.rd.scheduling.leveled.TimeTables.DayTimeTable;
import ru.rd.courier.logging.ConsoleHandlerEx;

/**
 * User: AStepochkin
 * Date: 19.03.2009
 * Time: 11:33:42
 */
public class ScheduleLevelTest extends TestCase {
    public void testNullParentStart() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.ALL);
        Handler h = new ConsoleHandlerEx(System.out);
        h.setLevel(Level.ALL);
        logger.addHandler(h);
        Timer timer = new Timer();
        ScheduleLevel level = new ScheduleLevel("", logger, new TimeTable() {
            public Date getAmbientStart(Date dt) {
                return null;
            }

            public Date getNextStart(Date start) {
                return null;
            }

            public Date getNextStop(Date start) {
                return null;
            }
        }, false, timer);
        level.start(null);
        level.stop();
        timer.cancel();
    }
}
