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
package ru.rd.courier;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.scripting.AbstractContext;
import ru.rd.courier.scripting.ScriptStatement;
import ru.rd.courier.scripting.XmlStatementFactory;
import ru.rd.courier.datalinks.AccountProvider;
import ru.rd.pool.ObjectPoolIntf;
import ru.rd.pool.PooledObjectHolder;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 21.01.2005
 * Time: 17:00:16
 */
public interface CourierContext {
    PooledObjectHolder getPooledObject(final String dbProfileName) throws CourierException;
    SourceProfile getSourceProfile(String name) throws CourierException;
    TargetProfile getTargetProfile(String name) throws CourierException;
    Pipeline getPipeline(String name) throws CourierException;
    XmlStatementFactory getStmtFactory();
    ObjectPoolIntf getThreadPool();
    Document parseXmlFile(File f)
        throws ParserConfigurationException, IOException, SAXException;
    void start();
    void stop() throws CourierException, InterruptedException;
    File getAppFile(String name) throws CourierException;
    String getParam(String name, boolean mustExist) throws CourierException;
    String getParam(String name) throws CourierException;
    String getParam(String name, String def) throws CourierException;
    String getScriptParam(String name, boolean mustExist) throws CourierException;
    Map<String,String> getDriverParams(String driverName);
    String getScriptParam(String name) throws CourierException;
    DataLogger getPipeLogger(String pipeName, boolean buffered);
    void syncProgress() throws CourierException;
    Timer getTimer();
    SystemDb getSystemDb();
    ScriptStatement getSourceTopStatement();
    ScriptStatement getPipesTopStatement();
    ScriptStatement getPipesScript();
    AbstractContext getCommonContext();
    AccountProvider getAccountProvider();
}
