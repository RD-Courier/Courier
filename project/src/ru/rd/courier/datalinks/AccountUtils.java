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

import org.w3c.dom.Node;
import org.jasypt.util.text.BasicTextEncryptor;
import ru.rd.courier.utils.DomHelper;
import ru.rd.courier.utils.FileHelper;

import java.util.Properties;
import java.io.File;
import java.io.IOException;

/**
 * User: Astepochkin
 * Date: 07.05.2009
 * Time: 16:11:54
 */
public class AccountUtils {
    public static String encrypt(String s, String pwd) {
        BasicTextEncryptor enc = new BasicTextEncryptor();
        enc.setPassword(pwd);
        return enc.encrypt(s);
    }

    public static String encrypt(File file, String pwd) throws IOException {
        String ftext = FileHelper.fileToString(file, "cp1251");
        return encrypt(ftext, pwd);
    }

    public static String decrypt(String s, String pwd) {
        BasicTextEncryptor enc = new BasicTextEncryptor();
        enc.setPassword(pwd);
        return enc.decrypt(s);
    }

    public static String decrypt(File file, String pwd) throws IOException {
        String ftext = FileHelper.fileToString(file, "cp1251");
        return decrypt(ftext, pwd);
    }

    public static Account confAccountDef(
            String host, String type, String code, AccountProvider prov, Node conf, Account def
    ) {
        String aname = DomHelper.getNodeAttr(conf, "username", null);
        if (aname != null) {
            return new Account(aname, DomHelper.getNodeAttr(conf, "password"));
        }
        Account account = null;
        if (prov != null) {
            account = prov.getAccount(host, type, code);
        }
        if (account == null) return def;
        return account;
    }

    public static Account confAccountDef(String code, AccountProvider prov, Node conf, Account def) {
        return confAccountDef(null, null, code, prov, conf, def);
    }

    public static Account confAccount(String code, AccountProvider prov, Node conf) {
        Account account = confAccountDef(code, prov, conf, null);
        if (account == null) throw new RuntimeException("Account for code '" + code + "' not found");
        return account;
    }

    public static Account confAccount(String host, String type, String code, AccountProvider prov, Node conf) {
        Account account = confAccountDef(host, type, code, prov, conf, null);
        if (account == null) throw new RuntimeException("Account for code '" + code + "' not found");
        return account;
    }

    public static Account confDbAccount(String host, String code, AccountProvider prov, Node conf) {
        return confAccount(host, "DB", code, prov, conf);
    }

    public static void accountToProps(
        String hostparam, String type, String code, AccountProvider prov, Properties props
    ) {
        if (props.getProperty("username") != null) return;
        if (prov == null) return;
        String host = null;
        if (hostparam != null) host = props.getProperty(hostparam);
        Account account = prov.getAccount(host, type, code);
        if (account == null) return;
        props.setProperty("username", account.name);
        props.setProperty("password", account.password);
    }
}
