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
package ru.rd.utils;

import ru.rd.courier.logging.CourierLogger;

import java.io.Closeable;
import java.util.List;
import java.util.LinkedList;
import java.util.Collection;

/**
 * User: AStepochkin
 * Date: 01.02.2008
 * Time: 11:47:05
 */
public class InvokeAllRunnable implements Runnable {
    private final CourierLogger m_logger;
    private final List<Invokee> m_items;
    private final String m_desc;
    private final boolean m_logFinished;

    public InvokeAllRunnable(CourierLogger logger, String desc, boolean logFinished) {
        m_logger = logger;
        m_desc = desc;
        m_items = new LinkedList<Invokee>();
        m_logFinished = logFinished;
    }

    public InvokeAllRunnable(CourierLogger logger, String desc) {
        this(logger, desc, false);
    }

    public InvokeAllRunnable(CourierLogger logger, String desc, Invokee[] items) {
        this(logger, desc);
        add(items);
    }

    public String toString() {
        if (m_desc == null) return super.toString();
        return m_desc;
    }

    public void add(Invokee item) {
        m_items.add(item);
    }

    public void add(Object object, Class cl, String methodName) {
        Invokee invokee = MethodInvoker.tryCreate(object, cl, methodName);
        if (invokee == null) {
            throw new RuntimeException("Object: " + object + " does not have method '" + methodName + "'");
        }

        add(invokee);
    }

    public void add(Object object, String methodName) {
        add(object, object.getClass(), methodName);
    }

    public void add(Invokee[] items) {
        for (Invokee item: items) add(item);
    }

    public void add(Collection<Invokee> items) {
        m_items.addAll(items);
    }

    public void run() {
        for (Invokee item: m_items) {
            try {
                item.invoke();
            } catch (Exception e) {
                m_logger.warning(e);
            }
        }
        if (m_logFinished) m_logger.debug(this + " finished");
    }

    public static Runnable closingWork(CourierLogger logger, String desc, Object[] items) {
        InvokeAllRunnable ret = new InvokeAllRunnable(logger, desc);
        for (final Object item: items) {
            if (item == null) continue;
            Invokee invokee;
            if (item instanceof Closeable) {
                invokee = new Invokee() {
                    private final Closeable m_closable = (Closeable)item;
                    public void invoke() throws Exception {
                        m_closable.close();
                    }
                };
            } else if (item instanceof Disposable) {
                invokee = new Invokee() {
                    private final Disposable m_closable = (Disposable)item;
                    public void invoke() throws Exception {
                        m_closable.dispose();
                    }
                };
            } else {
                invokee = MethodInvoker.tryCreate(item, item.getClass(), "close");
                if (invokee == null) {
                    throw new RuntimeException("Object: " + item + " does not support standard close interfaces");
                }
            }
            ret.add(invokee);
        }
        return ret;
    }
}
