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
package ru.rd.courier.scripting.dataaccess.streamed;

import ru.rd.courier.logging.CourierLogger;
import ru.rd.courier.scripting.dataaccess.OsCommand;
import ru.rd.courier.scripting.dataaccess.SshProcess;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.utils.StringSimpleParser;
import ru.rd.pool.ObjectPoolIntf;

import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 10.04.2008
 * Time: 14:06:02
 */
public class SshStreamConnector extends OsCommandConnector {
    private String m_host = null;
    private int m_port = -1;
    private String m_username = null;
    private String m_password = null;
    private String m_command = null;

    public SshStreamConnector(
        CourierLogger logger, ObjectPoolIntf threadPool,
        String host, int port, String username, String password, String command
    ) {
        super(logger, threadPool);
        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;
        m_command = command;
    }

    private static final String cHostProp = "host";
    private static final String cPortProp = "port";
    private static final String cUserNameProp = "username";
    private static final String cPasswordProp = "password";
    private static final String cCommandProp = "command";

    public void parseProperties(StringSimpleParser p) {
        Properties props = p.getProperties(null, '\'', "|");

        if (props.containsKey(cHostProp)) {
            m_host = StringHelper.stringParam(props, cHostProp);
        }
        if (m_host == null) throw new RuntimeException("Host is undefined");

        if (props.containsKey(cPortProp)) {
            m_port = StringHelper.intParam(props, cPortProp);
        }

        if (props.containsKey(cUserNameProp)) {
            m_username = StringHelper.stringParam(props, cUserNameProp);
        }
        if (m_username == null) throw new RuntimeException("Username is undefined");

        if (props.containsKey(cPasswordProp)) {
            m_password = StringHelper.stringParam(props, cPasswordProp);
        }
        if (m_password == null) throw new RuntimeException("Password is undefined");

        if (props.containsKey(cCommandProp)) {
            m_command = StringHelper.stringParam(props, cCommandProp);
        }
        if (m_command == null) throw new RuntimeException("Command is undefined");
    }

    protected OsCommand createCommand() {
        return new SshProcess(m_logger, m_host, m_port, m_username, m_password, m_command);
    }
}
