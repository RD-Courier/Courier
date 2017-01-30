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

import ru.rd.courier.scripting.Context;

public abstract class IntervalValue {
    private final String m_initialValue;
    protected final String m_currentIntervalVarName;
    protected final String m_newIntervalVarName;

    public abstract boolean greaterThanIntervalValue(Context ctx) throws CourierException;

    protected IntervalValue(
        String initialValue, String currentIntervalVarName, String newIntervalVarName
    ) {
        m_initialValue = initialValue;
        m_currentIntervalVarName = currentIntervalVarName;
        m_newIntervalVarName = newIntervalVarName;
    }

    public static IntervalValue createByType(
        char type, String initialValue, String currentIntervalVarName, String newIntervalVarName
    ) throws CourierException {
        switch(type) {
            case ' ': return new FakeInternalValue(
                initialValue, currentIntervalVarName, newIntervalVarName
            );
            case 'i': return new IntegerInternalValue(
                initialValue, currentIntervalVarName, newIntervalVarName
            );
            case 'd': return new DateInternalValue(
                initialValue, currentIntervalVarName, newIntervalVarName
            );
            default: throw new CourierException("Unknown interval type '" + type + "'");
        }
    }

    public String getInitialValue() {
        return m_initialValue;
    }

    private static class FakeInternalValue extends IntervalValue {
        public FakeInternalValue(
            String initialValue, String currentIntervalVarName, String newIntervalVarName
        ) {
            super(initialValue, currentIntervalVarName, newIntervalVarName);
        }

        public boolean greaterThanIntervalValue(Context ctx) {
            return false;
        }
    }

    private static class IntegerInternalValue extends IntervalValue {
        public IntegerInternalValue(
            String initialValue, String currentIntervalVarName, String newIntervalVarName
        ) {
            super(initialValue, currentIntervalVarName, newIntervalVarName);
        }

        public boolean greaterThanIntervalValue(Context ctx) throws CourierException {
            return Integer.parseInt(ctx.getVar(m_newIntervalVarName)) > Integer.parseInt(ctx.getVar(m_currentIntervalVarName));
        }
    }

    private static class DateInternalValue extends IntervalValue {
        public DateInternalValue(
            String initialValue, String currentIntervalVarName, String newIntervalVarName
        ) {
            super(initialValue, currentIntervalVarName, newIntervalVarName);
        }

        public boolean greaterThanIntervalValue(Context ctx) throws CourierException {
            return ctx.getDateVar(m_newIntervalVarName).after(ctx.getDateVar(m_currentIntervalVarName));
        }
    }
}
