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

import java.util.Map;
import java.util.HashMap;

/**
 * User: Astepochkin
 * Date: 08.05.2009
 * Time: 9:24:53
 */
public class DefaultAccountProvider implements AccountProvider {
    private final Map<String, HostAccounts> m_accounts = new HashMap<String, HostAccounts>();

    private static class HostAccounts {
        public final Map<String, Account> types = new HashMap<String, Account>();
        public final Map<String, Account> codes = new HashMap<String, Account>();
    }

    private static final String NULL_HOST = "";

    private HostAccounts getHost(String host) {
        return m_accounts.get(host);
    }

    private HostAccounts findHost(String host) {
        HostAccounts ret = getHost(host);
        if (ret == null) {
            ret = new HostAccounts();
            m_accounts.put(host, ret);
        }
        return ret;
    }

    public final void addCodeAccount(String code, Account account) {
        addHostCodeAccount(NULL_HOST, code.toUpperCase(), account);
    }

    public final void addHostCodeAccount(String host, String code, Account account) {
        findHost(host.toUpperCase()).codes.put(code.toUpperCase(), account);
    }

    public final void addHostTypeAccount(String host, String type, Account account) {
        findHost(host.toUpperCase()).types.put(type.toUpperCase(), account);
    }

    private Account getCodeAccount(String code) {
        return getHostCodeAccount(NULL_HOST, code.toUpperCase());
    }

    private Account getHostCodeAccount(String host, String code) {
        HostAccounts ha = getHost(host.toUpperCase());
        if (ha == null) return null;
        return ha.codes.get(code.toUpperCase());
    }

    private Account getHostTypeAccount(String host, String type) {
        HostAccounts ha = getHost(host.toUpperCase());
        if (ha == null) return null;
        return ha.types.get(type.toUpperCase());
    }

    public final Account getAccount(String host, String type, String code) {
        Account account = null;
        if (host == null) {
            account = getCodeAccount(code);
        } else {
            if (type != null) {
                account = getHostTypeAccount(host, type);
            }
            if (account == null) {
                account = getHostCodeAccount(host, code);
            }
            if (account == null) {
                account = getCodeAccount(code);
            }
        }
        return account;
    }
}
