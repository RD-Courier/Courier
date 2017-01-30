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
package ru.rd.scheduling;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;

/**
 * User: AStepochkin
 * Date: 12.03.2009
 * Time: 15:21:42
 */
public class DayActiveState implements ActiveState {
    private int startHour;
    private int stopHour;
    private int startMinute;
    private int stopMinute;
    private int startSecond;
    private int stopSecond;

    public DayActiveState() {
        setStartHour(0);
        setStopHour(0);
        setStartMinute(0);
        setStopMinute(0);
        setStartSecond(0);
        setStopSecond(0);
    }

    public void setStartHour(int value) {
        startHour = value;
    }

    public void setStopHour(int value) {
        stopHour = value;
    }

    public void setStartMinute(int value) {
        startMinute = value;
    }

    public void setStopMinute(int value) {
        stopMinute = value;
    }

    public void setStartSecond(int value) {
        startSecond = value;
    }

    public void setStopSecond(int value) {
        stopSecond = value;
    }

    private static boolean isBetween(int value, int start, int stop) {
        return value >= start && value < stop;
    }

    public boolean isActive(Date date) {
        Calendar c = new GregorianCalendar(0, 0, 0);
        c.setTime(date);
        return
            isBetween(c.get(Calendar.HOUR_OF_DAY), startHour, stopHour) &&
            isBetween(c.get(Calendar.MINUTE), startMinute, stopMinute) &&
            isBetween(c.get(Calendar.SECOND), startSecond, stopSecond);
    }
}
