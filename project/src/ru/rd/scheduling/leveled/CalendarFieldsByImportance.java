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

import java.util.*;

public abstract class CalendarFieldsByImportance {
    // Importance enumeration
    private static final int c_millisecond = 1;
    private static final int c_second = c_millisecond + 1;
    private static final int c_minute = c_second + 1;
    private static final int c_hour = c_minute + 1;
    private static final int c_ampm = c_hour + 1;
    private static final int c_day = c_ampm + 1;
    private static final int c_week = c_day + 1;
    private static final int c_month = c_week + 1;
    private static final int c_year = c_month + 1;
    private static final int c_era = c_year + 1;
    private static final int c_zone = c_era + 1;
    private static final int c_dst = c_zone + 1;

    private static final int[] s_fildsImportance = new int[Calendar.FIELD_COUNT];

    static {
        s_fildsImportance[Calendar.MILLISECOND] = c_millisecond;
        s_fildsImportance[Calendar.SECOND] = c_second;
        s_fildsImportance[Calendar.MINUTE] = c_minute;
        s_fildsImportance[Calendar.HOUR_OF_DAY] = c_hour;
        s_fildsImportance[Calendar.HOUR] = c_hour;
        s_fildsImportance[Calendar.AM_PM] = c_ampm;
        s_fildsImportance[Calendar.DAY_OF_MONTH] = c_day;
        s_fildsImportance[Calendar.DAY_OF_WEEK] = c_day;
        s_fildsImportance[Calendar.DAY_OF_WEEK_IN_MONTH] = c_day;
        s_fildsImportance[Calendar.DAY_OF_YEAR] = c_day;
        s_fildsImportance[Calendar.DATE] = c_day;
        s_fildsImportance[Calendar.WEEK_OF_MONTH] = c_week;
        s_fildsImportance[Calendar.WEEK_OF_YEAR] = c_week;
        s_fildsImportance[Calendar.MONTH] = c_month;
        s_fildsImportance[Calendar.YEAR] = c_year;
        s_fildsImportance[Calendar.ERA] = c_era;
        s_fildsImportance[Calendar.ZONE_OFFSET] = c_zone;
        s_fildsImportance[Calendar.DST_OFFSET] = c_dst;
    }

    private static final Map<Integer, String> s_calendarFieldNames =
        new HashMap<Integer, String>(Calendar.FIELD_COUNT);

    static {
        s_calendarFieldNames.put(Calendar.MILLISECOND, "MILLISECOND");
        s_calendarFieldNames.put(Calendar.SECOND, "SECOND");
        s_calendarFieldNames.put(Calendar.MINUTE, "MINUTE");
        s_calendarFieldNames.put(Calendar.HOUR_OF_DAY, "HOUR_OF_DAY");
        s_calendarFieldNames.put(Calendar.HOUR, "HOUR");
        s_calendarFieldNames.put(Calendar.AM_PM, "AM_PM");
        s_calendarFieldNames.put(Calendar.DAY_OF_MONTH, "DAY_OF_MONTH");
        s_calendarFieldNames.put(Calendar.DAY_OF_WEEK, "DAY_OF_WEEK");
        s_calendarFieldNames.put(Calendar.DAY_OF_WEEK_IN_MONTH, "DAY_OF_WEEK_IN_MONTH");
        s_calendarFieldNames.put(Calendar.DAY_OF_YEAR, "DAY_OF_YEAR");
        s_calendarFieldNames.put(Calendar.DATE, "DATE");
        s_calendarFieldNames.put(Calendar.WEEK_OF_MONTH, "WEEK_OF_MONTH");
        s_calendarFieldNames.put(Calendar.WEEK_OF_YEAR, "WEEK_OF_YEAR");
        s_calendarFieldNames.put(Calendar.MONTH, "MONTH");
        s_calendarFieldNames.put(Calendar.YEAR, "YEAR");
        s_calendarFieldNames.put(Calendar.ERA, "ERA");
        s_calendarFieldNames.put(Calendar.ZONE_OFFSET, "ZONE_OFFSET");
        s_calendarFieldNames.put(Calendar.DST_OFFSET, "DST_OFFSET");
    }

    private String getFieldName(int field) {
        return s_calendarFieldNames.get(field);
    }

    private static int getImportance(int field) {
        return s_fildsImportance[field];
    }

    private static final Comparator s_importanceComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            return (
                getImportance((Integer) o2) -
                getImportance((Integer) o1)
            );
        }
    };

    private SortedMap<Integer, Object> m_fieldData;

    public SortedMap<Integer, Object> getData() {
        return m_fieldData;
    }

    public void setData(SortedMap<Integer, Object> data) {
        m_fieldData = data;
    }

    protected CalendarFieldsByImportance() {
        this(new TreeMap<Integer, Object>(s_importanceComparator));
    }

    protected CalendarFieldsByImportance(SortedMap<Integer, Object> data) {
        m_fieldData = data;
    }

    protected void addFieldData(int fieldId, Object data) {
        m_fieldData.put(fieldId, data);
    }

    protected abstract void handleField(Calendar c, int fieldId, Object data);

    protected void handleByImportance(Calendar c) {
        for (Map.Entry<Integer, Object> me : m_fieldData.entrySet()) {
            handleField(c, me.getKey(), me.getValue());
        }
    }

    public String toString() {
        String ret = "";
        for (Map.Entry<Integer, Object> me : m_fieldData.entrySet()) {
            if (ret.length() > 0) ret += " ";
            ret += getFieldName(me.getKey()) + " = " + me.getValue();
        }
        return ret;
    }

    public boolean equals(Object object) {
        if (object == null) return false;
        CalendarFieldsByImportance c = (CalendarFieldsByImportance) object;
        if (m_fieldData.size() != c.m_fieldData.size()) return false;
        for (Map.Entry<Integer, Object> me : m_fieldData.entrySet()) {
            Object v = me.getValue();
            Object cv = c.m_fieldData.get(me.getKey());
            if (cv == null) {
                if (v != null) return false;
            } else {
                if (v == null || !cv.equals(v)) return false;
            }
        }
        return true;
    }
}
