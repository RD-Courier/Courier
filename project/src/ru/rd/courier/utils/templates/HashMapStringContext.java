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
package ru.rd.courier.utils.templates;

import ru.rd.courier.CourierException;
import ru.rd.courier.utils.StringContext;
import ru.rd.courier.utils.StringExpression;

import java.util.HashMap;
import java.util.Map;

public class HashMapStringContext implements StringContext {
    private Map m_vars = new HashMap();

    public HashMapStringContext() {
    }

    public HashMapStringContext(String[][] data) {
        this();
        for(String[] varData: data) {
            setVar(varData[0], varData[1]);
        }
    }

    private static class StringConst implements StringExpression {
        private final String m_str;
        public StringConst(String str) {
            m_str = str;
        }
        public String calculate(StringContext ctx) throws CourierException {
            return m_str;
        }
    }

    public boolean hasVar(String name) {
        return m_vars.containsKey(name);
    }

    public String getVar(String name) throws CourierException {
        if (m_vars.containsKey(name)) {
            return ((StringExpression)m_vars.get(name)).calculate(this);
        } else throw new CourierException("Variable '" + name + "' does not exist");
    }

    public void setVar(String name, String value) {
        m_vars.put(
            name,
            new StringConst(value)
        );
    }

    public void setVar(String name, StringExpression exp) {
        m_vars.put(name, exp);
    }

    public void removeVar(String name) {
        m_vars.remove(name);
    }
}
