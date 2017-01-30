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
package ru.rd.courier.datalinks;

import java.net.URL;
import javax.xml.namespace.QName;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.w3c.dom.Node;
import ru.rd.courier.CourierContext;
import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.Axis2Source;
import ru.rd.courier.utils.DomHelper;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PoolObjectFactory;

public class Axis2SourceFactory implements PoolObjectFactory {

   private final CourierLogger m_logger;
   public final String m_fieldName;
   public final String m_wsdl;
   public final String m_namespace;
   public final String m_service;
   public final String m_port;
   public final String m_operation;
   public final String m_param;
   private final long m_timeout;


   public Axis2SourceFactory(CourierLogger logger, String name, CourierContext ctx, Node conf) throws Exception {
      this(logger, DomHelper.getNodeAttr(conf, "var-name", "WSResponse"), ctx.getAppFile(DomHelper.getNodeAttr(conf, "wsdl")).getAbsolutePath(), DomHelper.getNodeAttr(conf, "namespace"), DomHelper.getNodeAttr(conf, "service"), DomHelper.getNodeAttr(conf, "port"), DomHelper.getNodeAttr(conf, "operation"), DomHelper.getNodeAttr(conf, "param-name", (String)null), DomHelper.getTimeNodeAttr(conf, "timeout", -1L));
   }

   public Axis2SourceFactory(CourierLogger logger, String fieldName, String wsdl, String namespace, String service, String port, String operation, String param, long timeout) throws Exception {
      this.m_logger = logger;
      this.m_fieldName = fieldName == null?"WSResponse":fieldName;
      this.m_wsdl = wsdl;
      this.m_namespace = namespace;
      this.m_service = service;
      this.m_port = port;
      this.m_operation = operation;
      if(param == null) {
         this.m_param = extractParamName(this.createClient().getAxisService(), this.m_operation);
      } else {
         this.m_param = param;
      }

      this.m_timeout = timeout;
   }

   private static String extractParamName(AxisService service, String operation) {
      XmlSchema schema = service.getSchema(0);
      XmlSchemaElement e = schema.getElementByName(operation);
      XmlSchemaParticle t = ((XmlSchemaComplexType)e.getSchemaType()).getParticle();
      XmlSchemaObject so = ((XmlSchemaSequence)t).getItems().getItem(0);
      return ((XmlSchemaElement)so).getName();
   }

   public ServiceClient createClient() throws Exception {
      ConfigurationContext ctx = ConfigurationContextFactory.createDefaultConfigurationContext();
      ServiceClient sender = new ServiceClient(ctx, new URL("file:///" + this.m_wsdl), new QName(this.m_namespace, this.m_service), this.m_port);
      sender.getOptions().setExceptionToBeThrownOnSOAPFault(true);
      sender.getOptions().setTimeOutInMilliSeconds(this.m_timeout);
      return sender;
   }

   public Object getObject(ObjectPoolIntf pool) throws Exception {
      return new Axis2Source(this, this.createClient());
   }

   public void returnObject(Object o) {
      ((Axis2Source)o).dispose();
   }

   public boolean checkObject(Object o) {
      return true;
   }

   public CourierLogger getLogger() {
      return this.m_logger;
   }
}
