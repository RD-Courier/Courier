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

import java.sql.ResultSet;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import ru.rd.courier.CourierException;
import ru.rd.courier.datalinks.Axis2SourceFactory;
import ru.rd.courier.jdbc.ResultSets.StringListResultSet;
import ru.rd.courier.scripting.DataSource;
import ru.rd.courier.scripting.TimedStringReceiver;

public class Axis2Source extends TimedStringReceiver implements DataSource {

   private final Axis2SourceFactory m_f;
   private final ServiceClient m_sender;
   private final String m_namespace;
   private final String m_operation;
   private final String m_param;


   public Axis2Source(Axis2SourceFactory f, ServiceClient sender) throws Exception {
      this.m_f = f;
      this.m_sender = sender;
      this.m_namespace = f.m_namespace;
      this.m_operation = f.m_operation;
      this.m_param = f.m_param;
   }

   protected List timedProcess(String operation) throws CourierException {
      this.wsrequest(operation);
      return null;
   }

   public ResultSet request(String query) throws CourierException {
      OMElement response = this.wsrequest(query);
      String result = response.getFirstElement().getText();
      return new StringListResultSet(new String[]{this.m_f.m_fieldName}, new String[]{result});
   }

   protected List timedFlush() {
      return null;
   }

   protected void timedClose() throws CourierException {}

   public void setTimeout(int timeout) throws CourierException {}

   public void cancel() throws CourierException {}

   private OMElement wsrequest(String query) throws CourierException {
      try {
         return this.m_sender.sendReceive(new QName(this.m_namespace, this.m_operation), this.createPayload(query));
      } catch (Exception var3) {
         throw new CourierException(var3);
      }
   }

   private OMElement createPayload(String data) {
      OMFactory f = OMAbstractFactory.getOMFactory();
      OMNamespace omNs = f.createOMNamespace(this.m_namespace, "courier");
      OMElement method = f.createOMElement(this.m_operation, omNs);
      OMElement value = f.createOMElement(this.m_param, omNs);
      value.addChild(f.createOMText(value, data));
      method.addChild(value);
      return method;
   }

   public void dispose() {
      try {
         this.m_sender.cleanupTransport();
         this.m_sender.cleanup();
      } catch (AxisFault var2) {
         this.m_f.getLogger().warning(var2);
      }

   }
}
