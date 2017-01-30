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

import ru.rd.courier.scripting.*;
import ru.rd.courier.logging.data.DataLogger;
import ru.rd.courier.datalinks.AccountProvider;
import ru.rd.pool.ObjectPool;
import ru.rd.pool.PooledObjectHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.Map;

/**
 * User: AStepochkin
 * Date: 21.01.2005
 * Time: 17:07:24
 */
public class CourierContextMock implements CourierContext {
    public boolean hasDataSource(String profileName) {
        return false;
    }

    public DataSource getDataSource(String profileName) throws CourierException {
        return null;
    }

    public boolean hasDataReceiver(String dbProfileName) throws CourierException {
        return false;
    }

    public PooledObjectHolder getPooledObject(String dbProfileName) throws CourierException {
        return null;
    }

    public SourceProfile getSourceProfile(String name) throws CourierException {
        return null;
    }

    public TargetProfile getTargetProfile(String name) throws CourierException {
        return null;
    }

    public Pipeline getPipeline(String name) throws CourierException {
        return null;
    }

    public XmlStatementFactory getStmtFactory() {
        return null;
    }

    public ObjectPool getThreadPool() {
        return null;
    }

    public Document parseXmlFile(File f) throws ParserConfigurationException, IOException, SAXException {
        return null;
    }

    public void start() {
    }

    public void stop() throws CourierException {
    }

    public File getAppFile(String name) throws CourierException {
        return null;
    }

    public File getSysFile(String name) throws CourierException {
        return null;
    }

    public String getParam(String name, boolean mustExist) throws CourierException {
        return null;
    }

    public String getParam(String name) throws CourierException {
        return null;
    }

    public String getParam(String name, String def) throws CourierException {
        return null;
    }

    public String getScriptParam(String name, boolean mustExist) throws CourierException {
        return null;
    }

    public Map<String,String> getDriverParams(String driverName) {
        return null;
    }

    public String getScriptParam(String name) throws CourierException {
        return null;
    }

    public void initJdbcPool(String dbProfileName, Node n) throws CourierException {
    }

    public DataLogger getPipeLogger(String pipeName, boolean buffered) {
        return null;
    }

    public void syncProgress() throws CourierException {
    }

    public Timer getTimer() {
        return null;
    }

    public SystemDb getSystemDb() {
        return null;
    }

    public ScriptStatement getSourceTopStatement() {
        return null;
    }

    public ScriptStatement getPipesTopStatement() {
        return null;
    }

    public ScriptStatement getPipesScript() {
        return null;
    }

    public AbstractContext getCommonContext() {
        return null;
    }

    public AccountProvider getAccountProvider() {
        return null;
    }
}
