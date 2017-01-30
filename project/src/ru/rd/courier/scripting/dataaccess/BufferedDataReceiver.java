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
import ru.rd.courier.scripting.*;
import ru.rd.courier.scripting.dataaccess.jdbc.JdbcReceiver;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

public class BufferedDataReceiver implements DataReceiver, JdbcReceiver, ReceiverTimeCounter, StandardOperationSupport {
    protected final DataReceiver m_dr;
    private final int m_charsLimit;
    private final int m_recordsLimit;
    private final PortionFormatter m_formatter;
    private final PortionSendListener m_portionSendListener;

    private boolean m_operationTriggered;
    private int m_charsCount;
    private int m_recordCount;
    private List<String> m_buf;
    private boolean m_incRecordFlag;

    public BufferedDataReceiver(
        final DataReceiver dr,
        final int bytesLimit, final int recordsLimit,
        PortionFormatter formatter,
        PortionSendListener portionSendListener
    ) {
        if (formatter == null) {
            throw new IllegalArgumentException("Null buffer portion formatter");
        }

        m_dr = dr;
        m_charsLimit = bytesLimit;
        m_recordsLimit = recordsLimit;
        m_formatter = formatter;
        if (portionSendListener == null) {
            m_portionSendListener = new PortionSendListener() {
                public void before(PortionSendEvent event) {}
                public void after(PortionSendEvent event) {}
            };
        } else {
            m_portionSendListener = portionSendListener;
        }
        initBuffer();
        clear();
    }

    protected final DataReceiver getReceiver() {
        return m_dr;
    }

    // ********** buffer fields and operations *********************

    private void initBuffer() {
        m_buf = new LinkedList<String>();
        m_charsCount = 0;
        m_recordCount = 0;
    }

    private void addToBuf(final String operation) {
        m_charsCount += operation.length();
        m_buf.add(operation);
    }

    private void clearBuf() {
        m_buf.clear();
    }

    private String bufToString() throws CourierException {
        if (m_formatter == null) {
            throw new CourierException("No formatter was registered to format buffer");
        }
        return m_formatter.format(m_buf);
    }

    private int getBufCharCount() {
        return m_charsCount;
    }

    private int getBufRecordsCount() {
        return m_recordCount;
    }
    // ********** end of buffer operations *********************

    private boolean timeToAct() {
        return (m_charsLimit > 0) && (getBufCharCount() >= m_charsLimit);
    }

    private void clear() {
        clearBuf();
        m_charsCount = 0;
        m_recordCount = 0;
        m_operationTriggered = false;
        m_incRecordFlag = false;
    }

    private boolean m_bufferCanceled = false;

    public List<LinkWarning> flush() throws CourierException {
        PortionSendEvent e = new PortionSendEvent(
            getBufRecordsCount(), bufToString()
        );
        List<LinkWarning> ret = null;
        try {
            if (m_operationTriggered && (m_buf.size() > 0)) {
                m_bufferCanceled = true;
                try {
                    m_portionSendListener.before(e);
                    ret = processData(e.getPortion());
                    m_portionSendListener.after(e);
                } finally {
                    m_bufferCanceled = false;
                }
            } else if (m_incRecordFlag) {
                m_portionSendListener.after(e);
            }
        } finally {
            clear();
        }

        return ret;
    }

    public void clearTargetTime() {
        ((ReceiverTimeCounter)getReceiver()).clearTargetTime();
    }

    public long getTargetTime() {
        return ((ReceiverTimeCounter)getReceiver()).getTargetTime();
    }

    private List<LinkWarning> processData(Object data) {
        return getReceiver().process(data);
    }

    public List<LinkWarning> process(final Object operation) throws CourierException {
        if (operation instanceof String) {
            if (m_bufferCanceled) {
                return processData(operation);
            } else {
                m_operationTriggered = true;
                addToBuf((String)operation);
                if (timeToAct()) return flush();
                return null;
            }
        } else {
            List<LinkWarning> res1 = flush();
            List<LinkWarning> res2 = getReceiver().process(operation);
            if (res2 != null) {
                if (res1 != null) res2.addAll(res1);
                return res2;
            } else {
                if (res1 == null) return null;
                res1.addAll(res2);
                return res1;
            }
        }
    }

    public void setTimeout(final int timeout) throws CourierException {
        getReceiver().setTimeout(timeout);
    }

    public void cancel() throws CourierException {
        clear();
        getReceiver().cancel();
    }

    public void close() throws CourierException {
        flush();
        getReceiver().close();
    }

    public List<LinkWarning> incRecordsCount() throws CourierException {
        m_incRecordFlag = true;
        m_recordCount++;
        if ((m_recordsLimit > 0) && (m_recordCount >= m_recordsLimit)) {
            return flush();
        } else {
            return null;
        }
    }

    public String getType() {
        return ((StandardOperationSupport)getReceiver()).getType();
    }

    public Connection getConnection() {
        return ((StandardOperationSupport)getReceiver()).getConnection();
    }

    public void setAutoCommit(boolean autoCommit) {
        //if (m_dr instanceof JdbcReceiver) {
        //    ((JdbcReceiver)m_dr).setAutoCommit(autoCommit);
        //}
    }

    public boolean isOperationTriggered() {
        return m_operationTriggered;
    }

    public boolean isIncRecordFlag() {
        return m_incRecordFlag;
    }
}
