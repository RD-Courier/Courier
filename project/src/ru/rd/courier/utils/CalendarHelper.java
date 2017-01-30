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
package ru.rd.courier.utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 18.03.2005
 * Time: 20:10:21
 */
public class CalendarHelper {
    private static final Map<String, Integer> s_weekDaysNames =
        new HashMap<String, Integer>(7);

    static {
        s_weekDaysNames.put("SUNDAY", Calendar.SUNDAY);
        s_weekDaysNames.put("MONDAY", Calendar.MONDAY);
        s_weekDaysNames.put("TUESDAY", Calendar.TUESDAY);
        s_weekDaysNames.put("WEDNESDAY", Calendar.WEDNESDAY);
        s_weekDaysNames.put("THURSDAY", Calendar.THURSDAY);
        s_weekDaysNames.put("FRIDAY", Calendar.FRIDAY);
        s_weekDaysNames.put("SATURDAY", Calendar.SATURDAY);
    }

    public static boolean isWeekName(String name) {
        return s_weekDaysNames.containsKey(name.toUpperCase());
    }

    public static int getWeekByName(String name) {
        String uname = name.toUpperCase();
        if (s_weekDaysNames.containsKey(uname)) {
            return s_weekDaysNames.get(uname).intValue();
        } else {
            throw new RuntimeException("Invalid week day name '" + name + "'");
        }
    }

}
