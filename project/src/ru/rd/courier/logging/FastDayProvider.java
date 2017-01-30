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
package ru.rd.courier.logging;

import java.util.TimeZone;

/**
 * User: AStepochkin
 * Date: 11.04.2008
 * Time: 4:05:49
 */
public class FastDayProvider implements DayProvider {
    private int m_lastDay = -1;
    private long m_curOffset;
    private static final int c_millisecsInDay = 24 * 60 * 60 * 1000;

    public FastDayProvider() {
        initOffset();
    }

    public int getDay() {
        long ct = System.currentTimeMillis();
        int curDay = (int)((ct + m_curOffset) / c_millisecsInDay);
        if ((curDay >= 0) && curDay != m_lastDay) {
            initOffset();
            curDay = (int)((ct + m_curOffset) / c_millisecsInDay);
        }
        m_lastDay = curDay;
        return m_lastDay;
    }

    private void initOffset() {
        m_curOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
    }
}
