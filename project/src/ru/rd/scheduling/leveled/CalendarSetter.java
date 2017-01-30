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

import java.util.Calendar;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 22.03.2005
 * Time: 18:16:56
 */
public class CalendarSetter extends CalendarFieldsByImportance {
    public CalendarSetter() {
        super();
    }

    protected void handleField(Calendar c, int fieldId, Object data) {
        int interval = (Integer) data;
        if (fieldId == Calendar.DAY_OF_WEEK) {
            int shift = interval - c.get(fieldId);
            if (shift < 0) shift += 7;
            c.add(fieldId, shift);
        } else {
            c.set(fieldId, interval);
        }
    }

    public void addFieldValue(int fieldId, int value) {
        addFieldData(fieldId, value);
    }

    public void setCalendar(Calendar c) {
        handleByImportance(c);
    }

    public boolean isGreater(CalendarSetter setter) {
        for (Map.Entry<Integer, Object> me : getData().entrySet()) {
            Object value2 = setter.getData().get(me.getKey());
            if (value2 == null) return true;
            int value = (Integer) me.getValue();
            if (value > (Integer)value2) return true;
        }
        return false;
    }
}
