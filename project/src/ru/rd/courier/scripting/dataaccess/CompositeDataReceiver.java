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
package ru.rd.courier.scripting.dataaccess;

import ru.rd.courier.CourierException;
import ru.rd.courier.scripting.DataReceiver;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CompositeDataReceiver implements DataReceiver {
    private List m_receivers = new LinkedList();

    public CompositeDataReceiver(List receivers) {
        for (Iterator it = receivers.iterator(); it.hasNext();) {
            DataReceiver dr = (DataReceiver)it.next();
            m_receivers.add(dr);
        }
    }

    public void addReceiver(DataReceiver dr) {
        m_receivers.add(dr);
    }

    public List process(Object operation) throws CourierException {
        List res = null;
        for (Iterator it = m_receivers.iterator(); it.hasNext();) {
            DataReceiver dr = (DataReceiver)it.next();
            List l = dr.process(operation);
            if (res == null) res = l;
            else res.addAll(l);
        }
        return res;
    }

    public List flush() throws CourierException {
        List res = null;
        for (Iterator it = m_receivers.iterator(); it.hasNext();) {
            DataReceiver dr = (DataReceiver)it.next();
            List l = dr.flush();
            if (res == null) res = l;
            else res.addAll(l);
        }
        return res;
    }

    public void setTimeout(int timeout) throws CourierException {
        for (Iterator it = m_receivers.iterator(); it.hasNext();) {
            DataReceiver dr = (DataReceiver)it.next();
            dr.setTimeout(timeout);
        }
    }

    public void cancel() throws CourierException {
        for (Iterator it = m_receivers.iterator(); it.hasNext();) {
            DataReceiver dr = (DataReceiver)it.next();
            dr.cancel();
        }
    }

    public void close() throws CourierException {
        for (Iterator it = m_receivers.iterator(); it.hasNext();) {
            DataReceiver dr = (DataReceiver)it.next();
            dr.close();
        }
    }
}
