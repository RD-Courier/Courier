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

import ru.rd.courier.utils.StringHelper;

import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 31.08.2006
 * Time: 12:01:26
 */
public class FtpProperties {
    protected String m_host;
    protected int m_port;
    protected String m_username;
    protected String m_password;
    protected boolean m_isAsciiType;
    protected int m_defaultTimeout;
    protected int m_dataTimeout;

    public FtpProperties(Properties props) {
        m_host = StringHelper.stringParam(props, cHostProp);
        m_port = StringHelper.intParam(props, cPortProp, -1);
        m_username = StringHelper.stringParam(props, cUserNameProp);
        m_password = StringHelper.stringParam(props, cPasswordProp);
        m_isAsciiType = StringHelper.boolParam(props, cAsciiProp, false);
        m_defaultTimeout = StringHelper.intParam(props, cDefTimeoutProp, 0);
        m_dataTimeout = StringHelper.intParam(props, cDataTimeoutProp, 0);
    }

    public static final String cHostProp = "host";
    private static final String cPortProp = "port";
    private static final String cUserNameProp = "username";
    private static final String cPasswordProp = "password";
    private static final String cAsciiProp = "ascii-type";
    private static final String cDataTimeoutProp = "data-timeout";
    private static final String cDefTimeoutProp = "default-timeout";

    public void loadProps(Properties props) {
        if (props.containsKey(cHostProp)) {
            m_host = StringHelper.stringParam(props, cHostProp);
        }
        if (props.containsKey(cPortProp)) {
            m_port = StringHelper.intParam(props, cPortProp);
        }
        if (props.containsKey(cUserNameProp)) {
            m_username = StringHelper.stringParam(props, cUserNameProp);
        }
        if (props.containsKey(cPasswordProp)) {
            m_password = StringHelper.stringParam(props, cPasswordProp);
        }
        if (props.containsKey(cDefTimeoutProp)) {
            m_defaultTimeout = StringHelper.intParam(props, cDefTimeoutProp);
        }
        if (props.containsKey(cDataTimeoutProp)) {
            m_dataTimeout = StringHelper.intParam(props, cDataTimeoutProp);
        }
    }
}
