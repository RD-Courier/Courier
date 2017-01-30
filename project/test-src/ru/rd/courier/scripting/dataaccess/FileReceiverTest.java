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
import ru.rd.courier.logging.ConsoleCourierLogger;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

public class FileReceiverTest {
    public static void main1(String[] args) throws CourierException {
        FileReceiver fr = new FileReceiver(
            new ConsoleCourierLogger(""), "windows-1251", true, true, false,
            "p-", ".data", "./test-receiver", false,
            "yyyyMMdd-HHmmssSSSS", false
        );
        for (int i = 0; i < 10; i++) {
            fr.process("test operation " + i);
        }
    }

    public static void main(String[] args) throws CourierException, AddressException {
        InternetAddress aa = new InternetAddress("pers@aaa..bbb.ru", true);
        aa.validate();
        System.out.println(
            "Address=" + aa.getAddress() + " Personal=" + aa.getPersonal() + " Type=" + aa.getType()
        );
        /*
        InternetAddress[] as = InternetAddress.parse("aaabbbb", true);
        //InternetAddress[] as = InternetAddress.parse("MyAddress<testemail@rdxxx.ru>,AAAA<BBBB@a_d.domain.ru>");
        for (InternetAddress a: as) {
            a.validate();
            System.out.println(
                "Address=" + a.getAddress() + " Personal=" + a.getPersonal() + " Type=" + a.getType()
            );
        }
        */
    }
}
