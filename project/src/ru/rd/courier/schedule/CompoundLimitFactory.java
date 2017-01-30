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
package ru.rd.courier.schedule;

import ru.rd.courier.schedule.LimitFactory;

import java.util.Collection;
import java.util.LinkedList;

/**
 * User: AStepochkin
 * Date: 07.06.2008
 * Time: 10:36:46
 */
public class CompoundLimitFactory implements LimitFactory {
    Collection<LimitFactory> m_limitFactories;

    public CompoundLimitFactory(Collection<LimitFactory> limitFactories) {
        m_limitFactories = limitFactories;
    }

    public CompoundLimitFactory() {
        this(new LinkedList<LimitFactory>());
    }

    public void addFactory(LimitFactory factory) {
        m_limitFactories.add(factory);
    }

    public void addFactories(Collection<LimitFactory> limitFactories) {
        for (LimitFactory limitFactory : limitFactories) {
            addFactory(limitFactory);
        }
    }

    public RelaunchLimit create() {
        Collection<RelaunchLimit> limits = new LinkedList<RelaunchLimit>();
        for (LimitFactory m_limitFactory : m_limitFactories) {
            limits.add(m_limitFactory.create());
        }
        return new CompoundRelaunchLimit(limits);
    }
}
