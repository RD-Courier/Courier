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
package ru.rd.pool;

/**
 * User: AStepochkin
 * Date: 20.05.2005
 * Time: 15:02:04
 */
public class TestObject extends TestObjectValues {
    private Checker m_checker;

    private int m_closeCount = 0;
    private int m_checkCount = 0;

    public interface Checker {
        void testCheck(int count);
        void testClose();
    }

    public TestObject(Checker checker) {
        this();
        m_checker = checker;
    }

    public TestObject() {
        m_checker = null;
    }

    public TestObject(String id) {
        this();
        setId(id);
    }

    public TestObject(int id) {
        this(Integer.toString(id));
    }

    public int getCloseCount() {
        return m_closeCount;
    }

    public int getCheckCount() {
        return m_checkCount;
    }

    public boolean isValid() {
        m_checkCount++;
        if (m_checker != null) m_checker.testCheck(m_checkCount);
        return (m_validCheckCount >= 0 && m_checkCount <= m_validCheckCount);
    }

    public void close() {
        m_closeCount++;
        if (m_checker != null) m_checker.testClose();
        if (m_failOnClose) throw new RuntimeException("Test close fail");
    }

    public String toString() {
        return "PoolObject-" + m_id
            + " ValidCheckCount=" + m_validCheckCount
            + " FailOnClose=" + m_failOnClose
            + " CheckCount=" + m_checkCount
            + " CloseCount=" + m_closeCount
            + (m_checker == null ? "" : "Checker = " + m_checker);
    }
}
