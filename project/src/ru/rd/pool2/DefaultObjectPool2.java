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
package ru.rd.pool2;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.pool.PoolObjectFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: Astepochkin
 * Date: 01.09.2008
 * Time: 17:40:52
 */
public class DefaultObjectPool2 extends ObjectPool2 {
    private Map<Object, ObjectWrapper> m_bobjs;
    private LinkedList<ObjectWrapper> m_fobjs;
    private LinkedList<ObjectWrapper> m_iobjs;
    private boolean m_randomGet;

    public DefaultObjectPool2(
        CourierLogger logger, String desc, PoolObjectFactory objFactory
    ) {
        super(logger, desc, objFactory);
    }

    protected void customStart_nl() {
        m_bobjs = new HashMap<Object, ObjectWrapper>();
        m_fobjs = new LinkedList<ObjectWrapper>();
        m_iobjs = new LinkedList<ObjectWrapper>();
    }

    protected void customAddFreeObject_nl(ObjectWrapper ow) {
        m_fobjs.addFirst(ow);
    }

    protected void customAddBusyObject_nl(ObjectWrapper ow) {
        m_bobjs.put(ow.getObject(), ow);
    }

    protected void customObjectBusy_nl(ObjectWrapper ow, Iterator<ObjectWrapper> freeIterator) {
        freeIterator.remove();
        m_bobjs.put(ow.getObject(), ow);
    }

    protected void customObjectInvalid_nl(ObjectWrapper ow, Iterator<ObjectWrapper> freeIterator) {
        freeIterator.remove();
        m_iobjs.addFirst(ow);
    }

    protected ObjectWrapper customFindReleaseObject_nl(Object o) {
        ObjectWrapper ow = m_bobjs.remove(o);
        if (ow != null) m_fobjs.addFirst(ow);
        return ow;
    }

    protected ObjectWrapper customReleaseObjectToRemove_nl(Object o) {
        ObjectWrapper ow = m_bobjs.remove(o);
        if (ow != null) m_iobjs.addFirst(ow);
        return ow;
    }

    protected Iterator<ObjectWrapper> customGetFreeIterator_nl() {
        return m_fobjs.iterator();
    }

    protected Iterator<ObjectWrapper> customGetBusyIterator_nl() {
        return m_bobjs.values().iterator();
    }

    protected Iterator<ObjectWrapper> customGetInvalidIterator_nl() {
        return m_iobjs.iterator();
    }

    protected int customFreeSize() {
        return m_fobjs.size();
    }

    protected int customBusySize() {
        return m_bobjs.size();
    }


    public boolean getRandomGet() {
        return m_randomGet;
    }

    public void setRandomGet(boolean aValue) {
        ensureInitial();
        m_randomGet = aValue;
    }
}
