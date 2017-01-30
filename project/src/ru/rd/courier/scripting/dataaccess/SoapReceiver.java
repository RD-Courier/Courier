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

import ru.rd.courier.scripting.TimedStringReceiver;
import ru.rd.courier.scripting.LinkWarning;
import ru.rd.courier.CourierException;

import javax.xml.rpc.Call;
import javax.xml.namespace.QName;
import java.util.List;
import java.rmi.RemoteException;

import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.MessageContext;
import org.apache.axis.AxisProperties;
import org.apache.axis.components.net.SocketFactory;
import org.apache.axis.transport.http.HTTPConstants;

/**
 * User: AStepochkin
 * Date: 19.12.2005
 * Time: 12:53:49
 */
public class SoapReceiver extends TimedStringReceiver {
    private final Parser m_wsdlParser = new Parser();
    private Call m_call;
    static {
        AxisProperties.setClassDefault(
            SocketFactory.class, "ru.rd.axis.PoolSocketFactory"
        );
    }

    public SoapReceiver(String wsdlUri) {
        try {
            m_wsdlParser.run(wsdlUri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createCall(String serviceName, String portName, String operationName) {
        try {
            org.apache.axis.client.Service dpf = new org.apache.axis.client.Service(
                m_wsdlParser, QName.valueOf(serviceName)
            );
            m_call = dpf.createCall(
                QName.valueOf(portName), QName.valueOf(operationName)
            );
            m_call.setProperty(
                MessageContext.HTTP_TRANSPORT_VERSION, HTTPConstants.HEADER_PROTOCOL_V11
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<LinkWarning> timedProcess(String operation) throws CourierException {
        try {
            m_call.invoke(new Object[] {operation});
        } catch (RemoteException e) {
            throw new CourierException(e);
        }
        return null;
    }

    protected List timedFlush() {
        return null;
    }

    protected void timedClose() {}

    public void setTimeout(int timeout) throws CourierException {
        ((org.apache.axis.client.Call)m_call).setTimeout(new Integer(timeout * 1000));
    }

    public void cancel() throws CourierException {}
}
